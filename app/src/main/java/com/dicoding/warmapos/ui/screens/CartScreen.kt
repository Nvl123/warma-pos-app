package com.dicoding.warmapos.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dicoding.warmapos.ui.MainViewModel
import com.dicoding.warmapos.ui.components.CartItemCard
import com.dicoding.warmapos.ui.components.TotalCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val cartItems by viewModel.cartItems.collectAsState()
    val kasirName by viewModel.kasirName.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val lembarKe by viewModel.lembarKe.collectAsState()
    val keterangan by viewModel.keterangan.collectAsState()
    val keteranganOptions by viewModel.keteranganOptions.collectAsState()

    var showReceiptPreview by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var keteranganExpanded by remember { mutableStateOf(false) }

    // Clear confirmation dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            icon = { Icon(Icons.Outlined.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Kosongkan Keranjang?", fontWeight = FontWeight.Bold) },
            text = { Text("Semua item akan dihapus dari keranjang") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearCart()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus Semua")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Receipt preview dialog
    if (showReceiptPreview) {
        ReceiptPreviewDialog(
            viewModel = viewModel,
            onDismiss = { showReceiptPreview = false },
            onSaveAndPrint = {
                showReceiptPreview = false
                viewModel.saveReceipt()
            }
        )
    }

    if (cartItems.isEmpty()) {
        // Empty state - no scroll needed
        Column(
            modifier = modifier.fillMaxSize().padding(16.dp)
        ) {
            // Header
            CartHeader(cartItems.size, onClear = { showClearConfirmDialog = true })
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                EmptyCartState()
            }
        }
    } else {
        // Unified scrolling LazyColumn
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Header card
            item {
                CartHeader(cartItems.size, onClear = { showClearConfirmDialog = true })
            }
            
            // Kasir name input
            item {
                OutlinedTextField(
                    value = kasirName,
                    onValueChange = { viewModel.updateKasirName(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("👤 Kasir") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            }

            // Lembar Ke and Keterangan row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Lembar Ke input
                    OutlinedTextField(
                        value = lembarKe.toString(),
                        onValueChange = { value ->
                            value.toIntOrNull()?.let { viewModel.updateLembarKe(it) }
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("📄 Lembar Ke") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    // Keterangan dropdown
                        ExposedDropdownMenuBox(
                        expanded = keteranganExpanded,
                        onExpandedChange = { keteranganExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = keterangan,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("📝 Keterangan") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = keteranganExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = keteranganExpanded,
                            onDismissRequest = { keteranganExpanded = false }
                        ) {
                            keteranganOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        viewModel.updateKeterangan(option)
                                        keteranganExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Cart items
            items(cartItems, key = { it.id }) { item ->
                CartItemCard(
                    cartItem = item,
                    onQuantityChange = { newQty -> viewModel.updateCartQuantity(item.id, newQty) },
                    onRemove = { viewModel.removeFromCart(item.id) }
                )
            }

            // Total card
            item {
                TotalCard(
                    total = viewModel.cartTotal,
                    itemCount = cartItems.size
                )
            }

            // Action buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showReceiptPreview = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Preview", fontSize = 14.sp)
                    }

                    Button(
                        onClick = { viewModel.saveReceipt() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simpan", fontSize = 14.sp)
                    }
                }
            }

            // Print button (compact)
            item {
                Button(
                    onClick = { viewModel.printReceipt() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = viewModel.printerManager.savedPrinterAddress != null && !isLoading,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mencetak...", fontSize = 14.sp)
                    } else {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (viewModel.printerManager.isConnected) "Cetak Struk" else "Printer ✗",
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CartHeader(itemCount: Int, onClear: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🛒 Keranjang",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "$itemCount item",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }

            if (itemCount > 0) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Kosongkan",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyCartState() {
    Card(
        modifier = Modifier.fillMaxWidth(0.85f),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Keranjang Kosong",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tambah produk via OCR atau Pencarian",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ReceiptPreviewDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSaveAndPrint: () -> Unit
) {
    val design by viewModel.receiptDesign.collectAsState()
    val kasirName by viewModel.kasirName.collectAsState()
    val lembarKe by viewModel.lembarKe.collectAsState()
    val keterangan by viewModel.keterangan.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()

    // Build a live receipt object for the preview
    val previewReceipt = remember(cartItems, kasirName, lembarKe, keterangan, design) {
        com.dicoding.warmapos.data.model.Receipt(
            kasir = kasirName,
            storeName = design.storeName,
            items = cartItems.map { com.dicoding.warmapos.data.model.ReceiptItem.fromCartItem(it) },
            total = viewModel.cartTotal,
            lembarKe = lembarKe,
            keterangan = keterangan
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "🧾 Preview Struk",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Ini adalah tampilan struk yang akan dicetak",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Visual receipt card with drop shadow effect
                Card(
                    modifier = Modifier.fillMaxWidth(0.92f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    com.dicoding.warmapos.ui.components.ReceiptTemplate(
                        receipt = previewReceipt,
                        design = design,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onSaveAndPrint, shape = RoundedCornerShape(8.dp)) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Simpan & Cetak")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

