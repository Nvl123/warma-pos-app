package com.dicoding.warmapos.utils

import com.dicoding.warmapos.data.model.Product
import com.dicoding.warmapos.data.model.ProductMatch
import kotlin.math.max
import kotlin.math.min

/**
 * Product matcher with fuzzy string matching
 */
class ProductMatcher(private var products: List<Product> = emptyList()) {

    fun setProducts(products: List<Product>) {
        this.products = products
    }

    /**
     * Find products matching the query with fuzzy matching
     */
    fun findMatches(query: String, topK: Int = 5, threshold: Float = 0.4f): List<ProductMatch> {
        if (query.isBlank() || products.isEmpty()) {
            return emptyList()
        }

        val queryLower = query.lowercase().trim()

        return products
            .map { product ->
                val score = tokenSetRatio(queryLower, product.name.lowercase())
                ProductMatch(product, score)
            }
            .filter { it.score >= threshold }
            .sortedByDescending { it.score }
            .take(topK)
    }

    /**
     * Get best match for query
     */
    fun getBestMatch(query: String): ProductMatch? {
        val matches = findMatches(query, topK = 1)
        return matches.firstOrNull()
    }

    /**
     * Token set ratio algorithm (similar to rapidfuzz)
     * Better for multi-word matching
     */
    private fun tokenSetRatio(s1: String, s2: String): Float {
        val tokens1 = s1.split(Regex("\\s+")).filter { it.isNotEmpty() }.toSet()
        val tokens2 = s2.split(Regex("\\s+")).filter { it.isNotEmpty() }.toSet()

        val intersection = tokens1.intersect(tokens2)
        val diff1to2 = tokens1 - tokens2
        val diff2to1 = tokens2 - tokens1

        val sorted1 = intersection.sorted().joinToString(" ")
        val sorted2 = (intersection + diff1to2).sorted().joinToString(" ")
        val sorted3 = (intersection + diff2to1).sorted().joinToString(" ")

        val ratios = listOf(
            levenshteinRatio(sorted1, sorted2),
            levenshteinRatio(sorted1, sorted3),
            levenshteinRatio(sorted2, sorted3),
            levenshteinRatio(s1, s2)
        )

        return ratios.maxOrNull() ?: 0f
    }

    /**
     * Levenshtein distance ratio (0.0 to 1.0)
     */
    private fun levenshteinRatio(s1: String, s2: String): Float {
        if (s1.isEmpty() && s2.isEmpty()) return 1f
        if (s1.isEmpty() || s2.isEmpty()) return 0f

        val distance = levenshteinDistance(s1, s2)
        val maxLen = max(s1.length, s2.length)

        return 1f - (distance.toFloat() / maxLen)
    }

    /**
     * Levenshtein distance calculation
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length

        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }

        return dp[m][n]
    }

    fun getProductCount(): Int = products.size

    /**
     * Simple search - finds all products containing the query text (substring match)
     * Results are sorted by relevance: exact match > starts with > contains
     * No limit on output count - returns ALL matching products
     */
    fun simpleSearch(query: String): List<Product> {
        if (query.isBlank() || products.isEmpty()) {
            return emptyList()
        }

        val queryLower = query.lowercase().trim()
        val queryTokens = queryLower.split(Regex("\\s+")).filter { it.isNotEmpty() }

        return products
            .filter { product ->
                val nameLower = product.name.lowercase()
                // Match if all query tokens are found in product name
                queryTokens.all { token -> nameLower.contains(token) }
            }
            .sortedWith(compareBy(
                // Priority 1: Exact match (highest)
                { product -> if (product.name.lowercase() == queryLower) 0 else 1 },
                // Priority 2: Starts with the query
                { product -> if (product.name.lowercase().startsWith(queryLower)) 0 else 1 },
                // Priority 3: Alphabetical order
                { product -> product.name.lowercase() }
            ))
    }

    /**
     * Get keyword suggestions based on partial query (like Google autocomplete)
     * Returns unique keywords/product names that match the input
     */
    fun getSuggestions(query: String, maxSuggestions: Int = 8): List<String> {
        if (query.isBlank() || products.isEmpty()) {
            return emptyList()
        }

        val queryLower = query.lowercase().trim()
        
        // Get unique keywords from product names
        val keywords = mutableSetOf<String>()
        
        products.forEach { product ->
            val nameLower = product.name.lowercase()
            
            // Add product name if it contains the query
            if (nameLower.contains(queryLower)) {
                // Extract most relevant keyword part
                val words = product.name.split(Regex("\\s+"))
                
                // Add individual words that match
                words.forEach { word ->
                    if (word.lowercase().startsWith(queryLower) && word.length > 2) {
                        keywords.add(word)
                    }
                }
                
                // Add 2-word combinations that start with matching word
                for (i in words.indices) {
                    if (words[i].lowercase().startsWith(queryLower)) {
                        // Add 2-word combination
                        if (i + 1 < words.size) {
                            keywords.add("${words[i]} ${words[i + 1]}")
                        }
                        // Add single word
                        if (words[i].length > 2) {
                            keywords.add(words[i])
                        }
                    }
                }

                // Add full product name if short enough and starts with query
                if (product.name.length <= 30 && nameLower.startsWith(queryLower)) {
                    keywords.add(product.name)
                }
            }
        }

        return keywords
            .filter { it.lowercase() != queryLower && it.lowercase().startsWith(queryLower) }
            .sortedWith(compareBy(
                // Priority 1: Length (shorter suggestions first)
                { it.length },
                // Priority 2: Alphabetical
                { it.lowercase() }
            ))
            .take(maxSuggestions)
    }
}
