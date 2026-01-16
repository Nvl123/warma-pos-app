package com.dicoding.warmapos.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CropScreen(
    imageUri: Uri,
    onCropComplete: (Uri) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isCropping by remember { mutableStateOf(false) }
    var showTips by remember { mutableStateOf(true) }
    
    // Image transform state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    
    // Crop box state (relative to screen)
    var cropLeft by remember { mutableFloatStateOf(0f) }
    var cropTop by remember { mutableFloatStateOf(0f) }
    var cropRight by remember { mutableFloatStateOf(0f) }
    var cropBottom by remember { mutableFloatStateOf(0f) }
    var initialized by remember { mutableStateOf(false) }
    
    // Load bitmap
    LaunchedEffect(imageUri) {
        bitmap = loadBitmapFromUri(context, imageUri)
    }
    
    // Hoisted screen dimensions for use in button onClick
    var screenWidth by remember { mutableFloatStateOf(0f) }
    var screenHeight by remember { mutableFloatStateOf(0f) }
    
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val currentScreenWidth = with(density) { maxWidth.toPx() }
            val currentScreenHeight = with(density) { maxHeight.toPx() }
            
            // Update hoisted values
            LaunchedEffect(currentScreenWidth, currentScreenHeight) {
                screenWidth = currentScreenWidth
                screenHeight = currentScreenHeight
            }
            
            // Initialize crop box
            if (!initialized && screenWidth > 0) {
                val margin = with(density) { 40.dp.toPx() }
                cropLeft = margin
                cropTop = screenHeight * 0.2f
                cropRight = screenWidth - margin
                cropBottom = screenHeight * 0.65f
                initialized = true
            }
            
            bitmap?.let { bmp ->
                // Canvas with image and crop overlay
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 4f)
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        }
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    
                    // Image dimensions
                    val imgWidth = bmp.width.toFloat()
                    val imgHeight = bmp.height.toFloat()
                    
                    // Fit image
                    val fitScale = minOf(canvasWidth / imgWidth, canvasHeight / imgHeight)
                    val scaledWidth = imgWidth * fitScale * scale
                    val scaledHeight = imgHeight * fitScale * scale
                    
                    val left = (canvasWidth - scaledWidth) / 2 + offsetX
                    val top = (canvasHeight - scaledHeight) / 2 + offsetY
                    
                    // Draw image
                    drawImage(
                        image = bmp.asImageBitmap(),
                        dstOffset = IntOffset(left.toInt(), top.toInt()),
                        dstSize = IntSize(scaledWidth.toInt(), scaledHeight.toInt())
                    )
                    
                    // Draw darkened areas outside crop zone
                    val cropRect = Rect(cropLeft, cropTop, cropRight, cropBottom)
                    
                    drawRect(Color.Black.copy(alpha = 0.6f), Offset.Zero, Size(canvasWidth, cropRect.top))
                    drawRect(Color.Black.copy(alpha = 0.6f), Offset(0f, cropRect.bottom), Size(canvasWidth, canvasHeight - cropRect.bottom))
                    drawRect(Color.Black.copy(alpha = 0.6f), Offset(0f, cropRect.top), Size(cropRect.left, cropRect.height))
                    drawRect(Color.Black.copy(alpha = 0.6f), Offset(cropRect.right, cropRect.top), Size(canvasWidth - cropRect.right, cropRect.height))
                    
                    // Draw crop border
                    drawRect(Color.White, Offset(cropRect.left, cropRect.top), Size(cropRect.width, cropRect.height), style = Stroke(2.dp.toPx()))
                    
                    // Draw corner handles
                    val handleLen = 24.dp.toPx()
                    val handleStroke = 4.dp.toPx()
                    
                    // TL
                    drawLine(Color.White, Offset(cropRect.left, cropRect.top), Offset(cropRect.left + handleLen, cropRect.top), handleStroke)
                    drawLine(Color.White, Offset(cropRect.left, cropRect.top), Offset(cropRect.left, cropRect.top + handleLen), handleStroke)
                    // TR
                    drawLine(Color.White, Offset(cropRect.right - handleLen, cropRect.top), Offset(cropRect.right, cropRect.top), handleStroke)
                    drawLine(Color.White, Offset(cropRect.right, cropRect.top), Offset(cropRect.right, cropRect.top + handleLen), handleStroke)
                    // BL
                    drawLine(Color.White, Offset(cropRect.left, cropRect.bottom - handleLen), Offset(cropRect.left, cropRect.bottom), handleStroke)
                    drawLine(Color.White, Offset(cropRect.left, cropRect.bottom), Offset(cropRect.left + handleLen, cropRect.bottom), handleStroke)
                    // BR
                    drawLine(Color.White, Offset(cropRect.right - handleLen, cropRect.bottom), Offset(cropRect.right, cropRect.bottom), handleStroke)
                    drawLine(Color.White, Offset(cropRect.right, cropRect.bottom - handleLen), Offset(cropRect.right, cropRect.bottom), handleStroke)
                }
                
                // Drag handles for resizing crop box
                val handleSize = 48.dp
                val minCropSize = with(density) { 100.dp.toPx() }
                
                // Top-left handle
                Box(
                    modifier = Modifier
                        .offset { IntOffset((cropLeft - handleSize.toPx() / 2).toInt(), (cropTop - handleSize.toPx() / 2).toInt()) }
                        .size(handleSize)
                        .pointerInput(Unit) {
                            detectDragGestures { _, dragAmount ->
                                cropLeft = (cropLeft + dragAmount.x).coerceIn(0f, cropRight - minCropSize)
                                cropTop = (cropTop + dragAmount.y).coerceIn(0f, cropBottom - minCropSize)
                            }
                        }
                )
                
                // Top-right handle
                Box(
                    modifier = Modifier
                        .offset { IntOffset((cropRight - handleSize.toPx() / 2).toInt(), (cropTop - handleSize.toPx() / 2).toInt()) }
                        .size(handleSize)
                        .pointerInput(Unit) {
                            detectDragGestures { _, dragAmount ->
                                cropRight = (cropRight + dragAmount.x).coerceIn(cropLeft + minCropSize, screenWidth)
                                cropTop = (cropTop + dragAmount.y).coerceIn(0f, cropBottom - minCropSize)
                            }
                        }
                )
                
                // Bottom-left handle
                Box(
                    modifier = Modifier
                        .offset { IntOffset((cropLeft - handleSize.toPx() / 2).toInt(), (cropBottom - handleSize.toPx() / 2).toInt()) }
                        .size(handleSize)
                        .pointerInput(Unit) {
                            detectDragGestures { _, dragAmount ->
                                cropLeft = (cropLeft + dragAmount.x).coerceIn(0f, cropRight - minCropSize)
                                cropBottom = (cropBottom + dragAmount.y).coerceIn(cropTop + minCropSize, screenHeight)
                            }
                        }
                )
                
                // Bottom-right handle
                Box(
                    modifier = Modifier
                        .offset { IntOffset((cropRight - handleSize.toPx() / 2).toInt(), (cropBottom - handleSize.toPx() / 2).toInt()) }
                        .size(handleSize)
                        .pointerInput(Unit) {
                            detectDragGestures { _, dragAmount ->
                                cropRight = (cropRight + dragAmount.x).coerceIn(cropLeft + minCropSize, screenWidth)
                                cropBottom = (cropBottom + dragAmount.y).coerceIn(cropTop + minCropSize, screenHeight)
                            }
                        }
                )
            } ?: run {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
        
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp).align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(Icons.Default.Close, "Batal", tint = Color.White)
            }
            
            IconButton(
                onClick = { showTips = !showTips },
                modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(if (showTips) Icons.Default.VisibilityOff else Icons.Default.Info, "Tips", tint = Color.White)
            }
        }
        
        // Tips popup (hideable)
        if (showTips) {
            Card(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 72.dp, start = 16.dp, end = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("✨ Tips Crop", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("• Pinch: zoom gambar", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelSmall)
                    Text("• Drag gambar: geser posisi", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelSmall)
                    Text("• Drag sudut: ubah ukuran kotak", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        
        // Bottom actions
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    // Reset image transforms only
                    scale = 1f
                    offsetX = 0f
                    offsetY = 0f
                    // Reset crop to initial (re-trigger initialization)
                    initialized = false
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Reset")
            }
            
            Button(
                onClick = {
                    if (!isCropping) {
                        isCropping = true
                        bitmap?.let { bmp ->
                            // Calculate crop region in image coordinates
                            val canvasWidth = screenWidth
                            val canvasHeight = screenHeight
                            
                            // Image dimensions
                            val imgWidth = bmp.width.toFloat()
                            val imgHeight = bmp.height.toFloat()
                            
                            // Fit image calculations (same as in Canvas)
                            val fitScale = minOf(canvasWidth / imgWidth, canvasHeight / imgHeight)
                            val scaledWidth = imgWidth * fitScale * scale
                            val scaledHeight = imgHeight * fitScale * scale
                            
                            val left = (canvasWidth - scaledWidth) / 2 + offsetX
                            val top = (canvasHeight - scaledHeight) / 2 + offsetY
                            
                            // Convert screen crop box to image coordinates
                            val totalScale = fitScale * scale
                            val imgCropLeft = ((cropLeft - left) / totalScale).coerceIn(0f, imgWidth)
                            val imgCropTop = ((cropTop - top) / totalScale).coerceIn(0f, imgHeight)
                            val imgCropRight = ((cropRight - left) / totalScale).coerceIn(0f, imgWidth)
                            val imgCropBottom = ((cropBottom - top) / totalScale).coerceIn(0f, imgHeight)
                            
                            val cropX = imgCropLeft.toInt().coerceAtLeast(0)
                            val cropY = imgCropTop.toInt().coerceAtLeast(0)
                            val cropWidth = (imgCropRight - imgCropLeft).toInt().coerceAtLeast(1).coerceAtMost(bmp.width - cropX)
                            val cropHeight = (imgCropBottom - imgCropTop).toInt().coerceAtLeast(1).coerceAtMost(bmp.height - cropY)
                            
                            val croppedUri = saveCroppedImage(context, bmp, cropX, cropY, cropWidth, cropHeight)
                            onCropComplete(croppedUri ?: imageUri)
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = bitmap != null && !isCropping,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isCropping) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(4.dp))
                Text("Gunakan")
            }
        }
    }
}

private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    } catch (e: Exception) { null }
}

/**
 * Save cropped region of bitmap to file
 */
private fun saveCroppedImage(
    context: Context, 
    bitmap: Bitmap, 
    cropX: Int, 
    cropY: Int, 
    cropWidth: Int, 
    cropHeight: Int
): Uri? {
    return try {
        // Extract cropped region from bitmap
        val croppedBitmap = Bitmap.createBitmap(bitmap, cropX, cropY, cropWidth, cropHeight)
        
        val file = File(context.cacheDir, "crop_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg")
        FileOutputStream(file).use { croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        
        // Recycle cropped bitmap if it's a different object
        if (croppedBitmap != bitmap) {
            croppedBitmap.recycle()
        }
        
        Uri.fromFile(file)
    } catch (e: Exception) { 
        e.printStackTrace()
        null 
    }
}

