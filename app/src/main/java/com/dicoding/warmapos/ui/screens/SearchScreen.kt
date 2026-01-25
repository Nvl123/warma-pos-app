package com.dicoding.warmapos.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dicoding.warmapos.data.model.CartItem
import com.dicoding.warmapos.data.model.Product
import com.dicoding.warmapos.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onNavigateToCart: (() -> Unit)? = null
) {
    // Menggunakan simple search results (tanpa score)
    val searchResults by viewModel.simpleSearchResults.collectAsState()
    val searchSuggestions by viewModel.searchSuggestions.collectAsState()
    val productCount by viewModel.productCount.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val cartTotal = viewModel.cartTotal

    var searchQuery by remember { mutableStateOf("") }
    
    // Use Box to layer the cart preview bar on top
    Box(modifier = modifier.fillMaxSize()) {
        // Main content
        if (searchResults.isNotEmpty()) {
            // When results exist, use LazyColumn for collapsing header effect
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                    .padding(bottom = if (cartItems.isNotEmpty()) 80.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            // Header card - scrolls with content
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "🔍 Cari Produk",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "$productCount produk tersedia • ${cartItems.size} di keranjang",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            // Search field with suggestions - scrolls with content
            item {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { query ->
                            searchQuery = query
                            viewModel.simpleSearchProducts(query)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Ketik nama produk...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    viewModel.clearSimpleSearch()
                                }) {
                                    Icon(Icons.Default.Close, "Hapus", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLeadingIconColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    
                    // Keyword Suggestions (like Google)
                    AnimatedVisibility(
                        visible = searchSuggestions.isNotEmpty(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    "Saran Pencarian:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                                )
                                searchSuggestions.forEach { suggestion ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                searchQuery = suggestion
                                                viewModel.applySuggestion(suggestion)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Outlined.TrendingUp,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = suggestion,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Results counter
            item {
                Text(
                    "${searchResults.size} hasil ditemukan",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Product items (tanpa score)
            items(searchResults) { product ->
                SimpleProductCard(
                    product = product,
                    onAdd = { qty -> viewModel.addToCart(product, qty) }
                )
            }
        }
        } else {
            // No results - use fixed Column layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                    .padding(bottom = if (cartItems.isNotEmpty()) 80.dp else 0.dp)
            ) {
                // Header card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "🔍 Cari Produk",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "$productCount produk tersedia • ${cartItems.size} di keranjang",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search field with suggestions
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { query ->
                            searchQuery = query
                            viewModel.simpleSearchProducts(query)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Ketik nama produk...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    viewModel.clearSimpleSearch()
                                }) {
                                    Icon(Icons.Default.Close, "Hapus", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLeadingIconColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    
                    // Keyword Suggestions (like Google)
                    AnimatedVisibility(
                        visible = searchSuggestions.isNotEmpty(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    "Saran Pencarian:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                                )
                                searchSuggestions.forEach { suggestion ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                searchQuery = suggestion
                                                viewModel.applySuggestion(suggestion)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Outlined.TrendingUp,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = suggestion,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // No results state
                if (searchQuery.isNotEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("😕", style = MaterialTheme.typography.displaySmall)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Tidak ada produk ditemukan",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    "Coba kata kunci lain",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                } else {
                    // Empty state (no search yet)
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Inventory2, null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Cari dari $productCount Produk",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Ketik nama produk untuk mencari",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
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

/**
 * Simple Product Card - tanpa score/persentase match
 * UI lebih bersih untuk pencarian biasa
 */
@Composable
fun SimpleProductCard(
    product: Product,
    onAdd: (Int) -> Unit
) {
    var quantity by remember { mutableStateOf(1) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = product.formattedPrice(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "/ ${product.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (product.category.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = product.category,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Quantity selector and add button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = { if (quantity > 1) quantity-- },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("-", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "$quantity",
                        modifier = Modifier.width(24.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    FilledTonalIconButton(
                        onClick = { quantity++ },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("+", fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                FilledIconButton(
                    onClick = { 
                        onAdd(quantity)
                        quantity = 1
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah")
                }
            }
        }
    }
}

@Composable
fun EnhancedProductCard(
    product: Product,
    score: Float,
    onAdd: (Int) -> Unit
) {
    var quantity by remember { mutableStateOf(1) }
    val scorePercent = (score * 100).toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Score badge
            Surface(
                color = when {
                    score >= 0.8f -> MaterialTheme.colorScheme.primary
                    score >= 0.6f -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.tertiary
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "$scorePercent%",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Product info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = product.formattedPrice(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "/ ${product.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (product.category.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = product.category,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Quantity selector and add button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = { if (quantity > 1) quantity-- },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("-", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "$quantity",
                        modifier = Modifier.width(24.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    FilledTonalIconButton(
                        onClick = { quantity++ },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("+", fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                FilledIconButton(
                    onClick = { 
                        onAdd(quantity)
                        quantity = 1
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah")
                }
            }
        }
    }
}

/**
 * Expandable floating cart preview bar that shows when cart is not empty
 * - Collapsed: Shows summary with item count and total
 * - Expanded: Shows all cart items with quantity controls
 */
@Composable
fun CartPreviewBar(
    cartItems: List<CartItem>,
    cartTotal: Int,
    onNavigateToCart: (() -> Unit)?,
    onUpdateQuantity: ((String, Int) -> Unit)? = null,
    onRemoveItem: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "arrow_rotation"
    )
    
    AnimatedVisibility(
        visible = cartItems.isNotEmpty(),
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column {
                // Header row (always visible) - clickable to expand/collapse
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cart icon with badge
                    BadgedBox(
                        badge = {
                            Badge(containerColor = MaterialTheme.colorScheme.error) {
                                Text("${cartItems.size}", fontSize = 10.sp)
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Cart info
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${cartItems.size} Item di Keranjang",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        // Show last 2 items when collapsed
                        if (!isExpanded) {
                            cartItems.takeLast(2).forEach { item ->
                                Text(
                                    text = "• ${item.product.name} (x${item.quantity})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    
                    // Total price and expand button
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatCartTotal(cartTotal),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        // Arrow indicator (rotates when expanded)
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = if (isExpanded) "Tutup" else "Lihat Semua",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = rotationAngle }
                        )
                    }
                }
                
                // Expanded content - scrollable list of all items
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        )
                        
                        // Scrollable list of items
                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            cartItems.forEach { item ->
                                CartPreviewItem(
                                    item = item,
                                    onUpdateQuantity = { newQty -> 
                                        onUpdateQuantity?.invoke(item.id, newQty) 
                                    },
                                    onRemove = { 
                                        onRemoveItem?.invoke(item.id)
                                    }
                                )
                            }
                        }
                        
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        )
                        
                        // Bottom action row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Total display
                            Column {
                                Text(
                                    "Total",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    formatCartTotal(cartTotal),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            // Go to Cart button
                            Button(
                                onClick = { onNavigateToCart?.invoke() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Ke Keranjang")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single cart item in the expanded preview
 */
@Composable
private fun CartPreviewItem(
    item: CartItem,
    onUpdateQuantity: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Product info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.product.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "${item.product.formattedPrice()} / ${item.product.unit}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
        
        // Quantity controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Decrease button
            IconButton(
                onClick = { 
                    if (item.quantity > 1) onUpdateQuantity(item.quantity - 1)
                    else onRemove()
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    if (item.quantity == 1) Icons.Default.Delete else Icons.Default.Remove,
                    contentDescription = if (item.quantity == 1) "Hapus" else "Kurangi",
                    modifier = Modifier.size(16.dp),
                    tint = if (item.quantity == 1) MaterialTheme.colorScheme.error 
                           else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            // Quantity badge
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "${item.quantity}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Increase button
            IconButton(
                onClick = { onUpdateQuantity(item.quantity + 1) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Tambah",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        // Subtotal
        Text(
            text = item.formattedSubtotal(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(80.dp),
            textAlign = TextAlign.End
        )
    }
}

private fun formatCartTotal(amount: Int): String {
    return "Rp ${String.format("%,d", amount).replace(',', '.')}"
}

