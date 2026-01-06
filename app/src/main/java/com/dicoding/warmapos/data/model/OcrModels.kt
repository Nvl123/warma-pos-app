package com.dicoding.warmapos.data.model

/**
 * OCR result item parsed from image text
 */
data class OcrItem(
    val originalText: String,
    val processedText: String,
    val quantity: Int,
    val wasSynonymReplaced: Boolean = false,
    val originalSynonym: String? = null
)

/**
 * Match result from fuzzy product matching
 */
data class ProductMatch(
    val product: Product,
    val score: Float  // 0.0 to 1.0
) {
    val scorePercent: Int
        get() = (score * 100).toInt()
}
