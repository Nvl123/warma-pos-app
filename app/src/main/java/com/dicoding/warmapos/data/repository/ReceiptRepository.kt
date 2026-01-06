package com.dicoding.warmapos.data.repository

import android.content.Context
import android.os.Environment
import android.util.Log
import com.dicoding.warmapos.data.model.Receipt
import com.google.gson.Gson
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository for managing receipt history
 * Saves to external public storage for easy access via file manager
 */
class ReceiptRepository(private val context: Context) {

    private val gson = Gson()
    
    /**
     * Get the public Documents folder for WARMAPOS receipts
     * This folder is visible in file managers
     */
    private val historyDir: File
        get() {
            // Use public Documents folder
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val warmaPosDir = File(documentsDir, APP_FOLDER_NAME)
            val receiptsDir = File(warmaPosDir, HISTORY_DIR_NAME)
            
            if (!receiptsDir.exists()) {
                val created = receiptsDir.mkdirs()
                Log.d("ReceiptRepository", "Created directory: $receiptsDir, success: $created")
            }
            
            return receiptsDir
        }

    /**
     * Save a receipt to history (in public Documents/WARMAPOS/Struk folder)
     */
    fun saveReceipt(receipt: Receipt): String {
        val dateFolder = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Date(receipt.timestamp))
        val timeFile = SimpleDateFormat("HHmmss", Locale.getDefault())
            .format(Date(receipt.timestamp))

        val folder = File(historyDir, dateFolder)
        if (!folder.exists()) {
            folder.mkdirs()
        }

        val file = File(folder, "struk_$timeFile.json")
        val json = gson.toJson(receipt)
        file.writeText(json)
        
        Log.d("ReceiptRepository", "Saved receipt to: ${file.absolutePath}")

        return file.absolutePath
    }

    /**
     * Update an existing receipt
     */
    fun updateReceipt(receipt: Receipt, filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                val json = gson.toJson(receipt)
                file.writeText(json)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Load a receipt from file
     */
    fun loadReceipt(filePath: String): Receipt? {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                val json = file.readText()
                gson.fromJson(json, Receipt::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Get all receipts (most recent first)
     */
    fun getReceiptHistory(limit: Int = 50): List<ReceiptHistoryItem> {
        val receipts = mutableListOf<ReceiptHistoryItem>()
        
        val dir = historyDir
        if (!dir.exists()) return receipts

        val dateFolders = dir.listFiles()?.sortedByDescending { it.name } ?: return receipts

        for (dateFolder in dateFolders) {
            if (!dateFolder.isDirectory) continue

            val files = dateFolder.listFiles()
                ?.filter { it.name.endsWith(".json") }
                ?.sortedByDescending { it.name }
                ?: continue

            for (file in files) {
                try {
                    val receipt = loadReceipt(file.absolutePath)
                    if (receipt != null) {
                        receipts.add(
                            ReceiptHistoryItem(
                                path = file.absolutePath,
                                date = dateFolder.name,
                                time = file.name.removePrefix("struk_").removeSuffix(".json"),
                                total = receipt.total,
                                itemsCount = receipt.items.size,
                                kasir = receipt.kasir
                            )
                        )

                        if (receipts.size >= limit) {
                            return receipts
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return receipts
    }

    /**
     * Delete a receipt
     */
    fun deleteReceipt(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                val deleted = file.delete()
                // Remove empty parent folder
                val parent = file.parentFile
                if (parent?.listFiles()?.isEmpty() == true) {
                    parent.delete()
                }
                deleted
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Clear all receipt history
     */
    fun clearHistory() {
        val dir = historyDir
        if (dir.exists()) {
            dir.deleteRecursively()
            dir.mkdirs()
        }
    }
    
    /**
     * Get the folder path for user info
     */
    fun getStorageLocation(): String {
        return historyDir.absolutePath
    }

    companion object {
        private const val APP_FOLDER_NAME = "WARMAPOS"
        private const val HISTORY_DIR_NAME = "Struk"
    }
}

/**
 * Receipt history list item (lightweight summary)
 */
data class ReceiptHistoryItem(
    val path: String,
    val date: String,
    val time: String,
    val total: Int,
    val itemsCount: Int,
    val kasir: String
) {
    fun formattedTotal(): String {
        return "Rp${String.format("%,d", total).replace(',', '.')}"
    }

    fun formattedTime(): String {
        return if (time.length >= 4) {
            "${time.substring(0, 2)}:${time.substring(2, 4)}"
        } else {
            time
        }
    }
}
