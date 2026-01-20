package com.dicoding.warmapos.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dicoding.warmapos.bluetooth.BluetoothDeviceInfo
import com.dicoding.warmapos.bluetooth.BluetoothPrinterManager
import com.dicoding.warmapos.bluetooth.ReceiptPrinter
import com.dicoding.warmapos.data.model.*
import com.dicoding.warmapos.data.repository.*
import com.dicoding.warmapos.utils.OcrHandler
import com.dicoding.warmapos.utils.MlKitOcrHandler
import com.dicoding.warmapos.utils.ProductMatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // Repositories
    private val productRepository = ProductRepository(application)
    private val synonymRepository = SynonymRepository(application)
    private val receiptRepository = ReceiptRepository(application)
    private val groupedReceiptRepository = GroupedReceiptRepository()
    private val settingsRepository = SettingsRepository(application)
    private val backupRepository = BackupRepository(application)

    // Utilities
    private val productMatcher = ProductMatcher()
    private val ocrHandler = OcrHandler(application)
    private val mlKitOcrHandler = MlKitOcrHandler(application)
    val printerManager = BluetoothPrinterManager(application)
    private val receiptPrinter = ReceiptPrinter(printerManager)

    // UI State
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _searchResults = MutableStateFlow<List<ProductMatch>>(emptyList())
    val searchResults: StateFlow<List<ProductMatch>> = _searchResults.asStateFlow()

    private val _ocrResults = MutableStateFlow<List<OcrResultWithMatches>>(emptyList())
    val ocrResults: StateFlow<List<OcrResultWithMatches>> = _ocrResults.asStateFlow()

    private val _receiptHistory = MutableStateFlow<List<ReceiptHistoryItem>>(emptyList())
    val receiptHistory: StateFlow<List<ReceiptHistoryItem>> = _receiptHistory.asStateFlow()

    private val _synonyms = MutableStateFlow<Map<String, String>>(emptyMap())
    val synonyms: StateFlow<Map<String, String>> = _synonyms.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDeviceInfo>> = _pairedDevices.asStateFlow()

    private val _receiptDesign = MutableStateFlow(ReceiptDesign())
    val receiptDesign: StateFlow<ReceiptDesign> = _receiptDesign.asStateFlow()
    
    // Group Receipt State
    private val _groupedReceipts = MutableStateFlow<List<GroupedReceipt>>(emptyList())
    val groupedReceipts: StateFlow<List<GroupedReceipt>> = _groupedReceipts.asStateFlow()
    
    private val _isGroupSelectionMode = MutableStateFlow(false)
    val isGroupSelectionMode: StateFlow<Boolean> = _isGroupSelectionMode.asStateFlow()
    
    private val _selectedReceiptPaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedReceiptPaths: StateFlow<Set<String>> = _selectedReceiptPaths.asStateFlow()

    // ... existing initializers ...

    fun loadGroupedReceipts() {
        viewModelScope.launch(Dispatchers.IO) {
            val groups = groupedReceiptRepository.getGroupedReceipts()
            _groupedReceipts.value = groups
        }
    }
    
    fun toggleGroupSelectionMode() {
        _isGroupSelectionMode.value = !_isGroupSelectionMode.value
        if (!_isGroupSelectionMode.value) {
            _selectedReceiptPaths.value = emptySet()
        }
    }
    
    fun toggleReceiptSelection(path: String) {
        val current = _selectedReceiptPaths.value.toMutableSet()
        if (current.contains(path)) {
            current.remove(path)
        } else {
            current.add(path)
        }
        _selectedReceiptPaths.value = current
    }

    fun createGroupedReceipt(name: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val selectedPaths = _selectedReceiptPaths.value
            if (selectedPaths.isEmpty()) return@launch
            
            val allReceipts = _receiptHistory.value
            val selectedItems = allReceipts.filter { selectedPaths.contains(it.filePath) }
            
            if (selectedItems.isEmpty()) return@launch
            
            val timestamp = System.currentTimeMillis()
            val totalAmount = selectedItems.sumOf { it.totalAmount }
            val count = selectedItems.size
            val defaultName = if (name.isNotBlank()) name else "Kelompok ${count} Struk"
            
            val snapshots = selectedItems.map { 
                ReceiptSnapshot(
                    id = File(it.filePath).name, // simplified ID from filename
                    timestamp = it.timestamp,
                    total = it.totalAmount
                )
            }
            
            val group = GroupedReceipt(
                id = UUID.randomUUID().toString(),
                timestamp = timestamp,
                name = defaultName,
                totalAmount = totalAmount,
                receiptCount = count,
                receiptPaths = selectedPaths.toList(),
                receipts = snapshots
            )
            
            try {
                groupedReceiptRepository.saveGroupedReceipt(group)
                withContext(Dispatchers.Main) {
                    toggleGroupSelectionMode() // Exit selection mode
                    loadGroupedReceipts() // Refresh list
                    // Optional: Show success message
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to create group", e)
            }
        }
    }
    
    fun deleteGroupedReceipt(group: GroupedReceipt) {
        viewModelScope.launch(Dispatchers.IO) {
            if (groupedReceiptRepository.deleteGroupedReceipt(group)) {
                loadGroupedReceipts()
            }
        }
    }

    // State for adding to existing group
    private val _editingGroupTarget = MutableStateFlow<GroupedReceipt?>(null)
    val editingGroupTarget: StateFlow<GroupedReceipt?> = _editingGroupTarget.asStateFlow()

    fun removeReceiptFromGroup(group: GroupedReceipt, receiptPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Remove receipt path from group
                val updatedPaths = group.receiptPaths.filterNot { it == receiptPath }
                
                if (updatedPaths.isEmpty()) {
                    // Delete the group if no more receipts
                    groupedReceiptRepository.deleteGroupedReceipt(group)
                } else {
                    // Update group with new paths
                    val receipts = updatedPaths.mapNotNull { path ->
                        receiptRepository.loadReceipt(path)
                    }
                    val newTotal = receipts.sumOf { it.total }
                    
                    val updatedGroup = group.copy(
                        receiptPaths = updatedPaths,
                        receiptCount = updatedPaths.size,
                        totalAmount = newTotal
                    )
                    groupedReceiptRepository.updateGroupedReceipt(updatedGroup)
                }
                loadGroupedReceipts()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to remove receipt from group", e)
            }
        }
    }

    fun startAddingToGroup(group: GroupedReceipt) {
        _editingGroupTarget.value = group
        _isGroupSelectionMode.value = true
        _selectedReceiptPaths.value = emptySet()
    }

    fun addReceiptsToExistingGroup() {
        val targetGroup = _editingGroupTarget.value ?: return
        val pathsToAdd = _selectedReceiptPaths.value
        
        if (pathsToAdd.isEmpty()) {
            _editingGroupTarget.value = null
            _isGroupSelectionMode.value = false
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Combine existing paths with new paths (avoiding duplicates)
                val allPaths = (targetGroup.receiptPaths + pathsToAdd).distinct()
                
                // Load all receipts to calculate new total
                val receipts = allPaths.mapNotNull { path ->
                    receiptRepository.loadReceipt(path)
                }
                val newTotal = receipts.sumOf { it.total }
                
                val updatedGroup = targetGroup.copy(
                    receiptPaths = allPaths,
                    receiptCount = allPaths.size,
                    totalAmount = newTotal
                )
                groupedReceiptRepository.updateGroupedReceipt(updatedGroup)
                
                withContext(Dispatchers.Main) {
                    _editingGroupTarget.value = null
                    _isGroupSelectionMode.value = false
                    _selectedReceiptPaths.value = emptySet()
                    _successMessage.value = "Struk berhasil ditambahkan ke kelompok"
                }
                loadGroupedReceipts()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to add receipts to group", e)
            }
        }
    }

    fun cancelAddingToGroup() {
        _editingGroupTarget.value = null
        _isGroupSelectionMode.value = false
        _selectedReceiptPaths.value = emptySet()
    }

    fun printGroupedReceipt(group: GroupedReceipt) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Load all receipts in this group
                val receipts = group.receiptPaths.mapNotNull { path ->
                    receiptRepository.loadReceipt(path)
                }
                
                if (receipts.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        _errorMessage.value = "Tidak ada struk dalam kelompok ini"
                    }
                    return@launch
                }
                
                val design = _receiptDesign.value
                val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                val width = design.paperWidth
                
                // Helper function for aligned columns with colon
                fun alignedRow(label: String, value: String): String {
                    val labelWithColon = "$label:"
                    val space = width - labelWithColon.length - value.length
                    return if (space > 0) labelWithColon + " ".repeat(space) + value
                    else labelWithColon + value.take(width - labelWithColon.length)
                }
                
                fun formatCurrency(amount: Int): String {
                    return "Rp${String.format("%,d", amount).replace(',', '.')}"
                }
                
                // Use EscPosBuilder for proper formatting
                val builder = com.dicoding.warmapos.bluetooth.EscPosBuilder()
                builder.paperWidth = width
                builder.init()
                
                // Header with large store name
                builder.alignCenter()
                builder.bold(true)
                builder.doubleSize(true)
                builder.printLine(design.storeName.ifEmpty { "POS WARMA" })
                builder.doubleSize(false)
                builder.bold(false)
                
                if (design.headerText.isNotEmpty()) {
                    builder.printLine(design.headerText)
                }
                
                builder.doubleSeparator()
                
                // Group info - left aligned with colon alignment
                builder.alignLeft()
                builder.bold(true)
                builder.printLine("KELOMPOK STRUK")
                builder.bold(false)
                builder.printLine(alignedRow("Nama", group.name))
                builder.printLine(alignedRow("Tanggal", sdf.format(java.util.Date(group.timestamp))))
                builder.printLine(alignedRow("Jumlah", "${receipts.size} struk"))
                
                builder.doubleSeparator()
                
                // Set line spacing for better readability
                builder.setLineSpacing(32)
                
                // List each receipt with lembar and keterangan
                receipts.forEachIndexed { index, receipt ->
                    builder.bold(true)
                    builder.printLine("--- Struk #${index + 1} ---")
                    builder.bold(false)
                    builder.printLine(alignedRow("Waktu", sdf.format(java.util.Date(receipt.timestamp))))
                    builder.printLine(alignedRow("Lembar", receipt.lembarKe.toString()))
                    if (receipt.keterangan.isNotBlank()) {
                        builder.printLine(alignedRow("Ket", receipt.keterangan))
                    }
                    builder.printLine(alignedRow("Items", "${receipt.items.sumOf { it.quantity }} pcs"))
                    builder.bold(true)
                    builder.printLine(alignedRow("Total", formatCurrency(receipt.total)))
                    builder.bold(false)
                    builder.printLine("")
                }
                
                // Reset line spacing
                builder.resetLineSpacing()
                
                builder.doubleSeparator()
                
                // Grand total section
                builder.bold(true)
                builder.printLine(alignedRow("TOTAL STRUK", "${receipts.size}"))
                builder.printLine(alignedRow("GRAND TOTAL", formatCurrency(group.totalAmount)))
                builder.bold(false)
                
                builder.separator()
                
                // Footer
                builder.alignCenter()
                if (design.footerText.isNotEmpty()) {
                    builder.printLine(design.footerText)
                }
                
                builder.feed(3)
                builder.cut()
                
                val result = printerManager.sendRaw(builder.build())
                
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        _successMessage.value = "Kelompok struk berhasil dicetak"
                    } else {
                        _errorMessage.value = "Gagal mencetak: ${result.exceptionOrNull()?.message}"
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error printing grouped receipt", e)
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Gagal mencetak: ${e.message}"
                }
            }
        }
    }

    private val _kasirName = MutableStateFlow("Kasir")
    val kasirName: StateFlow<String> = _kasirName.asStateFlow()

    // Lembar Ke and Keterangan for receipt
    private val _lembarKe = MutableStateFlow(1)
    val lembarKe: StateFlow<Int> = _lembarKe.asStateFlow()

    private val _keterangan = MutableStateFlow("Asli")
    val keterangan: StateFlow<String> = _keterangan.asStateFlow()

    private val _keteranganOptions = MutableStateFlow<List<String>>(emptyList())
    val keteranganOptions: StateFlow<List<String>> = _keteranganOptions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _productCount = MutableStateFlow(0)
    val productCount: StateFlow<Int> = _productCount.asStateFlow()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    // OCR selected image - persisted across tab switches
    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    // App theme - UI color scheme (persisted)
    private val _appTheme = MutableStateFlow(loadSavedTheme())
    val appTheme: StateFlow<com.dicoding.warmapos.ui.theme.AppTheme> = _appTheme.asStateFlow()

    private fun loadSavedTheme(): com.dicoding.warmapos.ui.theme.AppTheme {
        val themeName = settingsRepository.getAppTheme()
        return try {
            com.dicoding.warmapos.ui.theme.AppTheme.valueOf(themeName)
        } catch (e: Exception) {
            com.dicoding.warmapos.ui.theme.AppTheme.EMERALD
        }
    }

    fun setAppTheme(theme: com.dicoding.warmapos.ui.theme.AppTheme) {
        _appTheme.value = theme
        settingsRepository.saveAppTheme(theme.name)
    }
    
    // OCR API Key (configurable)
    private val _ocrApiKey = MutableStateFlow(settingsRepository.getOcrApiKey())
    val ocrApiKey: StateFlow<String> = _ocrApiKey.asStateFlow()
    
    fun setOcrApiKey(key: String) {
        val trimmedKey = key.trim()
        _ocrApiKey.value = trimmedKey
        settingsRepository.saveOcrApiKey(trimmedKey)
    }
    
    fun resetOcrApiKey() {
        _ocrApiKey.value = SettingsRepository.DEFAULT_OCR_API_KEY
        settingsRepository.saveOcrApiKey(SettingsRepository.DEFAULT_OCR_API_KEY)
    }
    
    // OCR Mode (online/offline)
    private val _ocrMode = MutableStateFlow(settingsRepository.getOcrMode())
    val ocrMode: StateFlow<String> = _ocrMode.asStateFlow()
    
    fun setOcrMode(mode: String) {
        _ocrMode.value = mode
        settingsRepository.saveOcrMode(mode)
    }
    
    fun isOfflineOcrEnabled(): Boolean = _ocrMode.value == SettingsRepository.OCR_MODE_OFFLINE

    // Editing receipt
    private var editingReceiptPath: String? = null

    val cartTotal: Int
        get() = _cartItems.value.sumOf { it.subtotal }

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch(Dispatchers.IO) {
            // Load products (try internal storage first, then assets)
            val products = productRepository.loadProducts()
            
            withContext(Dispatchers.Main) {
                productMatcher.setProducts(products)
                _products.value = products
                _productCount.value = products.size
            }
            
            // Load synonyms
            val synonyms = synonymRepository.getSynonyms()
            withContext(Dispatchers.Main) {
                _synonyms.value = synonyms
            }
            
            // Load settings (lightweight)
            val design = settingsRepository.getReceiptDesign()
            val kasir = settingsRepository.getKasirName()
            withContext(Dispatchers.Main) {
                _receiptDesign.value = design
                _kasirName.value = kasir
            }

            // Load keterangan options
            val ketOptions = settingsRepository.getKeteranganOptions()
            withContext(Dispatchers.Main) {
                _keteranganOptions.value = ketOptions
                // Set default keterangan if available
                if (ketOptions.isNotEmpty() && _keterangan.value.isBlank()) {
                    _keterangan.value = ketOptions.first()
                }
            }
            
            // Load history in background
            refreshReceiptHistory()
        }
    }

    // ===== SEARCH =====
    private var searchJob: kotlinx.coroutines.Job? = null
    
    fun searchProducts(query: String) {
        searchJob?.cancel()
        
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch {
            // Small delay to debounce rapid typing
            kotlinx.coroutines.delay(150)
            
            // Run fuzzy matching on background thread - show all results >= 40% match
            val matches = withContext(Dispatchers.Default) {
                val (processedQuery, _, _) = synonymRepository.applySynonyms(query)
                productMatcher.findMatches(processedQuery, topK = Int.MAX_VALUE, threshold = 0.4f)
            }
            _searchResults.value = matches
        }
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
    }

    // ===== CART =====
    fun addToCart(product: Product, quantity: Int = 1) {
        val currentItems = _cartItems.value.toMutableList()
        val existingIndex = currentItems.indexOfFirst {
            it.product.name.equals(product.name, ignoreCase = true)
        }

        if (existingIndex >= 0) {
            val existing = currentItems[existingIndex]
            currentItems[existingIndex] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            currentItems.add(CartItem(product = product, quantity = quantity))
        }

        _cartItems.value = currentItems
    }

    fun updateCartQuantity(itemId: String, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeFromCart(itemId)
            return
        }

        val currentItems = _cartItems.value.toMutableList()
        val index = currentItems.indexOfFirst { it.id == itemId }
        if (index >= 0) {
            currentItems[index] = currentItems[index].copy(quantity = newQuantity)
            _cartItems.value = currentItems
        }
    }

    fun removeFromCart(itemId: String) {
        _cartItems.value = _cartItems.value.filter { it.id != itemId }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        editingReceiptPath = null
        // Reset lembar ke and keterangan for new transaction
        _lembarKe.value = 1
        _keterangan.value = _keteranganOptions.value.firstOrNull() ?: "Asli"
    }

    // Update lembar ke
    fun updateLembarKe(value: Int) {
        if (value >= 1) {
            _lembarKe.value = value
        }
    }

    // Update keterangan
    fun updateKeterangan(value: String) {
        _keterangan.value = value
    }

    // Keterangan options management
    fun addKeteranganOption(option: String) {
        settingsRepository.addKeteranganOption(option)
        _keteranganOptions.value = settingsRepository.getKeteranganOptions()
    }

    fun removeKeteranganOption(option: String) {
        // Prevent removing 'Asli' as it's the default option
        if (option == "Asli") return
        
        settingsRepository.removeKeteranganOption(option)
        _keteranganOptions.value = settingsRepository.getKeteranganOptions()
        // If removed option was selected, reset to first available
        if (_keterangan.value == option) {
            _keterangan.value = _keteranganOptions.value.firstOrNull() ?: "Asli"
        }
    }

    // ===== OCR =====
    fun setSelectedImageUri(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    fun processOcr(imageUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val isOffline = isOfflineOcrEnabled()
            Log.d("MainViewModel", "Starting OCR processing... Mode: ${if (isOffline) "Offline (ML Kit)" else "Online (OCR.space)"}")
            
            val result = if (isOffline) {
                // Use ML Kit for offline OCR
                mlKitOcrHandler.processImage(imageUri)
            } else {
                // Use OCR.space API for online OCR
                val apiKey = settingsRepository.getOcrApiKey()
                ocrHandler.processImage(imageUri, apiKey)
            }

            result.fold(
                onSuccess = { items ->
                    Log.d("MainViewModel", "OCR returned ${items.size} items, starting fuzzy matching...")
                    
                    // Move heavy fuzzy matching to background thread
                    val resultsWithMatches = withContext(Dispatchers.Default) {
                        items.mapIndexed { index, ocrItem ->
                            Log.d("MainViewModel", "Processing item ${index + 1}/${items.size}: ${ocrItem.processedText}")
                            
                            // Apply synonyms
                            val (processed, replaced, original) = synonymRepository.applySynonyms(ocrItem.processedText)
                            val updatedItem = ocrItem.copy(
                                processedText = processed,
                                wasSynonymReplaced = replaced,
                                originalSynonym = original
                            )
                            // Get matches (5 per item)
                            val matches = productMatcher.findMatches(processed, topK = 5)
                            OcrResultWithMatches(updatedItem, matches)
                        }
                    }
                    
                    Log.d("MainViewModel", "Fuzzy matching complete, updating UI...")
                    _ocrResults.value = resultsWithMatches
                    _successMessage.value = "${items.size} item terdeteksi"
                    Log.d("MainViewModel", "OCR processing complete!")
                },
                onFailure = { e ->
                    Log.e("MainViewModel", "OCR failed: ${e.message}")
                    _errorMessage.value = e.message ?: "OCR gagal"
                }
            )

            _isLoading.value = false
        }
    }

    fun clearOcrResults() {
        _ocrResults.value = emptyList()
    }

    // ===== RECEIPT =====
    fun saveReceipt(): String? {
        val items = _cartItems.value
        if (items.isEmpty()) return null

        val receipt = Receipt(
            kasir = _kasirName.value,
            storeName = _receiptDesign.value.storeName,
            items = items.map { ReceiptItem.fromCartItem(it) },
            total = cartTotal,
            lembarKe = _lembarKe.value,
            keterangan = _keterangan.value
        )

        return if (editingReceiptPath != null) {
            if (receiptRepository.updateReceipt(receipt, editingReceiptPath!!)) {
                refreshReceiptHistory()
                _successMessage.value = "Struk diperbarui"
                clearCart()  // Clear cart after update
                editingReceiptPath
            } else null
        } else {
            val path = receiptRepository.saveReceipt(receipt)
            refreshReceiptHistory()
            _successMessage.value = "Struk tersimpan"
            clearCart()  // Clear cart after save
            path
        }
    }

    fun loadReceiptToCart(path: String) {
        val receipt = receiptRepository.loadReceipt(path) ?: return

        val cartItems = receipt.items.map { item ->
            val existingProduct = productRepository.getProductByName(item.name)
            val product = existingProduct ?: Product(
                name = item.name,
                sku = item.sku,
                price = item.price,
                unit = item.unit
            )
            CartItem(product = product, quantity = item.quantity)
        }

        _cartItems.value = cartItems
        editingReceiptPath = path
        _kasirName.value = receipt.kasir
        _lembarKe.value = receipt.lembarKe
        _keterangan.value = receipt.keterangan
        _successMessage.value = "Struk dimuat"
    }

    fun deleteReceipt(path: String) {
        if (receiptRepository.deleteReceipt(path)) {
            refreshReceiptHistory()
            _successMessage.value = "Struk dihapus"
        }
    }

    private fun refreshReceiptHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            val history = receiptRepository.getReceiptHistory()
            withContext(Dispatchers.Main) {
                _receiptHistory.value = history
            }
        }
    }

    fun generateReceiptPreview(): String {
        val items = _cartItems.value
        if (items.isEmpty()) return ""

        val receipt = Receipt(
            kasir = _kasirName.value,
            storeName = _receiptDesign.value.storeName,
            items = items.map { ReceiptItem.fromCartItem(it) },
            total = cartTotal,
            lembarKe = _lembarKe.value,
            keterangan = _keterangan.value
        )

        return receiptPrinter.generatePreview(receipt, _receiptDesign.value)
    }

    // ===== PRINTING =====
    fun refreshPairedDevices() {
        _pairedDevices.value = printerManager.getPairedDevices()
    }

    fun connectPrinter(address: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = printerManager.connect(address)
            result.fold(
                onSuccess = {
                    settingsRepository.savePrinterAddress(address)
                    _successMessage.value = "Printer terhubung"
                },
                onFailure = { e ->
                    _errorMessage.value = "Gagal terhubung: ${e.message}"
                }
            )
            _isLoading.value = false
        }
    }
    
    fun connectAndSavePrinter(address: String, name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = printerManager.connectAndSave(address, name)
            result.fold(
                onSuccess = {
                    settingsRepository.savePrinterAddress(address)
                    _successMessage.value = "Printer '$name' disimpan & terhubung"
                    refreshPairedDevices()
                },
                onFailure = { e ->
                    _errorMessage.value = "Gagal terhubung: ${e.message}"
                }
            )
            _isLoading.value = false
        }
    }

    fun disconnectPrinter() {
        printerManager.disconnect()
        _successMessage.value = "Printer terputus"
    }

    fun testPrint() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = printerManager.testPrint()
            result.fold(
                onSuccess = { _successMessage.value = "Test print berhasil" },
                onFailure = { e -> _errorMessage.value = "Test print gagal: ${e.message}" }
            )
            _isLoading.value = false
        }
    }

    fun printReceipt() {
        viewModelScope.launch {
            val items = _cartItems.value
            if (items.isEmpty()) {
                _errorMessage.value = "Keranjang kosong"
                return@launch
            }

            _isLoading.value = true

            val receipt = Receipt(
                kasir = _kasirName.value,
                storeName = _receiptDesign.value.storeName,
                items = items.map { ReceiptItem.fromCartItem(it) },
                total = cartTotal,
                lembarKe = _lembarKe.value,
                keterangan = _keterangan.value
            )

            val result = receiptPrinter.printReceipt(receipt, _receiptDesign.value)
            result.fold(
                onSuccess = { _successMessage.value = "Berhasil dicetak" },
                onFailure = { e -> _errorMessage.value = "Gagal cetak: ${e.message}" }
            )

            _isLoading.value = false
        }
    }

    // ===== SETTINGS =====
    fun updateReceiptDesign(design: ReceiptDesign) {
        _receiptDesign.value = design
        settingsRepository.saveReceiptDesign(design)
    }

    fun updateKasirName(name: String) {
        _kasirName.value = name
        settingsRepository.saveKasirName(name)
    }

    // ===== SYNONYMS =====
    fun addSynonym(key: String, value: String) {
        synonymRepository.addSynonym(key, value)
        _synonyms.value = synonymRepository.getSynonyms()
    }

    fun removeSynonym(key: String) {
        synonymRepository.removeSynonym(key)
        _synonyms.value = synonymRepository.getSynonyms()
    }

    // ===== IMPORT CSV =====
    fun importCsv(content: String, replaceExisting: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val count = productRepository.loadFromFile(content, replaceExisting)
                val allProducts = productRepository.getProducts()
                productMatcher.setProducts(allProducts)
                _products.value = allProducts
                _productCount.value = productRepository.getProductCount()
                _successMessage.value = "Berhasil import $count produk"
            } catch (e: Exception) {
                _errorMessage.value = "Gagal import: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    // ===== MESSAGES =====
    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSuccess() {
        _successMessage.value = null
    }
    
    fun loadReceiptHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            val history = receiptRepository.getReceiptHistory()
            _receiptHistory.value = history
        }
    }
    
    fun getReceiptByPath(filePath: String): com.dicoding.warmapos.data.model.Receipt? {
        return receiptRepository.loadReceipt(filePath)
    }

    fun deleteReceipt(item: ReceiptHistoryItem) {
        viewModelScope.launch(Dispatchers.IO) {
            if (receiptRepository.deleteReceipt(item.filePath)) {
                loadReceiptHistory()
            }
        }
    }

    fun reprintReceipt(item: ReceiptHistoryItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                val receipt = receiptRepository.loadReceipt(item.filePath)
                if (receipt != null) {
                    val result = receiptPrinter.printReceipt(receipt, _receiptDesign.value)
                    withContext(Dispatchers.Main) {
                        result.fold(
                            onSuccess = { _successMessage.value = "Struk berhasil dicetak ulang" },
                            onFailure = { e -> _errorMessage.value = "Gagal cetak: ${e.message}" }
                        )
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _errorMessage.value = "Struk tidak ditemukan"
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error reprinting receipt", e)
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Gagal cetak: ${e.message}"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun reuseReceipt(item: ReceiptHistoryItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val receipt = receiptRepository.loadReceipt(item.filePath)
            if (receipt != null) {
                val newCartItems = receipt.items.map { receiptItem ->
                    val product = Product(
                        name = receiptItem.name,
                        sku = receiptItem.sku,
                        price = receiptItem.price,
                        unit = receiptItem.unit
                    )
                    CartItem(
                        product = product,
                        quantity = receiptItem.quantity
                    )
                }
                withContext(Dispatchers.Main) {
                    _cartItems.value = newCartItems
                    editingReceiptPath = item.filePath
                    _kasirName.value = receipt.kasir
                    _lembarKe.value = receipt.lembarKe
                    _keterangan.value = receipt.keterangan
                    _successMessage.value = "Struk dimuat untuk diedit"
                }
            }
        }
    }
    
    // ===== PRODUCT MANAGEMENT =====
    fun addProduct(name: String, sku: String, category: String, price: Int, unit: String): Boolean {
        val product = Product(name = name, sku = sku, category = category, price = price, unit = unit)
        val success = productRepository.addProduct(product)
        if (success) {
            refreshProducts()
            _successMessage.value = "Produk ditambahkan"
        } else {
            _errorMessage.value = "Produk dengan nama sama sudah ada"
        }
        return success
    }
    
    fun updateProduct(oldName: String, name: String, sku: String, category: String, price: Int, unit: String): Boolean {
        val product = Product(name = name, sku = sku, category = category, price = price, unit = unit)
        val success = productRepository.updateProduct(oldName, product)
        if (success) {
            refreshProducts()
            _successMessage.value = "Produk diperbarui"
        } else {
            _errorMessage.value = "Gagal memperbarui produk"
        }
        return success
    }
    
    fun deleteProduct(productName: String): Boolean {
        val success = productRepository.deleteProduct(productName)
        if (success) {
            // Immediately update state on main thread for instant UI refresh
            val updatedProducts = productRepository.getProducts()
            productMatcher.setProducts(updatedProducts)
            _products.value = updatedProducts
            _productCount.value = updatedProducts.size
            _successMessage.value = "Produk dihapus"
        }
        return success
    }
    
    fun exportProducts(): java.io.File? {
        return productRepository.exportToExternalFile()
    }
    
    private fun refreshProducts() {
        viewModelScope.launch(Dispatchers.IO) {
            val products = productRepository.getProducts()
            productMatcher.setProducts(products)
            withContext(Dispatchers.Main) {
                _products.value = products
                _productCount.value = products.size
            }
        }
    }
    
    // ===== BACKUP & RESTORE =====
    fun createBackup(): java.io.File? {
        val backupFile = backupRepository.createBackup()
        if (backupFile != null) {
            _successMessage.value = "Backup berhasil dibuat"
        } else {
            _errorMessage.value = "Gagal membuat backup"
        }
        return backupFile
    }
    
    fun restoreBackup(zipUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { _isLoading.value = true }
            
            val success = backupRepository.restoreBackup(zipUri)
            
            if (success) {
                // Reload all data after restore
                val products = productRepository.loadProducts()
                val synonyms = synonymRepository.getSynonyms()
                val design = settingsRepository.getReceiptDesign()
                val kasir = settingsRepository.getKasirName()
                
                withContext(Dispatchers.Main) {
                    productMatcher.setProducts(products)
                    _products.value = products
                    _productCount.value = products.size
                    _synonyms.value = synonyms
                    _receiptDesign.value = design
                    _kasirName.value = kasir
                    _successMessage.value = "Restore berhasil! ${products.size} produk dimuat"
                }
                
                // Refresh receipt history
                refreshReceiptHistory()
            } else {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Gagal restore backup"
                }
            }
            
            withContext(Dispatchers.Main) { _isLoading.value = false }
        }
    }
    
    fun getBackupFiles(): List<BackupInfo> {
        return backupRepository.getBackupFiles()
    }
}

/**
 * OCR result with product matches
 */
data class OcrResultWithMatches(
    val ocrItem: OcrItem,
    val matches: List<ProductMatch>
)
