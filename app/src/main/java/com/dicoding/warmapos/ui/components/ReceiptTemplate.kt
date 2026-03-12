package com.dicoding.warmapos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dicoding.warmapos.R
import com.dicoding.warmapos.data.model.Receipt
import com.dicoding.warmapos.data.model.ReceiptDesign
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Poppins font family using res/font TTFs
val PoppinsFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

/**
 * Visual receipt template rendered in Compose with Poppins font.
 * This is exactly what will be printed — header/footer are fixed, only items change.
 */
@Composable
fun ReceiptTemplate(
    receipt: Receipt,
    design: ReceiptDesign,
    modifier: Modifier = Modifier,
    paperWidthDp: Dp = 280.dp
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy  HH:mm", Locale.getDefault())

    Column(
        modifier = modifier
            .width(paperWidthDp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ─── HEADER (fixed, changes only with settings) ───────────────

        // Lembar ke + keterangan
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ReceiptText("Lembar ke: ${receipt.lembarKe}", FontWeight.Normal, 10)
            if (receipt.keterangan.isNotBlank()) {
                ReceiptText("Ket: ${receipt.keterangan}", FontWeight.Normal, 10)
            }
        }

        Spacer(Modifier.height(4.dp))
        ReceiptDivider()
        Spacer(Modifier.height(6.dp))

        if (design.headerText.isNotBlank()) {
            ReceiptText(design.headerText, FontWeight.Normal, 10, align = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
        }

        // Store name
        ReceiptText("★  ${design.storeName}  ★", FontWeight.Bold, 18, align = TextAlign.Center)
        Spacer(Modifier.height(2.dp))

        if (design.storeAddress.isNotBlank()) {
            ReceiptText(design.storeAddress, FontWeight.Normal, 10, align = TextAlign.Center)
        }
        if (design.storePhone.isNotBlank()) {
            ReceiptText("☎ ${design.storePhone}", FontWeight.Normal, 10, align = TextAlign.Center)
        }

        if (design.showDateTime) {
            ReceiptText(
                "📅 ${dateFormat.format(Date(receipt.timestamp))}",
                FontWeight.Normal, 10, align = TextAlign.Center
            )
        }
        if (design.showKasir) {
            ReceiptText("👤 Kasir: ${receipt.kasir}", FontWeight.Normal, 10, align = TextAlign.Center)
        }

        Spacer(Modifier.height(4.dp))
        ReceiptDoubleDivider()
        ReceiptText("― DAFTAR BELANJA ―", FontWeight.SemiBold, 10, align = TextAlign.Center)
        Spacer(Modifier.height(6.dp))

        // ─── ITEMS (dynamic, changes per receipt) ────────────────────
        for (item in receipt.items) {
            // Item name
            Text(
                text = item.name.uppercase(),
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = Color.Black,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            // Qty × Price  |  Subtotal
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ReceiptText("${item.quantity} × @${fmt(item.price)}", FontWeight.Normal, 11)
                ReceiptText("Rp ${fmt(item.subtotal)}", FontWeight.SemiBold, 11)
            }
            Spacer(Modifier.height(6.dp))
        }

        // ─── FOOTER (fixed) ───────────────────────────────────────────
        ReceiptDoubleDivider()
        Spacer(Modifier.height(4.dp))

        // Total — two lines to prevent overflow/overlap
        ReceiptText("TOTAL  (${receipt.items.size} item)", FontWeight.SemiBold, 12)
        Text(
            text = "Rp ${fmt(receipt.total)}",
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            color = Color.Black,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
        )

        Spacer(Modifier.height(4.dp))
        ReceiptDivider()
        Spacer(Modifier.height(8.dp))

        if (design.footerText.isNotBlank()) {
            ReceiptText(design.footerText, FontWeight.Normal, 10, align = TextAlign.Center)
        }
        ReceiptText("✨ Terima Kasih ✨", FontWeight.SemiBold, 11, align = TextAlign.Center)
    }
}

// ─── Helper composables ───────────────────────────────────────────────────────

@Composable
private fun ReceiptText(
    text: String,
    weight: FontWeight,
    sizeSp: Int,
    align: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        fontFamily = PoppinsFamily,
        fontWeight = weight,
        fontSize = sizeSp.sp,
        lineHeight = (sizeSp + 4).sp,
        color = Color.Black,
        textAlign = align,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ReceiptDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = Color.Black,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
private fun ReceiptDoubleDivider() {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        HorizontalDivider(thickness = 1.5.dp, color = Color.Black)
        HorizontalDivider(thickness = 1.5.dp, color = Color.Black)
    }
    Spacer(Modifier.height(4.dp))
}

private fun fmt(value: Int): String = String.format("%,d", value).replace(',', '.')
