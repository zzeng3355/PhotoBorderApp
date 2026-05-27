package com.zzeng.photoborder.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.zzeng.photoborder.engine.BorderEngine
import com.zzeng.photoborder.engine.FontManager
import com.zzeng.photoborder.engine.TemplateManager
import androidx.compose.material3.ListItem
import kotlinx.coroutines.launch
import java.io.File

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
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showBatchDialog by remember { mutableStateOf(false) }
    var previewUri by remember { mutableStateOf<Uri?>(null) }

    // Multiple image picker
    val multipleImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedImageUris = uris.take(100) // Max 100
            previewUri = uris.first()
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

            // Preview
            previewUri?.let { uri ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(uri),
                        contentDescription = "Preview",
                        modifier = Modifier.fillMaxSize()
                    )
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

                                selectedImageUris.forEachIndexed { index, uri ->
                                    progress = index + 1
                                    val outputFile = File(
                                        context.getExternalFilesDir(null),
                                        "bordered_${System.currentTimeMillis()}_${index}.jpg"
                                    )
                                    borderEngine.processImage(uri, selectedTemplate, outputFile)
                                }

                                resultMessage = "Success! Processed $total images"
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
                    progress = { progress.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Processing $progress / $total")
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

    // Font dialog
    if (showFontDialog) {
        val fonts = fontManager.listFonts()
        AlertDialog(
            onDismissRequest = { showFontDialog = false },
            title = { Text("Select Font") },
            text = {
                LazyColumn(modifier = Modifier.height(400.dp)) {
                    items(fonts) { font ->
                        val isSelected = font.id == selectedFont
                        ListItem(
                            headlineContent = { Text(font.name) },
                            supportingContent = { Text("${font.family} - ${font.style}") },
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

    // Batch list dialog
    if (showBatchDialog) {
        AlertDialog(
            onDismissRequest = { showBatchDialog = false },
            title = { Text("Selected Images (${selectedImageUris.size})") },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(selectedImageUris) { uri ->
                        Text(
                            text = uri.lastPathSegment ?: "Unknown",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(4.dp)
                        )
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
