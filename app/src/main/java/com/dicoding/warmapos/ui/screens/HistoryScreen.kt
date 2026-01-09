package com.dicoding.warmapos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dicoding.warmapos.data.repository.ReceiptHistoryItem
import com.dicoding.warmapos.ui.MainViewModel
import androidx.compose.foundation.background
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onNavigateToCart: (() -> Unit)? = null
) {
    val receiptHistory by viewModel.receiptHistory.collectAsState()
    val groupedReceipts by viewModel.groupedReceipts.collectAsState()
    val isSelectionMode by viewModel.isGroupSelectionMode.collectAsState()
    val selectedPaths by viewModel.selectedReceiptPaths.collectAsState()
    val editingGroupTarget by viewModel.editingGroupTarget.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) } // 0: Transaksi, 1: Kelompok Struk
    var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }
    var customDate by remember { mutableStateOf<String?>(null) } // Using String "yyyy-MM-dd" for API 24 compat
    var showDatePicker by remember { mutableStateOf(false) }
    var showGroupDetail by remember { mutableStateOf<com.dicoding.warmapos.data.model.GroupedReceipt?>(null) }
    var sortAscending by remember { mutableStateOf(false) } // false = newest first (desc)
    
    // Group creation dialog
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        viewModel.loadGroupedReceipts()
    }
    
    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        customDate = sdf.format(java.util.Date(millis))
                        selectedFilter = HistoryFilter.CUSTOM
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    // Create Group Dialog
    if (showCreateGroupDialog) {
        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = { Text("Buat Kelompok Struk") },
            text = {
                Column {
                    Text("Total: ${formatCurrency(receiptHistory.filter { selectedPaths.contains(it.filePath) }.sumOf { it.totalAmount })}")
                    Text("Jumlah: ${selectedPaths.size} struk")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        label = { Text("Nama Kelompok (Opsional)") },
                        placeholder = { Text("Kelompok ${selectedPaths.size} Struk") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.createGroupedReceipt(newGroupName)
                    showCreateGroupDialog = false
                    newGroupName = ""
                    selectedTab = 1 // Switch to groups tab
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGroupDialog = false }) { Text("Batal") }
            }
        )
    }

    // Edit Group Dialog
    if (showGroupDetail != null) {
        val editingGroup = showGroupDetail!!
        val groupReceipts = remember(editingGroup) {
            receiptHistory.filter { editingGroup.receiptPaths.contains(it.filePath) }
        }
        
        AlertDialog(
            onDismissRequest = { showGroupDetail = null },
            title = { 
                Text("Edit Kelompok: ${editingGroup.name}") 
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    Text(
                        "Total: ${formatCurrency(editingGroup.totalAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${editingGroup.receiptCount} struk dalam kelompok",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(Modifier.height(12.dp))
                    
                    Text(
                        "Daftar Struk:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // Scrollable list of receipts in group
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(groupReceipts) { receipt ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${receipt.date} ${receipt.time}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = receipt.formattedTotal(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.removeReceiptFromGroup(editingGroup, receipt.filePath)
                                            // Refresh the group
                                            val updatedGroup = groupedReceipts.find { it.id == editingGroup.id }
                                            if (updatedGroup == null || updatedGroup.receiptCount == 0) {
                                                showGroupDetail = null
                                            } else {
                                                showGroupDetail = updatedGroup
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Outlined.DeleteForever,
                                            contentDescription = "Hapus dari kelompok",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    // Add more receipts button
                    OutlinedButton(
                        onClick = {
                            showGroupDetail = null
                            selectedTab = 0 // Switch to transactions
                            viewModel.startAddingToGroup(editingGroup)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Tambah Struk ke Kelompok")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showGroupDetail = null }) {
                    Text("Tutup")
                }
            },
            dismissButton = {}
        )
    }

    // Filter logic for receipts
    // Date filtering using Calendar for API 24 compatibility
    val filteredHistory = remember(receiptHistory, selectedFilter, customDate, sortAscending) {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val todayString = sdf.format(java.util.Date())
        val todayCal = java.util.Calendar.getInstance()
        
        val filtered = receiptHistory.filter { item ->
            when (selectedFilter) {
                HistoryFilter.ALL -> true
                HistoryFilter.TODAY -> item.date == todayString
                HistoryFilter.THIS_WEEK -> {
                    try {
                        val itemCal = java.util.Calendar.getInstance()
                        itemCal.time = sdf.parse(item.date) ?: return@filter true
                        val weekStart = java.util.Calendar.getInstance()
                        weekStart.set(java.util.Calendar.DAY_OF_WEEK, weekStart.firstDayOfWeek)
                        itemCal >= weekStart
                    } catch (e: Exception) { true }
                }
                HistoryFilter.THIS_MONTH -> {
                    try {
                        val itemCal = java.util.Calendar.getInstance()
                        itemCal.time = sdf.parse(item.date) ?: return@filter true
                        itemCal.get(java.util.Calendar.YEAR) == todayCal.get(java.util.Calendar.YEAR) &&
                            itemCal.get(java.util.Calendar.MONTH) == todayCal.get(java.util.Calendar.MONTH)
                    } catch (e: Exception) { true }
                }
                HistoryFilter.CUSTOM -> {
                    customDate?.let { item.date == it } ?: true
                }
            }
        }
        if (sortAscending) filtered else filtered.reversed()
    }

    // Filter logic for grouped receipts (same filtering)
    val filteredGroupedReceipts = remember(groupedReceipts, selectedFilter, customDate, sortAscending) {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val todayString = sdf.format(java.util.Date())
        val todayCal = java.util.Calendar.getInstance()
        
        val filtered = groupedReceipts.filter { group ->
            val groupDateString = sdf.format(java.util.Date(group.timestamp))
            when (selectedFilter) {
                HistoryFilter.ALL -> true
                HistoryFilter.TODAY -> groupDateString == todayString
                HistoryFilter.THIS_WEEK -> {
                    try {
                        val itemCal = java.util.Calendar.getInstance()
                        itemCal.time = java.util.Date(group.timestamp)
                        val weekStart = java.util.Calendar.getInstance()
                        weekStart.set(java.util.Calendar.DAY_OF_WEEK, weekStart.firstDayOfWeek)
                        itemCal >= weekStart
                    } catch (e: Exception) { true }
                }
                HistoryFilter.THIS_MONTH -> {
                    try {
                        val itemCal = java.util.Calendar.getInstance()
                        itemCal.time = java.util.Date(group.timestamp)
                        itemCal.get(java.util.Calendar.YEAR) == todayCal.get(java.util.Calendar.YEAR) &&
                            itemCal.get(java.util.Calendar.MONTH) == todayCal.get(java.util.Calendar.MONTH)
                    } catch (e: Exception) { true }
                }
                HistoryFilter.CUSTOM -> {
                    customDate?.let { groupDateString == it } ?: true
                }
            }
        }
        val sorted = if (sortAscending) filtered.sortedBy { it.timestamp } else filtered.sortedByDescending { it.timestamp }
        sorted
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        floatingActionButton = {
            if (isSelectionMode && selectedPaths.isNotEmpty()) {
                if (editingGroupTarget != null) {
                    // Adding to existing group
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.addReceiptsToExistingGroup() },
                        icon = { Icon(Icons.Default.Add, null) },
                        text = { Text("Tambah ke ${editingGroupTarget!!.name}") },
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                } else {
                    // Creating new group
                    ExtendedFloatingActionButton(
                        onClick = { showCreateGroupDialog = true },
                        icon = { Icon(Icons.Default.Save, null) },
                        text = { Text("Buat Kelompok (${selectedPaths.size})") },
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (isSelectionMode && editingGroupTarget != null) {
                // Cancle button when in add-to-group mode with no selection
                ExtendedFloatingActionButton(
                    onClick = { viewModel.cancelAddingToGroup() },
                    icon = { Icon(Icons.Default.Close, null) },
                    text = { Text("Batal") },
                    containerColor = MaterialTheme.colorScheme.error
                )
            } else if (!isSelectionMode && selectedTab == 0) {
                 FloatingActionButton(
                    onClick = { viewModel.toggleGroupSelectionMode() },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.Checklist, "Mode Pilih")
                }
            }
        }
    ) { paddingValues ->
        // Single scrollable list for everything
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Riwayat Penjualan",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Kelola riwayat transaksi dan struk",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            
            // Tabs
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { 
                            selectedTab = 0 
                            if (isSelectionMode) viewModel.toggleGroupSelectionMode()
                        },
                        text = { Text("Transaksi") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { 
                            selectedTab = 1
                            if (isSelectionMode) viewModel.toggleGroupSelectionMode() 
                        },
                        text = { Text("Kelompok Struk") }
                    )
                }
            }
            
            // Filters (for both tabs)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sort toggle button
                    IconButton(
                        onClick = { sortAscending = !sortAscending }
                    ) {
                        Icon(
                            imageVector = if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = if (sortAscending) "Terlama" else "Terbaru",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = if (sortAscending) "Terlama" else "Terbaru",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Filter chips
                    ScrollableTabRow(
                        selectedTabIndex = selectedFilter.ordinal,
                        edgePadding = 0.dp,
                        containerColor = Color.Transparent,
                        divider = {},
                        indicator = {},
                        modifier = Modifier.weight(1f)
                    ) {
                        HistoryFilter.values().forEach { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { 
                                    if (filter == HistoryFilter.CUSTOM) showDatePicker = true
                                    selectedFilter = filter 
                                },
                                label = { 
                                    Text(
                                        if (filter == HistoryFilter.CUSTOM && customDate != null) 
                                            customDate.toString() 
                                        else filter.label,
                                        fontSize = 12.sp
                                    ) 
                                },
                                leadingIcon = if (filter == HistoryFilter.CUSTOM) {
                                    { Icon(Icons.Default.DateRange, null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                modifier = Modifier.padding(end = 4.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            if (selectedTab == 0) {

                if (isSelectionMode) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Checklist, null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Pilih struk untuk dikelompokkan",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { viewModel.toggleGroupSelectionMode() }) {
                                    Icon(Icons.Default.Close, "Batal")
                                }
                            }
                        }
                    }
                }
                
                items(filteredHistory) { item ->
                    val isSelected = selectedPaths.contains(item.filePath)
                    
                    HistoryItemCard(
                        item = item,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onToggleSelection = { viewModel.toggleReceiptSelection(item.filePath) },
                        onItemClick = { if (!isSelectionMode) { /* Open detail */ } },
                        onDelete = { viewModel.deleteReceipt(item) },
                        onPrint = { viewModel.reprintReceipt(item) },
                        onReuse = {
                            viewModel.reuseReceipt(item)
                            onNavigateToCart?.invoke()
                        }
                    )
                }
            } else {
                // Grouped Receipts List
                if (groupedReceipts.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "Belum ada kelompok struk.\nBuat dari tab Transaksi dengan mode pilih.",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(filteredGroupedReceipts) { group ->
                        GroupHistoryItemCard(
                            group = group,
                            onItemClick = { /* Show details */ },
                            onDelete = { viewModel.deleteGroupedReceipt(group) },
                            onPrint = { viewModel.printGroupedReceipt(group) },
                            onEdit = { showGroupDetail = group }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GroupHistoryItemCard(
    group: com.dicoding.warmapos.data.model.GroupedReceipt,
    onItemClick: () -> Unit,
    onDelete: () -> Unit,
    onPrint: () -> Unit,
    onEdit: () -> Unit
) {
    var showActions by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showActions = !showActions },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min)
        ) {
            // Left Sidebar (Vertical Group Name)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(40.dp)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${group.receiptCount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            // Main Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                // Top: Name and Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(group.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom: Total
                val total = group.totalAmount
                val fontSize = when {
                    total >= 100_000_000 -> 20.sp
                    else -> 22.sp
                }

                Text(
                    text = formatCurrency(total),
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = fontSize),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.End)
                )
            }

            // Action Buttons (Revealed on click)
            if (showActions) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(onClick = { onEdit(); showActions = false }) {
                        Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { onPrint(); showActions = false }) {
                        Icon(Icons.Default.Print, "Print", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { onDelete(); showActions = false }) {
                        Icon(Icons.Outlined.DeleteForever, "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryItemCard(
    item: ReceiptHistoryItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onItemClick: () -> Unit,
    onDelete: () -> Unit,
    onPrint: () -> Unit,
    onReuse: () -> Unit
) {
    var showActions by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                if (isSelectionMode) onToggleSelection() 
                else showActions = !showActions // Toggle action buttons
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) 
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min)
        ) {
            // Left Sidebar (Vertical Order ID)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelection() }
                    )
                } else {
                    Text(
                        text = "#${item.orderId.takeLast(4)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .rotate(-90f)
                            .padding(vertical = 4.dp),
                        maxLines = 1
                    )
                }
            }

            // Main Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                // Top: Date/Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.time,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom: Total
                val total = item.totalAmount
                val fontSize = when {
                    total >= 100_000_000 -> 20.sp
                    else -> 22.sp
                }

                Text(
                    text = item.formattedTotal(),
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = fontSize),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.End)
                )
            }

            // Action Buttons (Revealed on click)
            if (showActions && !isSelectionMode) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(onClick = { onReuse(); showActions = false }) {
                        Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { onPrint(); showActions = false }) {
                        Icon(Icons.Default.Print, "Print", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { onDelete(); showActions = false }) {
                        Icon(Icons.Outlined.DeleteForever, "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

enum class HistoryFilter(val label: String) {
    ALL("Semua"),
    TODAY("Hari Ini"),
    THIS_WEEK("Minggu Ini"),
    THIS_MONTH("Bulan Ini"),
    CUSTOM("Pilih Tanggal")
}

fun formatCurrency(amount: Int): String {
    return java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID")).format(amount)
}
