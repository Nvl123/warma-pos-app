package com.dicoding.warmapos.data.repository

import android.os.Environment
import android.util.Log
import com.dicoding.warmapos.data.model.GroupedReceipt
import com.google.gson.Gson
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository for managing grouped receipts
 * Saves to Documents/WARMAPOS/GroupedStruk
 */
class GroupedReceiptRepository {

    private val gson = Gson()
    
    private val groupedDir: File
        get() {
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val warmaPosDir = File(documentsDir, "WARMAPOS")
            val groupedDir = File(warmaPosDir, "GroupedStruk")
            
            if (!groupedDir.exists()) {
                groupedDir.mkdirs()
            }
            return groupedDir
        }

    fun saveGroupedReceipt(groupedReceipt: GroupedReceipt): String {
        val dateFolder = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Date(groupedReceipt.timestamp))
        val timeFile = SimpleDateFormat("HHmmss", Locale.getDefault())
            .format(Date(groupedReceipt.timestamp))

        val folder = File(groupedDir, dateFolder)
        if (!folder.exists()) {
            folder.mkdirs()
        }

        val filename = "Group_$timeFile.json"
        val file = File(folder, filename)

        try {
            val json = gson.toJson(groupedReceipt)
            file.writeText(json)
            Log.d("GroupedReceiptRepo", "Saved group receipt to ${file.absolutePath}")
            return file.absolutePath
        } catch (e: Exception) {
            Log.e("GroupedReceiptRepo", "Failed to save group receipt", e)
            throw e
        }
    }

    fun getGroupedReceipts(): List<GroupedReceipt> {
        val receipts = mutableListOf<GroupedReceipt>()
        
        if (!groupedDir.exists()) return receipts

        groupedDir.walkTopDown().forEach { file ->
            if (file.isFile && file.extension.lowercase() == "json") {
                try {
                    val json = file.readText()
                    val receipt = gson.fromJson(json, GroupedReceipt::class.java)
                    receipts.add(receipt)
                } catch (e: Exception) {
                    Log.e("GroupedReceiptRepo", "Error reading group receipt: ${file.name}", e)
                }
            }
        }
        
        return receipts.sortedByDescending { it.timestamp }
    }
    
    fun deleteGroupedReceipt(receipt: GroupedReceipt): Boolean {
        // We need to find the file based on timestamp if path isn't stored, 
        // but let's try to search by approximate match if exact path logic is complex,
        // or re-construct path logic.
        
        // Strategy: Look for file with matching ID content
        groupedDir.walkTopDown().forEach { file ->
            if (file.isFile && file.extension.lowercase() == "json") {
                try {
                    val json = file.readText()
                    // Quick check to avoid full parsing if performance is issue, but ID check is safest
                    if (json.contains(receipt.id)) {
                        val parsed = gson.fromJson(json, GroupedReceipt::class.java)
                        if (parsed.id == receipt.id) {
                            return file.delete()
                        }
                    }
                } catch (e: Exception) {
                   // ignore
                }
            }
        }
        return false
    }

    fun updateGroupedReceipt(updatedReceipt: GroupedReceipt): Boolean {
        // Find the file with matching ID and update it
        groupedDir.walkTopDown().forEach { file ->
            if (file.isFile && file.extension.lowercase() == "json") {
                try {
                    val json = file.readText()
                    if (json.contains(updatedReceipt.id)) {
                        val parsed = gson.fromJson(json, GroupedReceipt::class.java)
                        if (parsed.id == updatedReceipt.id) {
                            val newJson = gson.toJson(updatedReceipt)
                            file.writeText(newJson)
                            Log.d("GroupedReceiptRepo", "Updated group receipt: ${file.absolutePath}")
                            return true
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GroupedReceiptRepo", "Error updating group receipt", e)
                }
            }
        }
        return false
    }
}
