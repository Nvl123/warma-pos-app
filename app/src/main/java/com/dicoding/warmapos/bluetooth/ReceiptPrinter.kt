package com.dicoding.warmapos.bluetooth

import android.content.Context
import com.dicoding.warmapos.data.model.Receipt
import com.dicoding.warmapos.data.model.ReceiptDesign
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Receipt printer utility for formatting and printing receipts.
 * Renders the receipt as a Poppins-font Bitmap image before sending to the printer.
 */
class ReceiptPrinter(
    private val printerManager: BluetoothPrinterManager,
    private val context: Context
) {

    /**
     * Print a receipt as a Poppins-font bitmap image.
     * Falls back to plain text if bitmap rendering fails.
     */
    suspend fun printReceipt(
        receipt: Receipt,
        design: ReceiptDesign
    ): Result<Unit> {
        try {
            // --- Bitmap path (primary) ---
            val renderer = ReceiptBitmapRenderer(context)
            val bitmap = renderer.render(receipt, design)

            val maxWidthDots = if (design.paperWidth >= 48) 576 else 384

            val builder = EscPosBuilder()
            builder.init()
            builder.alignCenter()  // align center for the image
            builder.printBitmap(bitmap, maxWidthDots)
            bitmap.recycle()
            builder.feed(3)
            builder.cut()

            return printerManager.sendRaw(builder.build())
        } catch (e: Exception) {
            android.util.Log.e("ReceiptPrinter", "Bitmap print failed, falling back to text: ${e.message}")
            return printReceiptText(receipt, design)
        }
    }

    /**
     * Fallback: plain text receipt using ESC/POS text commands
     */
    private suspend fun printReceiptText(
        receipt: Receipt,
        design: ReceiptDesign
    ): Result<Unit> {
        val builder = EscPosBuilder()
        builder.paperWidth = design.paperWidth
        builder.init()
        builder.feed(2)

        // Lembar Ke / Keterangan
        val lembarText = "Lembar ke: ${receipt.lembarKe}"
        val ketText = if (receipt.keterangan.isNotBlank()) "Ket: ${receipt.keterangan}" else ""
        builder.alignLeft()
        if (ketText.isNotBlank()) builder.printDoubleColumn(lembarText, ketText)
        else builder.printLine(lembarText)
        builder.separator()

        if (design.headerText.isNotBlank()) {
            builder.alignCenter()
            builder.printLine(design.headerText)
        }

        builder.alignCenter()
        builder.bold(true)
        builder.doubleSize(true)
        builder.printLine(design.storeName)
        builder.doubleSize(false)
        builder.bold(false)

        if (design.storeAddress.isNotBlank()) builder.printLine(design.storeAddress)
        if (design.storePhone.isNotBlank()) builder.printLine(design.storePhone)

        if (design.showDateTime) {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            builder.printLine(dateFormat.format(Date(receipt.timestamp)))
        }
        if (design.showKasir) builder.printLine("Kasir: ${receipt.kasir}")

        builder.alignLeft()
        builder.doubleSeparator()
        builder.setLineSpacing(32)
        builder.setCharacterSpacing(0)

        for (item in receipt.items) {
            val name = item.name.uppercase().let {
                if (it.length > design.paperWidth) it.take(design.paperWidth - 3) + "..." else it
            }
            builder.bold(true)
            builder.printLine(name)
            builder.bold(false)

            val priceStr = formatNumber(item.price)
            val subtotalStr = "Rp${formatNumber(item.subtotal)}"
            val detail = "${item.quantity} x $priceStr"
            val space = design.paperWidth - detail.length - subtotalStr.length
            val line = if (space > 0) detail + " ".repeat(space) + subtotalStr else "$detail $subtotalStr"
            builder.printLine(line)
        }

        builder.resetLineSpacing()
        builder.doubleSeparator()
        builder.bold(true)
        builder.printDoubleColumn("TOTAL", "Rp ${formatNumber(receipt.total)}")
        builder.bold(false)
        builder.separator()
        builder.alignCenter()
        builder.printLine(design.footerText)
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

        // Add 2 blank lines at top for spacing
        lines.add("")
        lines.add("")

        // Lembar Ke and Keterangan row (left-right aligned)
        fun doubleColumnSimple(left: String, right: String): String {
            val space = width - left.length - right.length
            return if (space > 0) left + " ".repeat(space) + right
            else left.take(width - right.length - 1) + " " + right
        }
        val lembarText = "Lembar ke: ${receipt.lembarKe}"
        val ketText = if (receipt.keterangan.isNotBlank()) "Ket: ${receipt.keterangan}" else ""
        if (ketText.isNotBlank()) {
            lines.add(doubleColumnSimple(lembarText, ketText))
        } else {
            lines.add(lembarText)
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

        // Items - UPPERCASE
        for (item in receipt.items) {
            val upperName = item.name.uppercase()
            val name = if (upperName.length > width - 4) {
                upperName.take(width - 7) + "..."
            } else {
                upperName
            }
            lines.add("║ • $name".padEnd(width - 1) + "║")

            val detail = "   ${item.quantity} x @${formatNumber(item.price)}"
            val subtotal = "Rp ${formatNumber(item.subtotal)}"
            lines.add("║${doubleColumn(detail, subtotal).padEnd(width - 2)}║")
        }

        // Total section
        lines.add("╠${"═".repeat(width - 2)}╣")
        
        val totalLabel = "  TOTAL (${receipt.items.size} item)"
        val totalValue = "Rp ${formatNumber(receipt.total)}"
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
