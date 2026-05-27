package com.zzeng.photoborder.ui.screens

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import android.graphics.Typeface
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.zzeng.photoborder.engine.BorderEngine
import com.zzeng.photoborder.engine.FontManager
import com.zzeng.photoborder.engine.TemplateManager
import androidx.compose.material3.ListItem
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val borderEngine = remember { BorderEngine(context) }
    val templateManager = remember { TemplateManager(context) }
    val fontManager = remember { FontManager(context) }

    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedTemplate by remember { mutableStateOf("zzeng_minimal") }
    var selectedFont by remember { mutableStateOf("yahei") }
    var isProcessing by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var total by remember { mutableStateOf(0) }
    var resultMessage by remember { mutableStateOf("") }
    var currentProcessingFile by remember { mutableStateOf("") }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showBatchDialog by remember { mutableStateOf(false) }
    var previewUri by remember { mutableStateOf<Uri?>(null) }
    var previewIndex by remember { mutableStateOf(0) }
    var previewBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isPreviewLoading by remember { mutableStateOf(false) }
    val previewCache = remember { mutableMapOf<String, android.graphics.Bitmap>() }

    // Multiple image picker
    val multipleImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedImageUris = uris.take(100) // Max 100
            previewIndex = 0
            previewUri = uris.first()
            previewCache.clear()
        }
    }

    // Generate preview when selection changes
    LaunchedEffect(previewUri, selectedTemplate, selectedFont) {
        previewUri?.let { uri ->
            val cacheKey = "${uri}_${selectedTemplate}_${selectedFont}"
            previewCache[cacheKey]?.let {
                previewBitmap = it
                return@LaunchedEffect
            }

            isPreviewLoading = true
            withContext(Dispatchers.IO) {
                try {
                    val outputFile = File(
                        context.cacheDir,
                        "preview_${System.currentTimeMillis()}.jpg"
                    )
                    // 预览使用与输出相同的渲染逻辑，但降低分辨率以加速
                    val result = borderEngine.processImage(
                        uri,
                        selectedTemplate,
                        selectedFont,
                        outputFile,
                        maxWidth = 1200  // Preview mode for faster rendering
                    )
                    if (result.isSuccess) {
                        val bitmap = android.graphics.BitmapFactory.decodeFile(outputFile.absolutePath)
                        bitmap?.let {
                            previewCache[cacheKey] = it
                            withContext(Dispatchers.Main) {
                                previewBitmap = it
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    withContext(Dispatchers.Main) {
                        isPreviewLoading = false
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Photo Border App") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Image selection
            Button(
                onClick = { multipleImagePicker.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (selectedImageUris.isNotEmpty())
                        "Selected ${selectedImageUris.size} images (max 100)"
                    else
                        "Select Images"
                )
            }

            // Preview with border effect and swipe
            previewUri?.let { uri ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .pointerInput(selectedImageUris.size) {
                            if (selectedImageUris.size > 1) {
                                detectHorizontalDragGestures { change, dragAmount ->
                                    change.consume()
                                    if (dragAmount < -50) {
                                        // Swipe left -> next
                                        val nextIndex = (previewIndex + 1) % selectedImageUris.size
                                        previewIndex = nextIndex
                                        previewUri = selectedImageUris[nextIndex]
                                    } else if (dragAmount > 50) {
                                        // Swipe right -> previous
                                        val prevIndex = if (previewIndex > 0) previewIndex - 1 else selectedImageUris.size - 1
                                        previewIndex = prevIndex
                                        previewUri = selectedImageUris[prevIndex]
                                    }
                                }
                            }
                        }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (previewBitmap != null) {
                            Image(
                                bitmap = previewBitmap!!.asImageBitmap(),
                                contentDescription = "Preview with border",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = "Original",
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        if (isPreviewLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        // Image counter overlay
                        if (selectedImageUris.size > 1) {
                            Text(
                                text = "${previewIndex + 1} / ${selectedImageUris.size}",
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Swipe hint
                        if (selectedImageUris.size > 1) {
                            Text(
                                text = "← Swipe to switch →",
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Template selection
            OutlinedButton(
                onClick = { showTemplateDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Template: $selectedTemplate")
            }

            // Font selection
            OutlinedButton(
                onClick = { showFontDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                val fontName = fontManager.getFontName(selectedFont) ?: selectedFont
                Text("Font: $fontName")
            }

            // Process buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (selectedImageUris.isNotEmpty()) {
                            scope.launch {
                                isProcessing = true
                                progress = 0
                                total = selectedImageUris.size
                                resultMessage = ""
                                var successCount = 0
                                var failCount = 0

                                selectedImageUris.forEachIndexed { index, uri ->
                                    progress = index + 1
                                    currentProcessingFile = uri.lastPathSegment ?: "Image ${index + 1}"
                                    try {
                                        val outputFile = File(
                                            context.getExternalFilesDir(null),
                                            "bordered_${System.currentTimeMillis()}_${index}.jpg"
                                        )
                                        val result = borderEngine.processImage(
                                            uri,
                                            selectedTemplate,
                                            selectedFont,
                                            outputFile,
                                            maxWidth = 0  // Full resolution for final output
                                        )
                                        if (result.isSuccess) {
                                            // Save to public gallery
                                            saveToGallery(context, outputFile)
                                            successCount++
                                        } else {
                                            failCount++
                                        }
                                    } catch (e: Exception) {
                                        failCount++
                                    }
                                }
                                currentProcessingFile = ""

                                resultMessage = if (failCount == 0) {
                                    "Success! Processed $successCount images. Saved to Pictures/PhotoBorderApp/"
                                } else {
                                    "Done! $successCount success, $failCount failed"
                                }
                                isProcessing = false
                            }
                        }
                    },
                    enabled = selectedImageUris.isNotEmpty() && !isProcessing,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Batch Process")
                    }
                }

                Button(
                    onClick = { showBatchDialog = true },
                    enabled = selectedImageUris.isNotEmpty() && !isProcessing,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("View List")
                }
            }

            // Progress
            if (isProcessing) {
                LinearProgressIndicator(
                    progress = { progress.toFloat() / total.toFloat() },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Processing $progress / $total")
                if (currentProcessingFile.isNotEmpty()) {
                    Text(
                        text = currentProcessingFile,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Result
            if (resultMessage.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (resultMessage.startsWith("Success"))
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = resultMessage,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    // Template dialog
    if (showTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            title = { Text("Select Template") },
            text = {
                Column {
                    templateManager.listTemplates().forEach { template ->
                        TextButton(
                            onClick = {
                                selectedTemplate = template
                                showTemplateDialog = false
                            }
                        ) {
                            Text(template)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTemplateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Font dialog with preview
    if (showFontDialog) {
        val fonts = fontManager.listFonts()
        AlertDialog(
            onDismissRequest = { showFontDialog = false },
            title = { Text("Select Font") },
            text = {
                LazyColumn(modifier = Modifier.height(400.dp)) {
                    items(fonts) { font ->
                        val isSelected = font.id == selectedFont
                        val typeface = remember(font.id) {
                            try {
                                Typeface.createFromAsset(context.assets, "fonts/${font.file}")
                            } catch (e: Exception) {
                                Typeface.DEFAULT
                            }
                        }
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = font.name,
                                    fontFamily = FontFamily(typeface),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = "${font.family} - ${font.style}",
                                    fontFamily = FontFamily(typeface),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            leadingContent = {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedFont = font.id
                                        showFontDialog = false
                                    }
                                )
                            },
                            modifier = Modifier.clickable {
                                selectedFont = font.id
                                showFontDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFontDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Batch list dialog - show thumbnails
    if (showBatchDialog) {
        AlertDialog(
            onDismissRequest = { showBatchDialog = false },
            title = { Text("Selected Images (${selectedImageUris.size})") },
            text = {
                LazyColumn(modifier = Modifier.height(400.dp)) {
                    items(selectedImageUris) { uri ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(uri),
                                    contentDescription = null,
                                    modifier = Modifier.size(60.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = uri.lastPathSegment ?: "Unknown",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBatchDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

private fun saveToGallery(context: android.content.Context, file: File) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PhotoBorderApp")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let { imageUri ->
                context.contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                    FileInputStream(file).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(imageUri, values, null, null)
            }
        } else {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(picturesDir, "PhotoBorderApp").apply { mkdirs() }
            val destFile = File(appDir, file.name)
            file.copyTo(destFile, overwrite = true)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
