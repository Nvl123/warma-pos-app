package com.dicoding.warmapos.bluetooth

/**
 * ESC/POS command builder for thermal printers
 */
class EscPosBuilder {

    private val buffer = mutableListOf<Byte>()

    // Paper width in characters (58mm = 32 chars, 80mm = 48 chars)
    var paperWidth: Int = 32

    /**
     * Initialize printer
     */
    fun init(): EscPosBuilder {
        buffer.addAll(ESC_INIT.toList())
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
     */
    fun printDoubleColumn(left: String, right: String): EscPosBuilder {
        val space = paperWidth - left.length - right.length
        val line = if (space > 0) {
            left + " ".repeat(space) + right
        } else {
            left.take(paperWidth - right.length - 1) + " " + right
        }
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
