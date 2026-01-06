package com.dicoding.warmapos.data.model

/**
 * Product data class representing an item in the inventory
 */
data class Product(
    val name: String,
    val sku: String = "",
    val category: String = "",
    val price: Int,
    val unit: String = "pcs"
) {
    fun formattedPrice(): String {
        return "Rp${String.format("%,d", price).replace(',', '.')}"
    }
}
