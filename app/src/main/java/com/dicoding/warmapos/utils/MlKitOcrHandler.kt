package com.dicoding.warmapos.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.dicoding.warmapos.data.model.OcrItem
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * ML Kit OCR Handler for offline text recognition
 * Uses Google ML Kit's on-device text recognition (no internet required)
 */
class MlKitOcrHandler(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    companion object {
        private const val TAG = "MlKitOcrHandler"
    }

    /**
     * Process image and extract items using ML Kit (offline)
     */
    suspend fun processImage(imageUri: Uri): Result<List<OcrItem>> {
        Log.d(TAG, "=== Starting ML Kit OCR Processing ===")
        Log.d(TAG, "Image URI: $imageUri")

        return withContext(Dispatchers.IO) {
            try {
                // Load image
                val inputStream = context.contentResolver.openInputStream(imageUri)
                    ?: return@withContext Result.failure(Exception("Gagal membuka gambar"))
                
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                
                if (bitmap == null) {
                    return@withContext Result.failure(Exception("Gagal decode gambar"))
                }

                Log.d(TAG, "Image loaded: ${bitmap.width}x${bitmap.height}")

                // Create InputImage for ML Kit
                val image = InputImage.fromBitmap(bitmap, 0)

                // Process with ML Kit using suspendCancellableCoroutine
                Log.d(TAG, "Processing with ML Kit...")
                val fullText = suspendCancellableCoroutine<String?> { continuation ->
                    recognizer.process(image)
                        .addOnSuccessListener { result ->
                            continuation.resume(result.text)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "ML Kit failed", e)
                            continuation.resume(null)
                        }
                }
                
                Log.d(TAG, "ML Kit result: ${fullText?.length ?: 0} characters")
                
                if (fullText.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("Tidak ada teks terdeteksi di gambar"))
                }
                
                Log.d(TAG, "Text preview: ${fullText.take(200)}...")

                // Parse items from text
                val items = parseItems(fullText)
                Log.d(TAG, "Parsed ${items.size} items")

                Result.success(items)
            } catch (e: Exception) {
                Log.e(TAG, "ML Kit OCR failed", e)
                Result.failure(Exception("OCR gagal: ${e.message}"))
            }
        }
    }

    /**
     * Parse raw text into OcrItems
     */
    private fun parseItems(text: String): List<OcrItem> {
        val items = mutableListOf<OcrItem>()
        val lines = text.lines()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue

            // Skip common non-item lines
            if (isSkippableLine(trimmed)) continue

            // Try to extract quantity and item name
            val parsed = parseItemLine(trimmed)
            if (parsed != null) {
                items.add(parsed)
            }
        }

        return items
    }

    private fun isSkippableLine(line: String): Boolean {
        val lower = line.lowercase()
        val skipPatterns = listOf(
            "total", "subtotal", "sub total", "grand total",
            "tunai", "cash", "kembalian", "change",
            "terima kasih", "thank you", "selamat",
            "struk", "receipt", "nota", "invoice",
            "tanggal", "date", "waktu", "time",
            "kasir", "cashier", "operator",
            "-----", "=====", "*****",
            "ppn", "tax", "pajak", "diskon", "discount"
        )
        return skipPatterns.any { lower.contains(it) }
    }

    private fun parseItemLine(line: String): OcrItem? {
        // Pattern: quantity x item name or item name x quantity
        val patterns = listOf(
            // "2 x Indomie Goreng" or "2x Indomie Goreng"
            Regex("""^(\d+)\s*[xX×]\s*(.+)$"""),
            // "Indomie Goreng x 2" or "Indomie Goreng x2"
            Regex("""^(.+?)\s*[xX×]\s*(\d+)$"""),
            // "2 Indomie Goreng" (quantity at start)
            Regex("""^(\d+)\s+(.+)$"""),
            // Just item name (quantity = 1)
            Regex("""^(.+)$""")
        )

        for ((index, pattern) in patterns.withIndex()) {
            val match = pattern.find(line)
            if (match != null) {
                return when (index) {
                    0 -> { // qty x item
                        val qty = match.groupValues[1].toIntOrNull() ?: 1
                        val name = match.groupValues[2].trim()
                        if (name.length >= 2) createOcrItem(name, qty) else null
                    }
                    1 -> { // item x qty
                        val name = match.groupValues[1].trim()
                        val qty = match.groupValues[2].toIntOrNull() ?: 1
                        if (name.length >= 2) createOcrItem(name, qty) else null
                    }
                    2 -> { // qty item
                        val qty = match.groupValues[1].toIntOrNull() ?: 1
                        val name = match.groupValues[2].trim()
                        // Only if qty is reasonable (1-99) and name is valid
                        if (qty in 1..99 && name.length >= 2 && !name.first().isDigit()) {
                            createOcrItem(name, qty)
                        } else null
                    }
                    else -> { // just name
                        val name = line.trim()
                        // Filter out lines that look like prices or numbers
                        if (name.length >= 3 && !name.matches(Regex("""^[\d.,\s]+$"""))) {
                            createOcrItem(name, 1)
                        } else null
                    }
                }
            }
        }
        return null
    }

    private fun createOcrItem(originalText: String, quantity: Int): OcrItem {
        // Clean the text for processed version
        val processed = originalText
            .replace(Regex("""[^\w\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .lowercase()
            .replaceFirstChar { it.uppercase() }

        return OcrItem(
            originalText = originalText,
            processedText = processed,
            quantity = quantity
        )
    }
}
