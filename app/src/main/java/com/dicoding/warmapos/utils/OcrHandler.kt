package com.dicoding.warmapos.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.dicoding.warmapos.data.api.RetrofitClient
import com.dicoding.warmapos.data.model.OcrItem
import com.dicoding.warmapos.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

/**
 * OCR Handler for processing images via OCR.space API
 * Includes automatic image compression to stay under 1MB limit
 * All heavy operations run on Dispatchers.IO to prevent UI blocking
 * Supports custom API Key via SettingsRepository
 */
class OcrHandler(private val context: Context) {

    private val settingsRepository = SettingsRepository(context)
    private val apiService = RetrofitClient.ocrApiService
    
    companion object {
        private const val TAG = "OcrHandler"
        private const val MAX_FILE_SIZE = 800 * 1024 // 800KB to be safe
        private const val MAX_DIMENSION = 1200 // Reduced for faster processing
        private const val INITIAL_QUALITY = 70
        private const val MIN_QUALITY = 30
    }

    /**
     * Process image and extract items
     * Runs on IO dispatcher to prevent main thread blocking
     */
    suspend fun processImage(imageUri: Uri, apiKey: String): Result<List<OcrItem>> {
        Log.d(TAG, "=== Starting OCR Processing ===")
        Log.d(TAG, "Image URI: $imageUri")
        
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Step 1: Starting image compression...")
                
                // Compress image to ensure it's under 1MB (runs on IO thread)
                val compressedFile = compressImage(imageUri)
                
                if (compressedFile == null) {
                    Log.e(TAG, "Step 1 FAILED: Image compression returned null")
                    return@withContext Result.failure(Exception("Gagal memproses gambar"))
                }
                
                Log.d(TAG, "Step 1 SUCCESS: Compressed file size = ${compressedFile.length() / 1024}KB")
                Log.d(TAG, "Step 2: Creating multipart request...")

                val requestFile = compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", compressedFile.name, requestFile)

                Log.d(TAG, "Step 3: Sending to OCR API...")
                
                val response = apiService.parseImage(
                    file = filePart,
                    apiKey = apiKey.toRequestBody("text/plain".toMediaTypeOrNull()),
                    language = "eng".toRequestBody("text/plain".toMediaTypeOrNull()),
                    engine = "2".toRequestBody("text/plain".toMediaTypeOrNull()),
                    detectOrientation = "true".toRequestBody("text/plain".toMediaTypeOrNull()),
                    scale = "true".toRequestBody("text/plain".toMediaTypeOrNull()),
                    isOverlayRequired = "false".toRequestBody("text/plain".toMediaTypeOrNull())
                )

                Log.d(TAG, "Step 3 SUCCESS: Got API response")
                
                // Clean up temp file
                compressedFile.delete()
                Log.d(TAG, "Step 4: Temp file deleted")

                if (response.IsErroredOnProcessing) {
                    val errorMsg = response.ErrorMessage?.joinToString(", ") ?: "OCR gagal diproses"
                    Log.e(TAG, "OCR API Error: $errorMsg")
                    return@withContext Result.failure(Exception(errorMsg))
                }

                val text = response.ParsedResults?.firstOrNull()?.ParsedText
                Log.d(TAG, "Step 5: Parsed text length = ${text?.length ?: 0}")
                
                if (text.isNullOrBlank()) {
                    Log.e(TAG, "No text detected in image")
                    return@withContext Result.failure(Exception("Tidak ada teks terdeteksi di gambar"))
                }

                val items = parseItems(text)
                Log.d(TAG, "Step 6 SUCCESS: Parsed ${items.size} items")
                
                Result.success(items)

            } catch (e: OutOfMemoryError) {
                Log.e(TAG, "OUT OF MEMORY ERROR!", e)
                System.gc()
                Result.failure(Exception("Memori tidak cukup. Coba gambar lebih kecil."))
            } catch (e: Exception) {
                Log.e(TAG, "EXCEPTION during OCR: ${e.message}", e)
                Result.failure(Exception("Error: ${e.message ?: "Unknown error"}"))
            }
        }
    }

    /**
     * Compress image to ensure it's under MAX_FILE_SIZE
     * This runs on IO thread (called from withContext(Dispatchers.IO))
     */
    private fun compressImage(uri: Uri): File? {
        Log.d(TAG, "compressImage: Starting...")
        
        return try {
            // Step 1: Open input stream and get dimensions
            Log.d(TAG, "compressImage: Opening input stream...")
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "compressImage: Failed to open input stream")
                return null
            }
            
            // Get dimensions without loading full bitmap
            Log.d(TAG, "compressImage: Reading dimensions...")
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            
            val originalWidth = options.outWidth
            val originalHeight = options.outHeight
            Log.d(TAG, "compressImage: Original size = ${originalWidth}x${originalHeight}")
            
            if (originalWidth <= 0 || originalHeight <= 0) {
                Log.e(TAG, "compressImage: Invalid image dimensions")
                return null
            }
            
            // Calculate sample size
            var sampleSize = 1
            while (originalWidth / sampleSize > MAX_DIMENSION || originalHeight / sampleSize > MAX_DIMENSION) {
                sampleSize *= 2
            }
            Log.d(TAG, "compressImage: Using sampleSize = $sampleSize")
            
            // Decode with calculated sample size
            Log.d(TAG, "compressImage: Decoding bitmap...")
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory
            }
            
            val freshInputStream = context.contentResolver.openInputStream(uri)
            if (freshInputStream == null) {
                Log.e(TAG, "compressImage: Failed to reopen input stream")
                return null
            }
            
            val bitmap = BitmapFactory.decodeStream(freshInputStream, null, decodeOptions)
            freshInputStream.close()
            
            if (bitmap == null) {
                Log.e(TAG, "compressImage: Failed to decode bitmap")
                return null
            }
            
            Log.d(TAG, "compressImage: Decoded bitmap size = ${bitmap.width}x${bitmap.height}")
            
            // Compress with decreasing quality
            var quality = INITIAL_QUALITY
            var tempFile: File
            
            do {
                tempFile = File.createTempFile("ocr_compressed_", ".jpg", context.cacheDir)
                FileOutputStream(tempFile).use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
                }
                
                val fileSize = tempFile.length()
                Log.d(TAG, "compressImage: quality=$quality, file size=${fileSize/1024}KB")
                
                if (fileSize > MAX_FILE_SIZE && quality > MIN_QUALITY) {
                    tempFile.delete()
                    quality -= 10
                }
            } while (tempFile.length() > MAX_FILE_SIZE && quality > MIN_QUALITY)
            
            // Recycle bitmap to free memory immediately
            bitmap.recycle()
            Log.d(TAG, "compressImage: Bitmap recycled")
            
            // Force garbage collection
            System.gc()
            Log.d(TAG, "compressImage: GC triggered, final file size = ${tempFile.length()/1024}KB")
            
            tempFile
            
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "compressImage: OUT OF MEMORY!", e)
            System.gc()
            null
        } catch (e: Exception) {
            Log.e(TAG, "compressImage: Exception - ${e.message}", e)
            null
        }
    }

    /**
     * Parse OCR text to extract items and quantities
     */
    fun parseItems(text: String): List<OcrItem> {
        val items = mutableListOf<OcrItem>()

        // Pattern 1: "Nama Barang 2" or "Nama Barang x2" or "Nama Barang - 2"
        val pattern1 = Regex("^(.+?)\\s*[-x]?\\s*(\\d+)\\s*$", RegexOption.IGNORE_CASE)
        // Pattern 2: "2 Nama Barang" or "2x Nama Barang"
        val pattern2 = Regex("^(\\d+)\\s*[x]?\\s+(.+)$", RegexOption.IGNORE_CASE)

        text.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.length < 2) return@forEach

            val match2 = pattern2.find(trimmed)
            val match1 = pattern1.find(trimmed)

            when {
                match2 != null -> {
                    val qty = match2.groupValues[1].toIntOrNull() ?: 1
                    val name = match2.groupValues[2].trim()
                    if (name.isNotEmpty() && name.length >= 2) {
                        items.add(OcrItem(
                            originalText = trimmed,
                            processedText = name,
                            quantity = qty
                        ))
                    }
                }
                match1 != null -> {
                    val name = match1.groupValues[1].trim()
                    val qty = match1.groupValues[2].toIntOrNull() ?: 1
                    if (name.isNotEmpty() && name.length >= 2) {
                        items.add(OcrItem(
                            originalText = trimmed,
                            processedText = name,
                            quantity = qty
                        ))
                    }
                }
                trimmed.length >= 2 -> {
                    // No quantity found, assume 1
                    items.add(OcrItem(
                        originalText = trimmed,
                        processedText = trimmed,
                        quantity = 1
                    ))
                }
            }
        }

        return items
    }
}
