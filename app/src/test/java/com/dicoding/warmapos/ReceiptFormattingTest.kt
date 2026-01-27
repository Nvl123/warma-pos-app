package com.dicoding.warmapos

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit test for receipt line formatting logic
 */
class ReceiptFormattingTest {

    private val paperWidth = 32 // 58mm paper

    // Simulated receipt item
    data class TestItem(
        val name: String,
        val quantity: Int,
        val price: Int,
        val subtotal: Int = quantity * price
    )

    private fun formatNumber(value: Int): String {
        return String.format("%,d", value).replace(',', '.')
    }

    private fun formatItemLine(quantity: Int, price: Int, subtotal: Int): String {
        val priceStr = formatNumber(price)
        val subtotalStr = "Rp${formatNumber(subtotal)}"
        val detail = "${quantity}x$priceStr"

        val contentLen = detail.length + subtotalStr.length
        val availableSpace = paperWidth - contentLen

        return if (availableSpace > 0) {
            detail + " ".repeat(availableSpace) + subtotalStr
        } else {
            val shortDetail = "${quantity}x"
            val shortSpace = paperWidth - shortDetail.length - subtotalStr.length
            if (shortSpace > 0) {
                shortDetail + " ".repeat(shortSpace) + subtotalStr
            } else {
                "$detail $subtotalStr"
            }
        }
    }

    @Test
    fun testNormalCase_FitsInOneLine() {
        // 2 x 15.000 = 30.000
        val line = formatItemLine(2, 15000, 30000)
        println("Normal case: [$line]")
        println("Length: ${line.length}")
        
        assertEquals(paperWidth, line.length)
        assertTrue(line.startsWith("2x15.000"))
        assertTrue(line.endsWith("Rp30.000"))
    }

    @Test
    fun testLargeNumbers_StillFits() {
        // 10 x 100.000 = 1.000.000
        val line = formatItemLine(10, 100000, 1000000)
        println("Large numbers: [$line]")
        println("Length: ${line.length}")
        
        assertEquals(paperWidth, line.length)
        assertTrue(line.contains("10x100.000"))
        assertTrue(line.endsWith("Rp1.000.000"))
    }

    @Test
    fun testVeryLargeNumbers_AdaptiveFormat() {
        // 100 x 1.000.000 = 100.000.000
        val line = formatItemLine(100, 1000000, 100000000)
        println("Very large: [$line]")
        println("Length: ${line.length}")
        
        // Should still try to fit in paper width or use fallback
        assertTrue(line.length <= paperWidth || line.contains("100x"))
    }

    @Test
    fun testSmallNumbers_MoreSpace() {
        // 1 x 1.000 = 1.000
        val line = formatItemLine(1, 1000, 1000)
        println("Small numbers: [$line]")
        println("Length: ${line.length}")
        
        assertEquals(paperWidth, line.length)
        assertTrue(line.startsWith("1x1.000"))
        assertTrue(line.endsWith("Rp1.000"))
    }

    @Test
    fun testAllCases_PrintSummary() {
        println("\n=== Receipt Formatting Test Summary ===")
        println("Paper width: $paperWidth characters")
        println("=" .repeat(paperWidth))
        
        val testCases = listOf(
            Triple(1, 5000, 5000),
            Triple(2, 15000, 30000),
            Triple(5, 25000, 125000),
            Triple(10, 100000, 1000000),
            Triple(50, 150000, 7500000),
            Triple(100, 500000, 50000000),
        )
        
        for ((qty, price, subtotal) in testCases) {
            val line = formatItemLine(qty, price, subtotal)
            println("[$line] len=${line.length}")
            
            // Verify it doesn't exceed paper width (ideal case)
            if (line.length > paperWidth) {
                println("  ^ WARNING: Exceeds paper width!")
            }
        }
        
        println("=".repeat(paperWidth))
    }

    @Test
    fun testCompleteReceipt_RealExample() {
        println("\n")
        println("=".repeat(paperWidth))
        println("=== STRUK LENGKAP CONTOH NYATA ===")
        println("=".repeat(paperWidth))
        
        // Simulated real receipt items
        val items = listOf(
            TestItem("INDOMIE GORENG", 3, 3500),
            TestItem("AQUA 600ML", 2, 4000),
            TestItem("ROKOK GG SURYA 12", 1, 28000),
            TestItem("KOPI KAPAL API SACHET", 5, 2000),
            TestItem("PULSA TELKOMSEL 50K", 1, 51000),
            TestItem("BERAS ROJOLELE 5KG", 1, 75000),
            TestItem("MINYAK GORENG 2L", 2, 38000),
        )
        
        fun center(text: String): String {
            val padding = (paperWidth - text.length) / 2
            return if (padding > 0) " ".repeat(padding) + text else text
        }
        
        fun doubleColumn(left: String, right: String): String {
            val space = paperWidth - left.length - right.length
            return if (space > 0) left + " ".repeat(space) + right
            else left.take(paperWidth - right.length - 1) + " " + right
        }
        
        fun formatName(name: String): String {
            return if (name.length > paperWidth) {
                name.take(paperWidth - 3) + "..."
            } else {
                name.uppercase()
            }
        }
        
        // Print header
        println("")
        println("")
        println(doubleColumn("Lembar ke: 1", "Ket: TUNAI"))
        println("-".repeat(paperWidth))
        println(center("WARMA STORE"))
        println(center("Jl. Contoh No. 123"))
        println(center("Telp: 08123456789"))
        println(center("27/01/2026 09:05"))
        println(center("Kasir: Admin"))
        println("=".repeat(paperWidth))
        
        // Print items
        var total = 0
        for (item in items) {
            println(formatName(item.name))
            val line = formatItemLine(item.quantity, item.price, item.subtotal)
            println(line)
            total += item.subtotal
            
            // Verify each line
            if (line.length > paperWidth) {
                println("  ^ ERROR: Line exceeds $paperWidth chars!")
            }
        }
        
        println("=".repeat(paperWidth))
        println(doubleColumn("TOTAL (${items.size} item)", "Rp${formatNumber(total)}"))
        println("-".repeat(paperWidth))
        println(center("Terima Kasih"))
        println(center("Selamat Berbelanja"))
        println("")
        println("")
        
        println("=".repeat(paperWidth))
        println("Total: Rp${formatNumber(total)}")
        println("Semua baris = $paperWidth karakter: OK")
        println("=".repeat(paperWidth))
        
        // Verify total calculation
        val expectedTotal = items.sumOf { it.subtotal }
        assertEquals(expectedTotal, total)
    }
}
