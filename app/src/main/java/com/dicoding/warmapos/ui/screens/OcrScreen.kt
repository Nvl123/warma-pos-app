package com.dicoding.warmapos.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.dicoding.warmapos.data.model.Product
import com.dicoding.warmapos.ui.MainViewModel
import com.dicoding.warmapos.ui.OcrResultWithMatches

enum class InputMode {
    OCR, MANUAL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onNavigateToCart: (() -> Unit)? = null
) {
    val ocrResults by viewModel.ocrResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val products by viewModel.products.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val cartTotal = viewModel.cartTotal
    
    // Use ViewModel state for image URI (persisted across tab switches)
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()

    // Persistent state for input mode
    var inputMode by remember { mutableStateOf(InputMode.OCR) }
    
    // Manual input state (persisted)
    var manualItemName by remember { mutableStateOf("") }
    var manualQuantity by remember { mutableStateOf("1") }
    var searchResults by remember { mutableStateOf<List<Product>>(emptyList()) }
    
    // Camera and crop state
    var showCamera by remember { mutableStateOf(false) }
    var imageToCrop by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            // Show crop screen for gallery images too
            imageToCrop = uri
        }
    }
    
    // Full-screen crop overlay
    imageToCrop?.let { uri ->
        CropScreen(
            imageUri = uri,
            onCropComplete = { croppedUri ->
                viewModel.setSelectedImageUri(croppedUri)
                imageToCrop = null
            },
            onCancel = { imageToCrop = null }
        )
        return
    }
    
    // Full-screen camera overlay
    if (showCamera) {
        CameraScreen(
            onImageCaptured = { uri ->
                // After capture, show crop screen
                imageToCrop = uri
                showCamera = false
            },
            onClose = { showCamera = false }
        )
        return
    }

    // Main content wrapped in Box for cart preview overlay
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                .padding(bottom = if (cartItems.isNotEmpty()) 80.dp else 0.dp)
        ) {
        // Mode Toggle Tabs
        TabRow(
            selectedTabIndex = if (inputMode == InputMode.OCR) 0 else 1,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = inputMode == InputMode.OCR,
                onClick = { inputMode = InputMode.OCR },
                text = { Text("📸 OCR") },
                icon = { 
                    Icon(
                        if (inputMode == InputMode.OCR) Icons.Filled.CameraAlt else Icons.Outlined.CameraAlt,
                        contentDescription = null
                    ) 
                }
            )
            Tab(
                selected = inputMode == InputMode.MANUAL,
                onClick = { inputMode = InputMode.MANUAL },
                text = { Text("✏️ Manual") },
                icon = { 
                    Icon(
                        if (inputMode == InputMode.MANUAL) Icons.Filled.Edit else Icons.Outlined.Edit,
                        contentDescription = null
                    ) 
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (inputMode) {
            InputMode.OCR -> OcrContent(
                selectedImageUri = selectedImageUri,
                isLoading = isLoading,
                ocrResults = ocrResults,
                onPickImage = { imagePickerLauncher.launch("image/*") },
                onOpenCamera = { showCamera = true },
                onProcessOcr = { selectedImageUri?.let { viewModel.processOcr(it) } },
                onAddToCart = { product, qty -> viewModel.addToCart(product, qty) }
            )
            InputMode.MANUAL -> ManualInputContent(
                itemName = manualItemName,
                quantity = manualQuantity,
                searchResults = searchResults,
                products = products,
                onItemNameChange = { name ->
                    manualItemName = name
                    // Search products as user types
                    searchResults = if (name.length >= 2) {
                        products.filter { it.name.contains(name, ignoreCase = true) }.take(10)
                    } else {
                        emptyList()
                    }
                },
                onQuantityChange = { manualQuantity = it },
                onAddToCart = { product, qty ->
                    viewModel.addToCart(product, qty)
                    manualItemName = ""
                    manualQuantity = "1"
                    searchResults = emptyList()
                }
            )
        }
        }
        
        // Cart Preview Bar - floating at bottom
        CartPreviewBar(
            cartItems = cartItems,
            cartTotal = cartTotal,
            onNavigateToCart = onNavigateToCart,
            onUpdateQuantity = { itemId, newQty -> viewModel.updateCartQuantity(itemId, newQty) },
            onRemoveItem = { itemId -> viewModel.removeFromCart(itemId) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun OcrContent(
    selectedImageUri: Uri?,
    isLoading: Boolean,
    ocrResults: List<OcrResultWithMatches>,
    onPickImage: () -> Unit,
    onOpenCamera: () -> Unit,
    onProcessOcr: () -> Unit,
    onAddToCart: (Product, Int) -> Unit
) {
    // State for image popup
    var showImagePopup by remember { mutableStateOf(false) }
    
    // Image popup dialog
    if (showImagePopup && selectedImageUri != null) {
        AlertDialog(
            onDismissRequest = { showImagePopup = false },
            title = { 
                Text(
                    "📷 Preview Gambar",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Image(
                    painter = rememberAsyncImagePainter(selectedImageUri),
                    contentDescription = "Full image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
            },
            confirmButton = {
                Button(
                    onClick = { showImagePopup = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Tutup")
                }
            }
        )
    }
    
    // Single LazyColumn for unified scrolling
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Image preview or picker (clickable)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .then(
                        if (selectedImageUri != null) {
                            Modifier.clickable { showImagePopup = true }
                        } else {
                            Modifier
                        }
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (selectedImageUri != null) {
                    Box {
                        Image(
                            painter = rememberAsyncImagePainter(selectedImageUri),
                            contentDescription = "Selected image",
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                        // Tap to enlarge hint
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "👆 Tap untuk perbesar",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Pilih gambar untuk OCR",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Action buttons - Row 1: Image sources
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPickImage,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Galeri", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = onOpenCamera,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kamera", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Action button - Row 2: Process OCR
        item {
            Button(
                onClick = onProcessOcr,
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedImageUri != null && !isLoading,
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Proses OCR")
            }
        }

        // OCR Results header
        if (ocrResults.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "✅ Hasil OCR (${ocrResults.size} item)",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // OCR Results items
            items(ocrResults) { result ->
                OcrResultCard(
                    result = result,
                    onAddToCart = onAddToCart
                )
            }
        } else if (!isLoading && selectedImageUri != null) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Klik 'Proses OCR' untuk memulai",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualInputContent(
    itemName: String,
    quantity: String,
    searchResults: List<Product>,
    products: List<Product>,
    onItemNameChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onAddToCart: (Product, Int) -> Unit
) {
    // Manual input form
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Input Manual",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = itemName,
                onValueChange = onItemNameChange,
                label = { Text("Nama Produk") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = quantity,
                onValueChange = { if (it.all { c -> c.isDigit() }) onQuantityChange(it) },
                label = { Text("Jumlah") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Search Results
    if (searchResults.isNotEmpty()) {
        Text(
            text = "Hasil Pencarian (${searchResults.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(searchResults) { product ->
                ProductSearchCard(
                    product = product,
                    quantity = quantity.toIntOrNull() ?: 1,
                    onAddToCart = onAddToCart
                )
            }
        }
    } else if (itemName.length >= 2) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Tidak ada produk ditemukan",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Box(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Ketik nama produk untuk mencari...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProductSearchCard(
    product: Product,
    quantity: Int,
    onAddToCart: (Product, Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${product.formattedPrice()} / ${product.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            FilledIconButton(
                onClick = { onAddToCart(product, quantity) },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah")
            }
        }
    }
}

@Composable
fun OcrResultCard(
    result: OcrResultWithMatches,
    onAddToCart: (Product, Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // OCR detected text header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (result.ocrItem.wasSynonymReplaced) {
                        Text(
                            text = "🔄 ${result.ocrItem.originalSynonym} → ${result.ocrItem.processedText}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = result.ocrItem.processedText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "×${result.ocrItem.quantity}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            if (result.matches.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "📦 ${result.matches.size} kemungkinan produk:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                // Show all 5 matches
                result.matches.forEach { match ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Score badge
                            Surface(
                                color = when {
                                    match.score >= 0.8f -> MaterialTheme.colorScheme.primary
                                    match.score >= 0.6f -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.tertiary
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${match.scorePercent}%",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Product info
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = match.product.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2
                                )
                                Text(
                                    text = match.product.formattedPrice(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Add button
                            FilledIconButton(
                                onClick = { onAddToCart(match.product, result.ocrItem.quantity) },
                                modifier = Modifier.size(40.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Tambah",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "❌ Tidak ada produk yang cocok",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

