package com.dicoding.warmapos.data.model

import java.util.UUID

/**
 * Cart item with product and quantity
 */
data class CartItem(
    val id: String = UUID.randomUUID().toString(),
    val product: Product,
    val quantity: Int = 1
) {
    val subtotal: Int
        get() = product.price * quantity

    fun formattedSubtotal(): String {
        return "Rp ${String.format("%,d", subtotal).replace(',', '.')}"
    }
}
