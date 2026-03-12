package com.dicoding.warmapos.bluetooth

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.dicoding.warmapos.R
import com.dicoding.warmapos.data.model.Receipt
import com.dicoding.warmapos.data.model.ReceiptDesign
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Renders a Receipt as a Bitmap image using Poppins font.
 *
 * Font loading strategy: extract TTF from res/font to a temp file,
 * then use Typeface.createFromFile() which is guaranteed thread-safe
 * and works reliably from Dispatchers.IO.
 *
 * Digit clarity: price numbers use larger text + letterSpacing so
 * digits 5, 6, 8 are visually distinct on a thermal printer.
 */
class ReceiptBitmapRenderer(private val context: Context) {

    // --------------- Font loading (thread-safe) ---------------

    private fun loadTypeface(resId: Int, fallback: Typeface): Typeface {
        return try {
            val resName = context.resources.getResourceEntryName(resId)
            val tmpFile = File(context.cacheDir, "$resName.ttf")

            if (!tmpFile.exists() || tmpFile.length() == 0L) {
                // res/font files must be opened via openRawResourceFd
                val fd = context.resources.openRawResourceFd(resId)
                fd?.use { rawFd ->
                    rawFd.createInputStream().use { input ->
                        FileOutputStream(tmpFile).use { out -> input.copyTo(out) }
                    }
                }
            }
            if (tmpFile.exists() && tmpFile.length() > 0L) {
                Typeface.createFromFile(tmpFile)
            } else {
                fallback
            }
        } catch (e: Exception) {
            android.util.Log.w("ReceiptBitmapRenderer", "Font load fallback for res $resId: ${e.message}")
            fallback
        }
    }

    private val typefaceRegular: Typeface by lazy { loadTypeface(R.font.poppins_regular,  Typeface.DEFAULT) }
    private val typefaceSemiBold: Typeface by lazy { loadTypeface(R.font.poppins_semibold, Typeface.DEFAULT_BOLD) }
    private val typefaceBold: Typeface by lazy    { loadTypeface(R.font.poppins_bold,     Typeface.DEFAULT_BOLD) }

    // --------------- Render entry point ---------------

    fun render(receipt: Receipt, design: ReceiptDesign): Bitmap {
        // Full paper width: 58mm = 384 dots, 80mm = 576 dots (1 dot per pixel in GS v 0)
        val widthPx = if (design.paperWidth >= 48) 576 else 384
        val paddingH = 8f  // minimal padding to maximize usable paper width

        val ops = buildDrawOps(receipt, design)
        val totalHeight = ops.sumOf { it.height } + (paddingH * 2).toInt() + 60

        val bitmap = Bitmap.createBitmap(widthPx, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        var y = paddingH
        val contentWidth = widthPx - paddingH * 2
        for (op in ops) {
            op.draw(canvas, paddingH, y, contentWidth)
            y += op.height
        }
        return bitmap
    }

    // --------------- Draw Operation Model ---------------

    private interface DrawOp {
        val height: Int
        fun draw(canvas: Canvas, left: Float, top: Float, width: Float)
    }

    private fun makePaint(
        sizeF: Float,
        tf: Typeface,
        align: Paint.Align = Paint.Align.LEFT,
        letterSpacing: Float = 0f
    ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sizeF
        typeface = tf
        color = Color.BLACK
        textAlign = align
        this.letterSpacing = letterSpacing
    }

    /** Single text line */
    private inner class TextOp(
        val text: String,
        val textSizePx: Float,
        val tf: Typeface,
        val align: Paint.Align = Paint.Align.LEFT,
        val spacingAfter: Float = 4f,
        val letterSpacing: Float = 0f
    ) : DrawOp {
        private val paint = makePaint(textSizePx, tf, align, letterSpacing)
        private val fm = paint.fontMetrics
        override val height: Int = (-fm.ascent + fm.descent + spacingAfter).toInt()
        override fun draw(canvas: Canvas, left: Float, top: Float, width: Float) {
            val x = when (align) {
                Paint.Align.CENTER -> left + width / 2f
                Paint.Align.RIGHT  -> left + width
                else               -> left
            }
            canvas.drawText(text, x, top + (-fm.ascent), paint)
        }
    }

    /** Two columns (left-aligned + right-aligned) on the same baseline */
    private inner class TwoColOp(
        val left: String,
        val right: String,
        val textSizePx: Float,
        val tfLeft: Typeface,
        val tfRight: Typeface,
        val spacingAfter: Float = 4f,
        val letterSpacingRight: Float = 0.05f   // slight spacing for price digits
    ) : DrawOp {
        private val lp = makePaint(textSizePx, tfLeft,  Paint.Align.LEFT)
        private val rp = makePaint(textSizePx, tfRight, Paint.Align.RIGHT, letterSpacingRight)
        private val fm = lp.fontMetrics
        override val height: Int = (-fm.ascent + fm.descent + spacingAfter).toInt()
        override fun draw(canvas: Canvas, left: Float, top: Float, width: Float) {
            val baseline = top + (-fm.ascent)
            canvas.drawText(this.left, left, baseline, lp)
            canvas.drawText(right, left + width, baseline, rp)
        }
    }

    private inner class DivOp(val double: Boolean = false) : DrawOp {
        override val height = if (double) 22 else 16
        override fun draw(canvas: Canvas, left: Float, top: Float, width: Float) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; strokeWidth = 1.5f; style = Paint.Style.STROKE }
            val mid = top + height / 2f
            canvas.drawLine(left, mid - if (double) 3f else 0f, left + width, mid - if (double) 3f else 0f, p)
            if (double) canvas.drawLine(left, mid + 3f, left + width, mid + 3f, p)
        }
    }

    private inner class SpaceOp(override val height: Int) : DrawOp {
        override fun draw(canvas: Canvas, left: Float, top: Float, width: Float) {}
    }

    // --------------- Build draw ops ---------------

    private fun buildDrawOps(receipt: Receipt, design: ReceiptDesign): List<DrawOp> {
        val ops = mutableListOf<DrawOp>()

        // Lembar ke / keterangan
        val ket = receipt.keterangan.trim()
        if (ket.isNotBlank()) {
            ops += TwoColOp("Lembar ke: ${receipt.lembarKe}", "Ket: $ket", 22f, typefaceRegular, typefaceRegular, 4f, 0f)
        } else {
            ops += TextOp("Lembar ke: ${receipt.lembarKe}", 22f, typefaceRegular, spacingAfter = 4f)
        }

        ops += SpaceOp(6)
        ops += DivOp()
        ops += SpaceOp(6)

        if (design.headerText.isNotBlank()) {
            ops += TextOp(design.headerText, 22f, typefaceRegular, Paint.Align.CENTER, 4f)
        }

        // ── HEADER (fixed / changes only in Settings) ────────────────
        ops += TextOp("★  ${design.storeName}  ★", 34f, typefaceBold, Paint.Align.CENTER, 6f)
        if (design.storeAddress.isNotBlank()) ops += TextOp(design.storeAddress, 20f, typefaceRegular, Paint.Align.CENTER, 3f)
        if (design.storePhone.isNotBlank())   ops += TextOp("☎ ${design.storePhone}",   20f, typefaceRegular, Paint.Align.CENTER, 3f)
        if (design.showDateTime) {
            val df = SimpleDateFormat("dd MMM yyyy  HH:mm", Locale.getDefault())
            ops += TextOp(df.format(Date(receipt.timestamp)), 20f, typefaceRegular, Paint.Align.CENTER, 3f)
        }
        if (design.showKasir) ops += TextOp("Kasir: ${receipt.kasir}", 20f, typefaceRegular, Paint.Align.CENTER, 6f)

        ops += SpaceOp(4)
        ops += DivOp(double = true)
        ops += TextOp("─── DAFTAR BELANJA ───", 20f, typefaceSemiBold, Paint.Align.CENTER, 8f)

        // ── ITEMS (dynamic) ──────────────────────────────────────────
        for (item in receipt.items) {
            // Item name — SemiBold, uppercase
            ops += TextOp(item.name.uppercase(), 26f, typefaceSemiBold, spacingAfter = 2f)

            // Qty × price | subtotal
            // letterSpacing = 0.08f → digits are visually separated → 5/6/8 more distinct
            ops += TwoColOp(
                left = "  ${item.quantity} × ${fmt(item.price)}",
                right = "Rp ${fmt(item.subtotal)}",
                textSizePx = 28f,          // larger than before → each digit gets more dots on paper
                tfLeft = typefaceRegular,
                tfRight = typefaceSemiBold,
                spacingAfter = 12f,
                letterSpacingRight = 0.08f // key: spread digits for thermal clarity
            )
        }

        // ── FOOTER (fixed) ───────────────────────────────────────────
        ops += DivOp(double = true)
        ops += SpaceOp(6)

        // TOTAL — split into two lines (no overlap possible)
        ops += TextOp("TOTAL  (${receipt.items.size} item)", 22f, typefaceSemiBold, Paint.Align.LEFT, 2f)
        ops += TextOp(
            text = "Rp ${fmt(receipt.total)}",
            textSizePx = 34f,             // large so the total amount is very readable
            tf = typefaceBold,
            align = Paint.Align.RIGHT,
            spacingAfter = 14f,
            letterSpacing = 0.08f
        )

        ops += DivOp()
        ops += SpaceOp(10)

        if (design.footerText.isNotBlank()) ops += TextOp(design.footerText, 20f, typefaceRegular, Paint.Align.CENTER, 20f)

        return ops
    }

    private fun fmt(value: Int): String = String.format("%,d", value).replace(',', '.')
}
