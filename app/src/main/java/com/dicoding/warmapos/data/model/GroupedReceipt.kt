package com.dicoding.warmapos.data.model

/**
 * Represents a group of receipts combined together
 */
data class GroupedReceipt(
    val id: String, // UUID
    val timestamp: Long,
    val name: String, // e.g., "Kelompok Struk 08 Jan"
    val totalAmount: Int,
    val receiptCount: Int,
    val receiptPaths: List<String>, // List of file paths to original receipt JSONs
    val receipts: List<ReceiptSnapshot> = emptyList() // Snapshot of data for display/printing
)

/**
 * Minimal snapshot of a receipt for grouped display
 */
data class ReceiptSnapshot(
    val id: String,
    val timestamp: Long,
    val total: Int,
    val customerName: String = "Pelanggan" // If we had customer names
)
