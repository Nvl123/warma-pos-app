package com.dicoding.warmapos.bluetooth

import com.dicoding.warmapos.data.model.Receipt
import com.dicoding.warmapos.data.model.ReceiptDesign
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Receipt printer utility for formatting and printing receipts
 */
class ReceiptPrinter(
    private val printerManager: BluetoothPrinterManager
) {

    /**
     * Print a receipt
     */
    suspend fun printReceipt(
        receipt: Receipt,
        design: ReceiptDesign
    ): Result<Unit> {
        val builder = EscPosBuilder()
        builder.paperWidth = design.paperWidth

        // Initialize
        builder.init()

        // Header text (if any)
        if (design.headerText.isNotBlank()) {
            builder.alignCenter()
            builder.printLine(design.headerText)
        }

        // Store name
        builder.alignCenter()
        builder.bold(true)
        builder.doubleSize(true)
        builder.printLine(design.storeName)
        builder.doubleSize(false)
        builder.bold(false)

        // Store info
        if (design.storeAddress.isNotBlank()) {
            builder.printLine(design.storeAddress)
        }
        if (design.storePhone.isNotBlank()) {
            builder.printLine(design.storePhone)
        }

        // Date/Time
        if (design.showDateTime) {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            builder.printLine(dateFormat.format(Date(receipt.timestamp)))
        }

        // Kasir
        if (design.showKasir) {
            builder.printLine("Kasir: ${receipt.kasir}")
        }

        builder.alignLeft()
        builder.doubleSeparator()

        // Items
        for (item in receipt.items) {
            // Item name (wrap if too long)
            val name = if (item.name.length > design.paperWidth) {
                item.name.take(design.paperWidth - 3) + "..."
            } else {
                item.name
            }
            builder.printLine(name)

            // Qty x Price = Subtotal
            val detail = "  ${item.quantity} x ${formatNumber(item.price)}"
            val subtotal = formatNumber(item.subtotal)
            builder.printDoubleColumn(detail, subtotal)
        }

        builder.doubleSeparator()

        // Total
        builder.bold(true)
        builder.printDoubleColumn("TOTAL", "Rp${formatNumber(receipt.total)}")
        builder.bold(false)

        builder.separator()

        // Footer
        builder.alignCenter()
        builder.printLine(design.footerText)

        // Feed and cut
        builder.feed(3)
        builder.cut()

        return printerManager.sendRaw(builder.build())
    }

    /**
     * Generate receipt text for preview - Modern design
     */
    fun generatePreview(
        receipt: Receipt,
        design: ReceiptDesign
    ): String {
        val lines = mutableListOf<String>()
        val width = design.paperWidth

        fun center(text: String): String {
            val padding = (width - text.length) / 2
            return if (padding > 0) " ".repeat(padding) + text else text
        }

        fun rightAlign(text: String): String {
            val padding = width - text.length
            return if (padding > 0) " ".repeat(padding) + text else text
        }

        fun doubleColumn(left: String, right: String): String {
            val space = width - left.length - right.length
            return if (space > 0) left + " ".repeat(space) + right
            else left.take(width - right.length - 1) + " " + right
        }

        // Top border
        lines.add("╔${"═".repeat(width - 2)}╗")
        
        // Header text
        if (design.headerText.isNotBlank()) {
            lines.add("║${center(design.headerText).padEnd(width - 2)}║")
        }

        // Store name (big)
        lines.add("║${center("★ ${design.storeName} ★").padEnd(width - 2)}║")

        if (design.storeAddress.isNotBlank()) {
            lines.add("║${center(design.storeAddress).padEnd(width - 2)}║")
        }
        if (design.storePhone.isNotBlank()) {
            lines.add("║${center("☎ ${design.storePhone}").padEnd(width - 2)}║")
        }

        // Separator
        lines.add("╠${"═".repeat(width - 2)}╣")

        // Date/Time and Kasir
        if (design.showDateTime) {
            val dateFormat = SimpleDateFormat("dd MMM yyyy  HH:mm", Locale.getDefault())
            lines.add("║${center("📅 ${dateFormat.format(Date(receipt.timestamp))}").padEnd(width - 2)}║")
        }

        if (design.showKasir) {
            lines.add("║${center("👤 Kasir: ${receipt.kasir}").padEnd(width - 2)}║")
        }

        // Items header
        lines.add("╠${"═".repeat(width - 2)}╣")
        lines.add("║${center("--- DAFTAR BELANJA ---").padEnd(width - 2)}║")
        lines.add("║${" ".repeat(width - 2)}║")

        // Items
        for (item in receipt.items) {
            val name = if (item.name.length > width - 4) {
                item.name.take(width - 7) + "..."
            } else {
                item.name
            }
            lines.add("║ • $name".padEnd(width - 1) + "║")

            val detail = "   ${item.quantity}x @${formatNumber(item.price)}"
            val subtotal = "Rp${formatNumber(item.subtotal)}"
            lines.add("║${doubleColumn(detail, subtotal).padEnd(width - 2)}║")
        }

        // Total section
        lines.add("╠${"═".repeat(width - 2)}╣")
        
        val totalLabel = "  TOTAL (${receipt.items.size} item)"
        val totalValue = "Rp${formatNumber(receipt.total)}"
        lines.add("║${doubleColumn(totalLabel, totalValue).padEnd(width - 2)}║")
        
        lines.add("╠${"═".repeat(width - 2)}╣")

        // Footer
        lines.add("║${center(design.footerText).padEnd(width - 2)}║")
        lines.add("║${center("✨ Terima Kasih ✨").padEnd(width - 2)}║")
        
        // Bottom border
        lines.add("╚${"═".repeat(width - 2)}╝")

        return lines.joinToString("\n")
    }

    private fun formatNumber(value: Int): String {
        return String.format("%,d", value).replace(',', '.')
    }
}
