package com.dicoding.warmapos.data.repository

import android.content.Context
import com.dicoding.warmapos.data.model.Product
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Repository for managing product data from CSV
 * Supports multiple CSV formats with auto-detection
 */
class ProductRepository(private val context: Context) {

    private var products: MutableList<Product> = mutableListOf()
    private var isLoaded = false
    
    // Column indices - will be detected from header
    private var colName = 0
    private var colSku = 1
    private var colCategory = 2
    private var colPrice = 3
    private var colUnit = 4

    /**
     * Load products from assets CSV file
     */
    fun loadFromAssets(filename: String = "ecopos_product.csv"): List<Product> {
        if (isLoaded && products.isNotEmpty()) {
            return products
        }

        try {
            context.assets.open(filename).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    products.clear()
                    var isHeader = true

                    reader.forEachLine { line ->
                        if (isHeader) {
                            isHeader = false
                            detectColumnIndices(line)
                            return@forEachLine
                        }

                        val product = parseProductLine(line)
                        if (product != null) {
                            products.add(product)
                        }
                    }
                }
            }
            isLoaded = true
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return products
    }

    /**
     * Load products from external CSV file (for import feature)
     */
    fun loadFromFile(csvContent: String, replaceExisting: Boolean): Int {
        val newProducts = mutableListOf<Product>()
        var isHeader = true

        csvContent.lines().forEach { line ->
            if (line.isBlank()) return@forEach
            
            if (isHeader) {
                isHeader = false
                detectColumnIndices(line)
                return@forEach
            }

            val product = parseProductLine(line)
            if (product != null) {
                newProducts.add(product)
            }
        }

        if (replaceExisting) {
            products.clear()
        }

        products.addAll(newProducts)
        
        // Save to internal storage so changes persist after app restart
        saveToInternalStorage()
        
        return newProducts.size
    }

    /**
     * Auto-detect column indices from header row
     * Supports formats:
     * - 12 columns: name,sku,barcode,category,sales_price,product_cost,stock,warning_limit,unit,...
     * - 5 columns: name,sku,category,sales_price,unit
     */
    private fun detectColumnIndices(headerLine: String) {
        val headers = parseCsvLine(headerLine).map { it.trim().lowercase() }
        
        // Find indices for each required column
        colName = headers.indexOfFirst { it == "name" || it == "nama" || it == "product_name" }
        colSku = headers.indexOfFirst { it == "sku" || it == "kode" || it == "code" }
        colCategory = headers.indexOfFirst { it == "category" || it == "kategori" }
        colPrice = headers.indexOfFirst { it == "sales_price" || it == "price" || it == "harga" }
        colUnit = headers.indexOfFirst { it == "unit" || it == "satuan" }
        
        // Set defaults if not found
        if (colName < 0) colName = 0
        if (colSku < 0) colSku = 1
        if (colCategory < 0) colCategory = if (headers.size > 5) 3 else 2  // Skip barcode if 12-col format
        if (colPrice < 0) colPrice = if (headers.size > 5) 4 else 3
        if (colUnit < 0) colUnit = if (headers.size > 5) 8 else 4
    }

    /**
     * Parse a CSV line into a Product using detected column indices
     */
    private fun parseProductLine(line: String): Product? {
        if (line.isBlank()) return null
        
        try {
            val parts = parseCsvLine(line)
            if (parts.size <= colName) return null

            val name = parts.getOrNull(colName)?.trim() ?: ""
            if (name.isEmpty()) return null

            return Product(
                name = name,
                sku = parts.getOrNull(colSku)?.trim() ?: "",
                category = parts.getOrNull(colCategory)?.trim() ?: "",
                price = parsePrice(parts.getOrNull(colPrice) ?: "0"),
                unit = parts.getOrNull(colUnit)?.trim()?.ifEmpty { "pcs" } ?: "pcs"
            )
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Parse CSV line handling quoted values (for prices like "1,500")
     */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString())

        return result
    }

    /**
     * Parse price string (handles formats like "1,500" or "1500" or "1500.00")
     */
    private fun parsePrice(priceStr: String): Int {
        val cleaned = priceStr.trim()
            .replace("\"", "")
            .replace(",", "")  // Remove thousand separator
            .replace("Rp", "")
            .replace(".", "")  // Remove Indonesian thousand separator
            .trim()
        return try {
            cleaned.toDouble().toInt()
        } catch (e: Exception) {
            0
        }
    }

    fun getProducts(): List<Product> = products.toList()

    fun getProductCount(): Int = products.size

    fun getProductByName(name: String): Product? {
        return products.find { it.name.equals(name, ignoreCase = true) }
    }

    fun clear() {
        products.clear()
        isLoaded = false
    }
    
    // ===== Product Management =====
    
    fun addProduct(product: Product): Boolean {
        // Check if product with same name already exists
        if (products.any { it.name.equals(product.name, ignoreCase = true) }) {
            return false
        }
        products.add(product)
        saveToInternalStorage()
        return true
    }
    
    fun updateProduct(oldName: String, updatedProduct: Product): Boolean {
        val index = products.indexOfFirst { it.name.equals(oldName, ignoreCase = true) }
        if (index >= 0) {
            products[index] = updatedProduct
            saveToInternalStorage()
            return true
        }
        return false
    }
    
    fun deleteProduct(productName: String): Boolean {
        val removed = products.removeAll { it.name.equals(productName, ignoreCase = true) }
        if (removed) {
            saveToInternalStorage()
        }
        return removed
    }
    
    /**
     * Export products to CSV string
     */
    fun exportToCsv(): String {
        val sb = StringBuilder()
        // Header
        sb.appendLine("name,sku,category,sales_price,unit")
        // Data
        products.forEach { p ->
            sb.appendLine("\"${p.name}\",\"${p.sku}\",\"${p.category}\",${p.price},\"${p.unit}\"")
        }
        return sb.toString()
    }
    
    /**
     * Save products to internal storage (for persistence after edits)
     */
    private fun saveToInternalStorage() {
        try {
            val csvContent = exportToCsv()
            context.openFileOutput("products.csv", Context.MODE_PRIVATE).use { output ->
                output.write(csvContent.toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Load from internal storage if exists, otherwise from assets
     */
    fun loadProducts(): List<Product> {
        // Try internal storage first (user-modified data)
        try {
            val file = context.getFileStreamPath("products.csv")
            if (file.exists()) {
                val content = context.openFileInput("products.csv").bufferedReader().readText()
                if (content.isNotBlank()) {
                    products.clear()
                    loadFromFile(content, replaceExisting = true)
                    isLoaded = true
                    return products
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Fall back to assets
        return loadFromAssets()
    }
    
    /**
     * Export CSV file to external storage for sharing
     */
    fun exportToExternalFile(): java.io.File? {
        return try {
            val csvContent = exportToCsv()
            val exportDir = context.getExternalFilesDir(null) ?: return null
            val file = java.io.File(exportDir, "warmapos_products_${System.currentTimeMillis()}.csv")
            file.writeText(csvContent)
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
