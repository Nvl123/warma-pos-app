package com.dicoding.warmapos.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

/**
 * Bluetooth Printer Manager for thermal printers
 * Supports saved printer configuration for quick printing
 */
class BluetoothPrinterManager(private val context: Context) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    
    private val prefs = context.getSharedPreferences("printer_prefs", Context.MODE_PRIVATE)

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var connectedDevice: BluetoothDevice? = null

    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    val isConnected: Boolean
        get() = socket?.isConnected == true

    val connectedDeviceName: String?
        @SuppressLint("MissingPermission")
        get() = if (hasBluetoothPermission()) connectedDevice?.name else null
    
    // Track last successful activity to detect stale connections
    private var lastActivityTime: Long = 0
    private val CONNECTION_TIMEOUT_MS = 5 * 60 * 1000L  // 5 minutes
    
    private fun isConnectionStale(): Boolean {
        if (!isConnected) return true
        if (lastActivityTime == 0L) return true
        return System.currentTimeMillis() - lastActivityTime > CONNECTION_TIMEOUT_MS
    }
    
    private fun updateActivity() {
        lastActivityTime = System.currentTimeMillis()
    }
    
    // ===== SAVED PRINTER =====
    
    val savedPrinterAddress: String?
        get() = prefs.getString("saved_printer_address", null)
    
    val savedPrinterName: String?
        get() = prefs.getString("saved_printer_name", null)
    
    fun savePrinter(address: String, name: String) {
        prefs.edit()
            .putString("saved_printer_address", address)
            .putString("saved_printer_name", name)
            .apply()
    }
    
    fun clearSavedPrinter() {
        prefs.edit()
            .remove("saved_printer_address")
            .remove("saved_printer_name")
            .apply()
    }
    
    /**
     * Ensure connected to saved printer, auto-connect if not
     * Also reconnects if connection is stale (idle > 5 minutes)
     * Call this before printing
     */
    @SuppressLint("MissingPermission")
    suspend fun ensureConnected(): Result<Unit> = withContext(Dispatchers.IO) {
        // Check if connection is stale and needs refresh
        if (isConnected && isConnectionStale()) {
            android.util.Log.d("BluetoothPrinter", "Connection stale, reconnecting...")
            disconnect()
        }
        
        // Already connected and not stale
        if (isConnected) {
            return@withContext Result.success(Unit)
        }
        
        // No saved printer
        val address = savedPrinterAddress
        if (address == null) {
            return@withContext Result.failure(Exception("Belum ada printer tersimpan. Pilih printer di Pengaturan."))
        }
        
        // Auto-connect to saved printer
        val result = connect(address)
        if (result.isSuccess) {
            updateActivity()
        }
        result
    }

    /**
     * Check if Bluetooth permissions are granted
     */
    fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Get paired Bluetooth devices
     */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDeviceInfo> {
        if (!hasBluetoothPermission()) return emptyList()
        if (bluetoothAdapter == null) return emptyList()

        val savedAddress = savedPrinterAddress
        
        return bluetoothAdapter.bondedDevices?.map { device ->
            BluetoothDeviceInfo(
                name = device.name ?: "Unknown",
                address = device.address,
                isSaved = device.address == savedAddress
            )
        }?.sortedByDescending { it.isSaved } ?: emptyList()
    }

    /**
     * Connect to a Bluetooth device with retry logic
     * Includes cancelDiscovery, initial delay, and fallback socket method
     */
    @SuppressLint("MissingPermission")
    suspend fun connect(address: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!hasBluetoothPermission()) {
            return@withContext Result.failure(Exception("Bluetooth permission not granted"))
        }

        val device = bluetoothAdapter?.getRemoteDevice(address)
            ?: return@withContext Result.failure(Exception("Device not found"))

        // Cancel discovery as it interferes with connection
        try {
            bluetoothAdapter?.cancelDiscovery()
        } catch (e: Exception) {
            android.util.Log.w("BluetoothPrinter", "Could not cancel discovery: ${e.message}")
        }

        var lastException: Exception? = null
        val maxRetries = 3
        
        for (attempt in 1..maxRetries) {
            try {
                // Disconnect existing connection
                disconnect()
                
                // Delay to let printer/Bluetooth stabilize
                // First attempt: 300ms, subsequent: 500ms
                val delayMs = if (attempt == 1) 300L else 500L
                android.util.Log.d("BluetoothPrinter", "Connect attempt $attempt, waiting ${delayMs}ms...")
                kotlinx.coroutines.delay(delayMs)

                // Try standard method first, then fallback
                socket = if (attempt <= 2) {
                    device.createRfcommSocketToServiceRecord(SPP_UUID)
                } else {
                    // Fallback: use reflection to create socket on channel 1
                    android.util.Log.d("BluetoothPrinter", "Using fallback socket method...")
                    try {
                        val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                        method.invoke(device, 1) as BluetoothSocket
                    } catch (e: Exception) {
                        device.createRfcommSocketToServiceRecord(SPP_UUID)
                    }
                }
                
                socket?.connect()

                outputStream = socket?.outputStream
                connectedDevice = device
                
                android.util.Log.d("BluetoothPrinter", "Connected successfully on attempt $attempt")
                return@withContext Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e("BluetoothPrinter", "Connect attempt $attempt failed: ${e.message}")
                lastException = e
                disconnect()
            }
        }
        
        Result.failure(lastException ?: Exception("Failed to connect after $maxRetries attempts"))
    }
    
    /**
     * Connect and save as default printer
     */
    @SuppressLint("MissingPermission")
    suspend fun connectAndSave(address: String, name: String): Result<Unit> {
        val result = connect(address)
        if (result.isSuccess) {
            savePrinter(address, name)
        }
        return result
    }

    /**
     * Disconnect from the current device
     */
    fun disconnect() {
        try {
            outputStream?.close()
            socket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        outputStream = null
        socket = null
        connectedDevice = null
    }

    /**
     * Send raw bytes to the printer (auto-connects if needed)
     * Includes retry logic for stale connections
     */
    suspend fun sendRaw(data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        // Try to connect and send, with one retry on failure
        suspend fun tryPrint(): Result<Unit> {
            // Auto-connect to saved printer if not connected
            val connectResult = ensureConnected()
            if (connectResult.isFailure) {
                return connectResult
            }
            
            try {
                if (outputStream == null) {
                    return Result.failure(Exception("Not connected to printer"))
                }
                outputStream?.write(data)
                outputStream?.flush()
                updateActivity()  // Track successful print time
                return Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                // Connection lost
                disconnect()
                return Result.failure(e)
            }
        }
        
        // First attempt
        val firstResult = tryPrint()
        if (firstResult.isSuccess) {
            return@withContext firstResult
        }
        
        // Retry once - connection might have been stale
        val retryResult = tryPrint()
        if (retryResult.isSuccess) {
            return@withContext retryResult
        }
        
        // Both failed
        return@withContext Result.failure(
            Exception("Gagal mencetak setelah 2 percobaan. Coba hubungkan ulang printer di Pengaturan.")
        )
    }

    /**
     * Test print
     */
    suspend fun testPrint(): Result<Unit> {
        val builder = EscPosBuilder()
        builder.init()
        builder.alignCenter()
        builder.printLine("=== TEST PRINT ===")
        builder.printLine("Printer OK!")
        builder.printLine(java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date()))
        builder.feed(3)
        builder.cut()

        return sendRaw(builder.build())
    }

    companion object {
        // Standard SPP UUID for Bluetooth serial communication
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}

/**
 * Bluetooth device info
 */
data class BluetoothDeviceInfo(
    val name: String,
    val address: String,
    val isSaved: Boolean = false
)
