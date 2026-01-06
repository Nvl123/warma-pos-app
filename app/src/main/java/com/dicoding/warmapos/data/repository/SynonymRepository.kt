package com.dicoding.warmapos.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Repository for managing synonym dictionary
 */
class SynonymRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    private val gson = Gson()

    private var synonyms: MutableMap<String, String> = mutableMapOf()

    init {
        loadSynonyms()
    }

    /**
     * Load synonyms from SharedPreferences
     */
    private fun loadSynonyms() {
        val json = prefs.getString(KEY_SYNONYMS, null)
        if (json != null) {
            try {
                val type = object : TypeToken<Map<String, String>>() {}.type
                synonyms = gson.fromJson<Map<String, String>>(json, type).toMutableMap()
            } catch (e: Exception) {
                synonyms = mutableMapOf()
            }
        }
    }

    /**
     * Save synonyms to SharedPreferences
     */
    private fun saveSynonyms() {
        val json = gson.toJson(synonyms)
        prefs.edit().putString(KEY_SYNONYMS, json).apply()
    }

    /**
     * Get all synonyms
     */
    fun getSynonyms(): Map<String, String> = synonyms.toMap()

    /**
     * Add or update a synonym
     */
    fun addSynonym(key: String, value: String) {
        synonyms[key.lowercase().trim()] = value.trim()
        saveSynonyms()
    }

    /**
     * Remove a synonym
     */
    fun removeSynonym(key: String) {
        synonyms.remove(key.lowercase().trim())
        saveSynonyms()
    }

    /**
     * Apply synonyms to text
     * Returns: Pair(replaced text, was replaced?)
     */
    fun applySynonyms(text: String): Triple<String, Boolean, String?> {
        val textLower = text.lowercase().trim()
        for ((key, value) in synonyms) {
            if (key == textLower) {
                return Triple(value, true, key)
            }
        }
        return Triple(text, false, null)
    }

    fun getCount(): Int = synonyms.size

    fun clear() {
        synonyms.clear()
        saveSynonyms()
    }

    companion object {
        private const val PREFS_NAME = "warmapos_synonyms"
        private const val KEY_SYNONYMS = "synonyms"
    }
}
