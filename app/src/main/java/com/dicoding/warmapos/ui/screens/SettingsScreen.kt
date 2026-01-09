package com.dicoding.warmapos.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.core.content.ContextCompat
import com.dicoding.warmapos.bluetooth.BluetoothDeviceInfo
import com.dicoding.warmapos.data.model.ReceiptDesign
import com.dicoding.warmapos.ui.MainViewModel
import com.dicoding.warmapos.ui.theme.AppTheme
import com.dicoding.warmapos.ui.theme.Success

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val receiptDesign by viewModel.receiptDesign.collectAsState()
    val synonyms by viewModel.synonyms.collectAsState()
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val productCount by viewModel.productCount.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentTheme by viewModel.appTheme.collectAsState()

    var showPrinterSection by remember { mutableStateOf(false) }
    var showThemeSection by remember { mutableStateOf(false) }
    var showDesignSection by remember { mutableStateOf(false) }
    var showKeteranganSection by remember { mutableStateOf(false) }
    var showImportSection by remember { mutableStateOf(false) }
    var showSynonymSection by remember { mutableStateOf(false) }
    var showDeveloperSection by remember { mutableStateOf(false) }
    var showProductSection by remember { mutableStateOf(false) }
    var showBackupSection by remember { mutableStateOf(false) }
    var showApiSection by remember { mutableStateOf(false) }

    val keteranganOptions by viewModel.keteranganOptions.collectAsState()

    // Bluetooth permission
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            viewModel.refreshPairedDevices()
        }
    }

    fun checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                    )
                )
            } else {
                viewModel.refreshPairedDevices()
            }
        } else {
            viewModel.refreshPairedDevices()
        }
    }

    LaunchedEffect(Unit) {
        checkAndRequestPermission()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Pengaturan",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "$productCount produk dimuat",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Theme Section (prominent)
        item {
            ModernSettingsSection(
                icon = Icons.Default.Palette,
                title = "Tema Warna",
                subtitle = currentTheme.displayName,
                expanded = showThemeSection,
                onToggle = { showThemeSection = !showThemeSection },
                accentColor = currentTheme.primary
            ) {
                ThemeSelector(
                    currentTheme = currentTheme,
                    onThemeSelected = { viewModel.setAppTheme(it) }
                )
            }
        }

        // Printer Section
        item {
            ModernSettingsSection(
                icon = Icons.Default.Print,
                title = "Printer Bluetooth",
                subtitle = if (viewModel.printerManager.isConnected) "Terhubung" else "Tidak terhubung",
                expanded = showPrinterSection,
                onToggle = { showPrinterSection = !showPrinterSection },
                accentColor = if (viewModel.printerManager.isConnected) Success else MaterialTheme.colorScheme.error
            ) {
                PrinterSettings(
                    viewModel = viewModel,
                    pairedDevices = pairedDevices,
                    isLoading = isLoading,
                    onRefresh = { checkAndRequestPermission() }
                )
            }
        }

        // Receipt Design Section
        item {
            ModernSettingsSection(
                icon = Icons.Default.Receipt,
                title = "Desain Struk",
                subtitle = receiptDesign.storeName,
                expanded = showDesignSection,
                onToggle = { showDesignSection = !showDesignSection }
            ) {
                ReceiptDesignSettings(
                    design = receiptDesign,
                    onUpdate = { viewModel.updateReceiptDesign(it) }
                )
            }
        }

        // Keterangan Options Section
        item {
            ModernSettingsSection(
                icon = Icons.Default.Description,
                title = "Opsi Keterangan",
                subtitle = "${keteranganOptions.size} opsi",
                expanded = showKeteranganSection,
                onToggle = { showKeteranganSection = !showKeteranganSection }
            ) {
                KeteranganOptionsSettings(
                    options = keteranganOptions,
                    onAdd = { viewModel.addKeteranganOption(it) },
                    onRemove = { viewModel.removeKeteranganOption(it) }
                )
            }
        }

        // Import CSV Section
        item {
            ModernSettingsSection(
                icon = Icons.Default.CloudUpload,
                title = "Import Produk",
                subtitle = "$productCount produk",
                expanded = showImportSection,
                onToggle = { showImportSection = !showImportSection }
            ) {
                ImportCsvSettings(
                    viewModel = viewModel,
                    productCount = productCount,
                    isLoading = isLoading
                )
            }
        }
        
        // Product Management Section
        item {
            ModernSettingsSection(
                icon = Icons.Default.Inventory,
                title = "Kelola Produk",
                subtitle = "$productCount produk • Tambah/Edit/Hapus",
                expanded = showProductSection,
                onToggle = { showProductSection = !showProductSection }
            ) {
                ProductManagementSection(
                    viewModel = viewModel,
                    productCount = productCount
                )
            }
        }

        // Synonym Section
        item {
            ModernSettingsSection(
                icon = Icons.Default.Book,
                title = "Kamus Sinonim",
                subtitle = "${synonyms.size} sinonim",
                expanded = showSynonymSection,
                onToggle = { showSynonymSection = !showSynonymSection }
            ) {
                SynonymSettings(
                    synonyms = synonyms,
                    onAdd = { key, value -> viewModel.addSynonym(key, value) },
                    onRemove = { viewModel.removeSynonym(it) }
                )
            }
        }
        
        // OCR API Section
        item {
            ModernSettingsSection(
                icon = Icons.Default.Api,
                title = "OCR API",
                subtitle = "Konfigurasi endpoint OCR",
                expanded = showApiSection,
                onToggle = { showApiSection = !showApiSection }
            ) {
                OcrApiSection(viewModel = viewModel)
            }
        }
        
        // Backup & Restore Section
        item {
            ModernSettingsSection(
                icon = Icons.Default.Backup,
                title = "Backup & Restore",
                subtitle = "Cadangkan data produk & riwayat",
                expanded = showBackupSection,
                onToggle = { showBackupSection = !showBackupSection }
            ) {
                BackupRestoreSection(viewModel = viewModel)
            }
        }
        
        // Developer Section
        item {
            ModernSettingsSection(
                icon = Icons.Default.Code,
                title = "Developer",
                subtitle = "NOVIL M",
                expanded = showDeveloperSection,
                onToggle = { showDeveloperSection = !showDeveloperSection },
                accentColor = MaterialTheme.colorScheme.tertiary
            ) {
                DeveloperSection()
            }
        }
    }
}

@Composable
fun ThemeSelector(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Pilih tema warna untuk aplikasi:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(AppTheme.entries.toList()) { theme ->
                ThemeCard(
                    theme = theme,
                    isSelected = theme == currentTheme,
                    onClick = { onThemeSelected(theme) }
                )
            }
        }
    }
}

@Composable
fun ThemeCard(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.border(3.dp, theme.primary, RoundedCornerShape(16.dp))
                else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) theme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Color preview circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(theme.primary),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = theme.emoji,
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = theme.displayName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun ModernSettingsSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // Header (clickable)
            Surface(
                onClick = onToggle,
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Content with animation
            if (expanded) {
                HorizontalDivider()
                Box(modifier = Modifier.padding(16.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun PrinterSettings(
    viewModel: MainViewModel,
    pairedDevices: List<BluetoothDeviceInfo>,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    val isConnected = viewModel.printerManager.isConnected
    val connectedName = viewModel.printerManager.connectedDeviceName
    val savedPrinterName = viewModel.printerManager.savedPrinterName
    val savedPrinterAddress = viewModel.printerManager.savedPrinterAddress

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Saved printer info
        if (savedPrinterName != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Printer Default", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(savedPrinterName, fontWeight = FontWeight.Medium)
                    }
                    TextButton(onClick = { viewModel.printerManager.clearSavedPrinter() }) {
                        Text("Hapus", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        
        // Status card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isConnected) Success.copy(alpha = 0.1f) 
                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.BluetoothDisabled,
                    contentDescription = null,
                    tint = if (isConnected) Success else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (isConnected) "Terhubung" else "Tidak terhubung", fontWeight = FontWeight.Medium)
                    if (isConnected && connectedName != null) {
                        Text(connectedName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (isConnected) {
                    TextButton(onClick = { viewModel.disconnectPrinter() }) { Text("Putuskan") }
                }
            }
        }

        // Actions
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onRefresh, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Refresh")
            }
            Button(
                onClick = { viewModel.testPrint() },
                enabled = (isConnected || savedPrinterAddress != null) && !isLoading,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.Print, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Test")
            }
        }

        // Paired devices
        if (pairedDevices.isNotEmpty()) {
            Text("Perangkat:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            pairedDevices.forEach { device ->
                val isSaved = device.isSaved
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSaved) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                        else MaterialTheme.colorScheme.surfaceVariant
                    ), 
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(device.name, fontWeight = FontWeight.Medium)
                                if (isSaved) {
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Default.Star, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Text(device.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = { viewModel.connectAndSavePrinter(device.address, device.name) }, 
                            enabled = !isLoading, 
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (isSaved) "Hubung" else "Simpan")
                        }
                    }
                }
            }
        } else {
            Text("Tidak ada printer Bluetooth dipasangkan.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Pasangkan printer via Pengaturan Bluetooth perangkat.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun ReceiptDesignSettings(design: ReceiptDesign, onUpdate: (ReceiptDesign) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(value = design.storeName, onValueChange = { onUpdate(design.copy(storeName = it)) }, label = { Text("Nama Toko") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
        OutlinedTextField(value = design.storeAddress, onValueChange = { onUpdate(design.copy(storeAddress = it)) }, label = { Text("Alamat") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
        OutlinedTextField(value = design.storePhone, onValueChange = { onUpdate(design.copy(storePhone = it)) }, label = { Text("Telepon") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
        OutlinedTextField(value = design.footerText, onValueChange = { onUpdate(design.copy(footerText = it)) }, label = { Text("Footer") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
        
        // Toggle cards
        ToggleCard(title = "Tanggal/Waktu", checked = design.showDateTime, onCheckedChange = { onUpdate(design.copy(showDateTime = it)) })
        ToggleCard(title = "Nama Kasir", checked = design.showKasir, onCheckedChange = { onUpdate(design.copy(showKasir = it)) })
    }
}

@Composable
fun ToggleCard(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), shape = RoundedCornerShape(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun ImportCsvSettings(viewModel: MainViewModel, productCount: Int, isLoading: Boolean) {
    val context = LocalContext.current
    var csvContent by remember { mutableStateOf<String?>(null) }
    var replaceMode by remember { mutableStateOf(false) }
    var fileName by remember { mutableStateOf("") }

    val csvPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                csvContent = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                fileName = uri.lastPathSegment ?: "file.csv"
            } catch (e: Exception) { csvContent = null }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Inventory, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("$productCount produk tersedia", fontWeight = FontWeight.Medium)
            }
        }

        OutlinedButton(onClick = { csvPicker.launch("text/*") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
            Icon(Icons.Default.FileOpen, null)
            Spacer(Modifier.width(8.dp))
            Text(if (csvContent != null) fileName else "Pilih File CSV")
        }

        if (csvContent != null) {
            ToggleCard(title = "Hapus data lama?", checked = replaceMode, onCheckedChange = { replaceMode = it })
            Text(
                text = if (replaceMode) "⚠️ Semua produk lama dihapus" else "✓ Produk baru ditambahkan",
                style = MaterialTheme.typography.bodySmall,
                color = if (replaceMode) MaterialTheme.colorScheme.error else Success
            )
            Button(
                onClick = { csvContent?.let { viewModel.importCsv(it, replaceMode) }; csvContent = null },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.Upload, null)
                Spacer(Modifier.width(8.dp))
                Text("Import")
            }
        }
    }
}

@Composable
fun SynonymSettings(synonyms: Map<String, String>, onAdd: (String, String) -> Unit, onRemove: (String) -> Unit) {
    var newKey by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = newKey, onValueChange = { newKey = it }, label = { Text("Singkatan") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(8.dp))
            OutlinedTextField(value = newValue, onValueChange = { newValue = it }, label = { Text("Produk") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(8.dp))
        }
        Button(
            onClick = { if (newKey.isNotBlank() && newValue.isNotBlank()) { onAdd(newKey, newValue); newKey = ""; newValue = "" } },
            modifier = Modifier.fillMaxWidth(),
            enabled = newKey.isNotBlank() && newValue.isNotBlank(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Tambah")
        }

        if (synonyms.isEmpty()) {
            Text("Belum ada sinonim", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            synonyms.forEach { (key, value) ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), shape = RoundedCornerShape(8.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(key, fontWeight = FontWeight.Medium)
                            Text("→ $value", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { onRemove(key) }) {
                            Icon(Icons.Default.Delete, "Hapus", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeteranganOptionsSettings(
    options: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var newOption by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = newOption,
                onValueChange = { newOption = it },
                label = { Text("Opsi Baru") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
            Button(
                onClick = {
                    if (newOption.isNotBlank()) {
                        onAdd(newOption.trim())
                        newOption = ""
                    }
                },
                modifier = Modifier.height(56.dp),
                enabled = newOption.isNotBlank(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, null)
            }
        }

        Text(
            "Opsi ini akan muncul di dropdown \"Keterangan\" pada halaman Cart.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (options.isEmpty()) {
            Text("Belum ada opsi keterangan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            options.forEach { option ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Label, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(option, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        if (option != "Asli") {
                            IconButton(onClick = { onRemove(option) }) {
                                Icon(Icons.Default.Delete, "Hapus", tint = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            // Show lock icon for default option
                            Icon(Icons.Default.Lock, "Default", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(24.dp).padding(end = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeveloperSection() {
    val context = LocalContext.current
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Developer name
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = com.dicoding.warmapos.R.drawable.novil),
                    contentDescription = "Developer Photo",
                    modifier = Modifier.size(48.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("NOVIL M", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Android Developer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        
        // Email link
        ContactCard(
            icon = Icons.Default.Email,
            label = "Email",
            value = "mohnovilm@gmail.com",
            onClick = {
                val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                    data = android.net.Uri.parse("mailto:mohnovilm@gmail.com")
                }
                context.startActivity(intent)
            }
        )
        
        // Instagram link
        ContactCard(
            icon = Icons.Default.Person,
            label = "Instagram",
            value = "@me_ezpzy",
            onClick = {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse("https://instagram.com/me_ezpzy")
                }
                context.startActivity(intent)
            }
        )
        
        // App info
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("WARMAPOS", fontWeight = FontWeight.Medium)
                    Text("POS Application with OCR", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun ContactCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, fontWeight = FontWeight.Medium)
            }
            Icon(Icons.Default.OpenInNew, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun ProductManagementSection(
    viewModel: MainViewModel,
    productCount: Int
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<com.dicoding.warmapos.data.model.Product?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Filter products by search - use derivedStateOf for proper reactivity
    val filteredProducts by remember {
        derivedStateOf {
            if (searchQuery.isBlank()) products.take(20)
            else products.filter { it.name.contains(searchQuery, ignoreCase = true) }.take(20)
        }
    }
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Action buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Tambah")
            }
            
            OutlinedButton(
                onClick = {
                    val file = viewModel.exportProducts()
                    if (file != null) {
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            file
                        )
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "Share CSV"))
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Export")
            }
        }
        
        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cari produk...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )
        
        // Product list
        if (filteredProducts.isEmpty()) {
            Text(
                if (searchQuery.isBlank()) "Belum ada produk" else "Tidak ditemukan",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            filteredProducts.forEach { product ->
                key(product.name) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(product.name, fontWeight = FontWeight.Medium, maxLines = 1)
                                Text(
                                    "${product.formattedPrice()} • ${product.category}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { 
                                editingProduct = product
                                showEditDialog = true 
                            }) {
                                Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { viewModel.deleteProduct(product.name) }) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            
            if (products.size > 20 && searchQuery.isBlank()) {
                Text(
                    "Menampilkan 20 dari $productCount produk. Gunakan pencarian untuk menemukan produk lain.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    // Add Product Dialog
    if (showAddDialog) {
        ProductFormDialog(
            title = "Tambah Produk",
            onDismiss = { showAddDialog = false },
            onSave = { name, sku, category, price, unit ->
                viewModel.addProduct(name, sku, category, price, unit)
                showAddDialog = false
            }
        )
    }
    
    // Edit Product Dialog
    if (showEditDialog && editingProduct != null) {
        ProductFormDialog(
            title = "Edit Produk",
            initialName = editingProduct!!.name,
            initialSku = editingProduct!!.sku,
            initialCategory = editingProduct!!.category,
            initialPrice = editingProduct!!.price.toString(),
            initialUnit = editingProduct!!.unit,
            onDismiss = { 
                showEditDialog = false
                editingProduct = null
            },
            onSave = { name, sku, category, price, unit ->
                viewModel.updateProduct(editingProduct!!.name, name, sku, category, price, unit)
                showEditDialog = false
                editingProduct = null
            }
        )
    }
}

@Composable
fun ProductFormDialog(
    title: String,
    initialName: String = "",
    initialSku: String = "",
    initialCategory: String = "",
    initialPrice: String = "",
    initialUnit: String = "pcs",
    onDismiss: () -> Unit,
    onSave: (name: String, sku: String, category: String, price: Int, unit: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var sku by remember { mutableStateOf(initialSku) }
    var category by remember { mutableStateOf(initialCategory) }
    var price by remember { mutableStateOf(initialPrice) }
    var unit by remember { mutableStateOf(initialUnit) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Produk *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("SKU/Kode") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Kategori") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter { c -> c.isDigit() } },
                    label = { Text("Harga *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    prefix = { Text("Rp") }
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Satuan") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && price.isNotBlank()) {
                        onSave(name, sku, category, price.toIntOrNull() ?: 0, unit.ifBlank { "pcs" })
                    }
                },
                enabled = name.isNotBlank() && price.isNotBlank()
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun BackupRestoreSection(viewModel: MainViewModel) {
    val context = LocalContext.current
    
    // File picker for restore
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.restoreBackup(it) }
    }
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Info card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Backup mencakup: Produk, Riwayat Struk, Sinonim, dan Pengaturan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        // Backup button
        Button(
            onClick = {
                val file = viewModel.createBackup()
                if (file != null) {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        file
                    )
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "Share Backup"))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Backup, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Buat Backup & Share")
        }
        
        // Restore button
        OutlinedButton(
            onClick = { restoreLauncher.launch("application/zip") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Restore, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Restore dari File")
        }
        
        // Backup location info
        Text(
            "📁 Lokasi backup: Documents/WARMAPOS_Backup/",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun OcrApiSection(viewModel: MainViewModel) {
    val currentKey by viewModel.ocrApiKey.collectAsState()
    val currentMode by viewModel.ocrMode.collectAsState()
    var editKey by remember(currentKey) { mutableStateOf(currentKey) }
    val isDefault = currentKey == com.dicoding.warmapos.data.repository.SettingsRepository.DEFAULT_OCR_API_KEY
    val isOffline = currentMode == com.dicoding.warmapos.data.repository.SettingsRepository.OCR_MODE_OFFLINE
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // OCR Mode selector
        Text("Mode OCR", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Online mode button
            Card(
                modifier = Modifier.weight(1f).clickable {
                    viewModel.setOcrMode(com.dicoding.warmapos.data.repository.SettingsRepository.OCR_MODE_ONLINE)
                },
                colors = CardDefaults.cardColors(
                    containerColor = if (!isOffline) MaterialTheme.colorScheme.primaryContainer 
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Cloud, null,
                        tint = if (!isOffline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Online", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text("OCR.space API", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            // Offline mode button
            Card(
                modifier = Modifier.weight(1f).clickable {
                    viewModel.setOcrMode(com.dicoding.warmapos.data.repository.SettingsRepository.OCR_MODE_OFFLINE)
                },
                colors = CardDefaults.cardColors(
                    containerColor = if (isOffline) MaterialTheme.colorScheme.primaryContainer 
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.PhoneAndroid, null,
                        tint = if (isOffline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Offline", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text("ML Kit (Gratis)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        
        // Mode info
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isOffline) Success.copy(alpha = 0.1f) 
                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isOffline) Icons.Default.OfflineBolt else Icons.Default.Language, 
                    null, 
                    tint = if (isOffline) Success else MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isOffline) "Mode Offline: Tidak perlu internet. Menggunakan ML Kit di perangkat."
                    else "Mode Online: Membutuhkan internet. Hasil lebih akurat dengan OCR.space API.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        // Only show API Key section when in Online mode
        if (!isOffline) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            
            Text("API Key (OCR.space)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            // API Key input
            OutlinedTextField(
                value = editKey,
                onValueChange = { editKey = it },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                trailingIcon = {
                    if (isDefault) {
                        Icon(Icons.Default.CheckCircle, null, tint = Success)
                    }
                }
            )
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { 
                        viewModel.resetOcrApiKey()
                        editKey = com.dicoding.warmapos.data.repository.SettingsRepository.DEFAULT_OCR_API_KEY
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isDefault
                ) {
                    Icon(Icons.Default.RestartAlt, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Default")
                }
                
                Button(
                    onClick = { viewModel.setOcrApiKey(editKey) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    enabled = editKey.isNotBlank() && editKey != currentKey
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Simpan")
                }
            }
        }
    }
}
