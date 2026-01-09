package com.dicoding.warmapos.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.dicoding.warmapos.data.model.ReceiptDesign
import com.google.gson.Gson

/**
 * Repository for app settings
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    private val gson = Gson()

    // Receipt Design
    fun getReceiptDesign(): ReceiptDesign {
        val json = prefs.getString(KEY_RECEIPT_DESIGN, null)
        return if (json != null) {
            try {
                gson.fromJson(json, ReceiptDesign::class.java)
            } catch (e: Exception) {
                ReceiptDesign()
            }
        } else {
            ReceiptDesign()
        }
    }

    fun saveReceiptDesign(design: ReceiptDesign) {
        val json = gson.toJson(design)
        prefs.edit().putString(KEY_RECEIPT_DESIGN, json).apply()
    }

    // Bluetooth Printer
    fun getSavedPrinterAddress(): String? {
        return prefs.getString(KEY_PRINTER_ADDRESS, null)
    }

    fun savePrinterAddress(address: String?) {
        prefs.edit().putString(KEY_PRINTER_ADDRESS, address).apply()
    }

    fun getSavedPrinterName(): String? {
        return prefs.getString(KEY_PRINTER_NAME, null)
    }

    fun savePrinterName(name: String?) {
        prefs.edit().putString(KEY_PRINTER_NAME, name).apply()
    }

    // Kasir Name
    fun getKasirName(): String {
        return prefs.getString(KEY_KASIR_NAME, "Kasir") ?: "Kasir"
    }

    fun saveKasirName(name: String) {
        prefs.edit().putString(KEY_KASIR_NAME, name).apply()
    }

    // OCR API Key
    fun getOcrApiKey(): String {
        return prefs.getString(KEY_OCR_API_KEY, DEFAULT_OCR_API_KEY) ?: DEFAULT_OCR_API_KEY
    }

    fun saveOcrApiKey(key: String) {
        prefs.edit().putString(KEY_OCR_API_KEY, key).apply()
    }

    // App Theme
    fun getAppTheme(): String {
        return prefs.getString(KEY_APP_THEME, "EMERALD") ?: "EMERALD"
    }

    fun saveAppTheme(themeName: String) {
        prefs.edit().putString(KEY_APP_THEME, themeName).apply()
    }

    // ===== OCR API URL =====
    fun getOcrApiUrl(): String {
        return prefs.getString(KEY_OCR_API_URL, DEFAULT_OCR_API_URL) ?: DEFAULT_OCR_API_URL
    }
    
    fun saveOcrApiUrl(url: String) {
        prefs.edit().putString(KEY_OCR_API_URL, url).apply()
    }
    
    // ===== OCR Mode =====
    fun getOcrMode(): String {
        return prefs.getString(KEY_OCR_MODE, OCR_MODE_ONLINE) ?: OCR_MODE_ONLINE
    }
    
    fun saveOcrMode(mode: String) {
        prefs.edit().putString(KEY_OCR_MODE, mode).apply()
    }
    
    fun isOfflineOcrEnabled(): Boolean {
        return getOcrMode() == OCR_MODE_OFFLINE
    }

    // ===== Keterangan Options =====
    fun getKeteranganOptions(): List<String> {
        val json = prefs.getString(KEY_KETERANGAN_OPTIONS, null)
        return if (json != null) {
            try {
                gson.fromJson(json, Array<String>::class.java).toList()
            } catch (e: Exception) {
                DEFAULT_KETERANGAN_OPTIONS
            }
        } else {
            DEFAULT_KETERANGAN_OPTIONS
        }
    }

    fun saveKeteranganOptions(options: List<String>) {
        val json = gson.toJson(options)
        prefs.edit().putString(KEY_KETERANGAN_OPTIONS, json).apply()
    }

    fun addKeteranganOption(option: String) {
        val current = getKeteranganOptions().toMutableList()
        if (!current.contains(option)) {
            current.add(option)
            saveKeteranganOptions(current)
        }
    }

    fun removeKeteranganOption(option: String) {
        val current = getKeteranganOptions().toMutableList()
        current.remove(option)
        saveKeteranganOptions(current)
    }

    companion object {
        private const val PREFS_NAME = "warmapos_settings"
        private const val KEY_RECEIPT_DESIGN = "receipt_design"
        private const val KEY_PRINTER_ADDRESS = "printer_address"
        private const val KEY_PRINTER_NAME = "printer_name"
        private const val KEY_KASIR_NAME = "kasir_name"
        private const val KEY_OCR_API_KEY = "ocr_api_key"
        private const val KEY_OCR_API_URL = "ocr_api_url"
        private const val KEY_OCR_MODE = "ocr_mode"
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_KETERANGAN_OPTIONS = "keterangan_options"
        const val DEFAULT_OCR_API_KEY = "YOUR_OCR_API_KEY"
        const val DEFAULT_OCR_API_URL = "https://api.ocr.space/"
        const val OCR_MODE_ONLINE = "online"
        const val OCR_MODE_OFFLINE = "offline"
        val DEFAULT_KETERANGAN_OPTIONS = listOf("Asli", "Copy", "Rangkap")
    }
}
