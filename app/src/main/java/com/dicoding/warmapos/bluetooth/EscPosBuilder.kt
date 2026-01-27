package com.dicoding.warmapos.bluetooth

/**
 * ESC/POS command builder for thermal printers
 */
class EscPosBuilder {

    private val buffer = mutableListOf<Byte>()

    // Paper width in characters (58mm = 32 chars, 80mm = 48 chars)
    var paperWidth: Int = 32

    /**
     * Initialize printer with optimized settings for clearer number printing
     */
    fun init(): EscPosBuilder {
        buffer.addAll(ESC_INIT.toList())
        // Set character spacing to 1 dot for clearer numbers (especially 8 and 6)
        setCharacterSpacing(1)
        return this
    }
    
    /**
     * Set character spacing (horizontal space between characters)
     * Helps prevent numbers like 8 and 6 from appearing connected
     * @param dots Number of dots to add between characters (0-255, recommended: 1-2)
     */
    fun setCharacterSpacing(dots: Int = 1): EscPosBuilder {
        // ESC SP n - Set right-side character spacing
        buffer.addAll(byteArrayOf(0x1B, 0x20, dots.coerceIn(0, 255).toByte()).toList())
        return this
    }
    
    /**
     * Select font (Font A or Font B)
     * Font B is typically narrower and may print numbers more clearly
     */
    fun selectFont(fontB: Boolean = false): EscPosBuilder {
        // ESC M n - Select font
        buffer.addAll(byteArrayOf(0x1B, 0x4D, if (fontB) 0x01 else 0x00).toList())
        return this
    }
    
    /**
     * Set print density/darkness
     * Lower density may help prevent number bleeding on some printers
     * @param heating Heating dots (0-7), lower = lighter print
     * @param breakTime Break time (0-7), higher = more cooling time
     */
    fun setPrintDensity(heating: Int = 7, breakTime: Int = 7): EscPosBuilder {
        // DC2 # n - Set print density
        val density = ((breakTime and 0x07) shl 5) or (heating and 0x1F)
        buffer.addAll(byteArrayOf(0x12, 0x23, density.toByte()).toList())
        return this
    }

    /**
     * Align text left
     */
    fun alignLeft(): EscPosBuilder {
        buffer.addAll(ALIGN_LEFT.toList())
        return this
    }

    /**
     * Align text center
     */
    fun alignCenter(): EscPosBuilder {
        buffer.addAll(ALIGN_CENTER.toList())
        return this
    }

    /**
     * Align text right
     */
    fun alignRight(): EscPosBuilder {
        buffer.addAll(ALIGN_RIGHT.toList())
        return this
    }

    /**
     * Set bold on/off
     */
    fun bold(enabled: Boolean): EscPosBuilder {
        buffer.addAll(if (enabled) BOLD_ON.toList() else BOLD_OFF.toList())
        return this
    }

    /**
     * Set double size on/off
     */
    fun doubleSize(enabled: Boolean): EscPosBuilder {
        buffer.addAll(if (enabled) DOUBLE_SIZE.toList() else NORMAL_SIZE.toList())
        return this
    }

    /**
     * Set line spacing (default 30 dots for better readability)
     */
    fun setLineSpacing(dots: Int = 30): EscPosBuilder {
        buffer.addAll(byteArrayOf(0x1B, 0x33, dots.toByte()).toList())
        return this
    }

    /**
     * Reset line spacing to default
     */
    fun resetLineSpacing(): EscPosBuilder {
        buffer.addAll(byteArrayOf(0x1B, 0x32).toList())
        return this
    }

    /**
     * Print text
     */
    fun print(text: String): EscPosBuilder {
        val bytes = text.toByteArray(Charsets.ISO_8859_1)
        buffer.addAll(bytes.toList())
        return this
    }

    /**
     * Print text with newline
     */
    fun printLine(text: String = ""): EscPosBuilder {
        print(text)
        buffer.add(NEWLINE)
        return this
    }

    /**
     * Print two columns (left and right aligned)
     * Reduce space between columns if needed (can be 0), don't truncate
     */
    fun printDoubleColumn(left: String, right: String): EscPosBuilder {
        val space = paperWidth - left.length - right.length
        // Allow 0 space if needed to fit in one line
        val line = left + " ".repeat(space.coerceAtLeast(0)) + right
        printLine(line)
        return this
    }

    /**
     * Print separator line
     */
    fun separator(char: Char = '-'): EscPosBuilder {
        printLine(char.toString().repeat(paperWidth))
        return this
    }

    /**
     * Print double separator
     */
    fun doubleSeparator(): EscPosBuilder {
        printLine("=".repeat(paperWidth))
        return this
    }

    /**
     * Feed paper
     */
    fun feed(lines: Int = 1): EscPosBuilder {
        repeat(lines) {
            buffer.add(NEWLINE)
        }
        return this
    }

    /**
     * Cut paper
     */
    fun cut(partial: Boolean = true): EscPosBuilder {
        buffer.addAll(if (partial) CUT_PARTIAL.toList() else CUT_FULL.toList())
        return this
    }

    /**
     * Build the byte array
     */
    fun build(): ByteArray {
        return buffer.toByteArray()
    }

    companion object {
        // ESC/POS Commands
        private val ESC_INIT = byteArrayOf(0x1B, 0x40)         // Initialize printer

        private val ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
        private val ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
        private val ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)

        private val BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
        private val BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)

        private val DOUBLE_SIZE = byteArrayOf(0x1B, 0x21, 0x30)
        private val NORMAL_SIZE = byteArrayOf(0x1B, 0x21, 0x00)

        private val CUT_FULL = byteArrayOf(0x1D, 0x56, 0x00)
        private val CUT_PARTIAL = byteArrayOf(0x1D, 0x56, 0x01)

        private const val NEWLINE: Byte = 0x0A
    }
}
