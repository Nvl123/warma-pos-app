package com.dicoding.warmapos.data.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Repository for backup and restore functionality
 * Creates ZIP archive containing products.csv and receipt history
 */
class BackupRepository(private val context: Context) {

    companion object {
        private const val TAG = "BackupRepository"
        private const val BACKUP_FOLDER = "WARMAPOS_Backup"
        private const val PRODUCTS_FILENAME = "products.csv"
        private const val RECEIPTS_FOLDER = "Struk"
        private const val GROUPED_RECEIPTS_FOLDER = "GroupedStruk"
    }

    /**
     * Create backup ZIP file containing:
     * - products.csv (from internal storage)
     * - All receipt JSON files (from Documents/WARMAPOS/Struk)
     * 
     * Returns the backup file or null if failed
     */
    fun createBackup(): File? {
        return try {
            // Create backup folder in external storage
            val backupDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                BACKUP_FOLDER
            )
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            // Generate backup filename with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(backupDir, "warmapos_backup_$timestamp.zip")

            ZipOutputStream(BufferedOutputStream(FileOutputStream(backupFile))).use { zos ->
                // Add products.csv from internal storage
                addProductsCsvToZip(zos)
                
                // Add receipt files from Documents/WARMAPOS/Struk
                addReceiptsToZip(zos)
                
                // Add grouped receipt files from Documents/WARMAPOS/GroupedStruk
                addGroupedReceiptsToZip(zos)
                
                // Add settings/synonyms if exist
                addSettingsToZip(zos)
            }

            Log.d(TAG, "Backup created: ${backupFile.absolutePath}")
            backupFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create backup", e)
            null
        }
    }

    private fun addProductsCsvToZip(zos: ZipOutputStream) {
        val productsFile = context.getFileStreamPath(PRODUCTS_FILENAME)
        if (productsFile.exists()) {
            zos.putNextEntry(ZipEntry(PRODUCTS_FILENAME))
            productsFile.inputStream().use { it.copyTo(zos) }
            zos.closeEntry()
            Log.d(TAG, "Added products.csv to backup")
        }
    }

    private fun addReceiptsToZip(zos: ZipOutputStream) {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val receiptsDir = File(documentsDir, "WARMAPOS/$RECEIPTS_FOLDER")
        
        if (receiptsDir.exists() && receiptsDir.isDirectory) {
            addDirectoryToZip(zos, receiptsDir, "$RECEIPTS_FOLDER/")
            Log.d(TAG, "Added receipts folder to backup")
        }
    }
    
    private fun addGroupedReceiptsToZip(zos: ZipOutputStream) {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val groupedDir = File(documentsDir, "WARMAPOS/$GROUPED_RECEIPTS_FOLDER")
        
        if (groupedDir.exists() && groupedDir.isDirectory) {
            addDirectoryToZip(zos, groupedDir, "$GROUPED_RECEIPTS_FOLDER/")
            Log.d(TAG, "Added grouped receipts folder to backup")
        }
    }

    private fun addSettingsToZip(zos: ZipOutputStream) {
        // Add synonyms file
        val synonymsFile = context.getFileStreamPath("synonyms.json")
        if (synonymsFile.exists()) {
            zos.putNextEntry(ZipEntry("synonyms.json"))
            synonymsFile.inputStream().use { it.copyTo(zos) }
            zos.closeEntry()
        }
        
        // Add settings file
        val settingsFile = context.getFileStreamPath("settings.json") 
        if (settingsFile.exists()) {
            zos.putNextEntry(ZipEntry("settings.json"))
            settingsFile.inputStream().use { it.copyTo(zos) }
            zos.closeEntry()
        }
    }

    private fun addDirectoryToZip(zos: ZipOutputStream, dir: File, zipPath: String) {
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                addDirectoryToZip(zos, file, "$zipPath${file.name}/")
            } else {
                zos.putNextEntry(ZipEntry("$zipPath${file.name}"))
                file.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }

    /**
     * Restore backup from ZIP file URI
     * Extracts products.csv to internal storage and receipts to Documents folder
     */
    fun restoreBackup(zipUri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    
                    while (entry != null) {
                        when {
                            entry.name == PRODUCTS_FILENAME -> {
                                // Restore products.csv to internal storage
                                context.openFileOutput(PRODUCTS_FILENAME, Context.MODE_PRIVATE).use { out ->
                                    zis.copyTo(out)
                                }
                                Log.d(TAG, "Restored products.csv")
                            }
                            entry.name.startsWith("$RECEIPTS_FOLDER/") && !entry.isDirectory -> {
                                // Restore receipt files
                                restoreExternalFile(entry.name, zis)
                            }
                            entry.name.startsWith("$GROUPED_RECEIPTS_FOLDER/") && !entry.isDirectory -> {
                                // Restore grouped receipt files
                                restoreExternalFile(entry.name, zis)
                            }
                            entry.name == "synonyms.json" -> {
                                context.openFileOutput("synonyms.json", Context.MODE_PRIVATE).use { out ->
                                    zis.copyTo(out)
                                }
                                Log.d(TAG, "Restored synonyms.json")
                            }
                            entry.name == "settings.json" -> {
                                context.openFileOutput("settings.json", Context.MODE_PRIVATE).use { out ->
                                    zis.copyTo(out)
                                }
                                Log.d(TAG, "Restored settings.json")
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
            Log.d(TAG, "Backup restored successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore backup", e)
            false
        }
    }

    private fun restoreExternalFile(entryName: String, zis: ZipInputStream) {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val targetFile = File(documentsDir, "WARMAPOS/$entryName")
        
        // Create parent directories if needed
        targetFile.parentFile?.mkdirs()
        
        FileOutputStream(targetFile).use { out ->
            zis.copyTo(out)
        }
        Log.d(TAG, "Restored receipt: ${targetFile.absolutePath}")
    }

    /**
     * Get list of available backup files
     */
    fun getBackupFiles(): List<BackupInfo> {
        val backupDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            BACKUP_FOLDER
        )
        
        return if (backupDir.exists()) {
            backupDir.listFiles { file -> file.extension == "zip" }
                ?.map { file ->
                    BackupInfo(
                        name = file.name,
                        path = file.absolutePath,
                        size = file.length(),
                        date = Date(file.lastModified())
                    )
                }
                ?.sortedByDescending { it.date }
                ?: emptyList()
        } else {
            emptyList()
        }
    }
}

data class BackupInfo(
    val name: String,
    val path: String,
    val size: Long,
    val date: Date
) {
    fun formattedSize(): String {
        return when {
            size >= 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024.0))
            size >= 1024 -> String.format("%.1f KB", size / 1024.0)
            else -> "$size B"
        }
    }
    
    fun formattedDate(): String {
        return SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id")).format(date)
    }
}
