package com.dicoding.warmapos.data.model

/**
 * Receipt design settings for customizing printed receipts
 */
data class ReceiptDesign(
    val storeName: String = "WARMA STORE",
    val storeAddress: String = "",
    val storePhone: String = "",
    val headerText: String = "",
    val footerText: String = "Terima Kasih!",
    val showDateTime: Boolean = true,
    val showKasir: Boolean = true,
    val paperWidth: Int = 32  // Characters per line for 58mm paper
)
