package com.dicoding.warmapos.data.model

import java.util.UUID

/**
 * Receipt data class for saved transactions
 */
data class Receipt(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val kasir: String = "Kasir",
    val storeName: String = "WARMA STORE",
    val items: List<ReceiptItem>,
    val total: Int,
    val lembarKe: Int = 1,
    val keterangan: String = ""
) {
    fun formattedTotal(): String {
        return "Rp ${String.format("%,d", total).replace(',', '.')}"
    }
}

/**
 * Item in a receipt (snapshot of cart item at time of sale)
 */
data class ReceiptItem(
    val name: String,
    val sku: String = "",
    val price: Int,
    val quantity: Int,
    val unit: String = "pcs"
) {
    val subtotal: Int
        get() = price * quantity

    companion object {
        fun fromCartItem(cartItem: CartItem): ReceiptItem {
            return ReceiptItem(
                name = cartItem.product.name,
                sku = cartItem.product.sku,
                price = cartItem.product.price,
                quantity = cartItem.quantity,
                unit = cartItem.product.unit
            )
        }
    }
}
