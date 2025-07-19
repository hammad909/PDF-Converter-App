package com.example.pdfconverter.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.NavController
import com.example.pdfconverter.viewmodels.PdfConverterViewModel
import com.example.pdfconverter.viewmodels.StateViewModel
import kotlinx.coroutines.delay

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPickerScreen(
    navController: NavController,
    pdfConverterViewModel: PdfConverterViewModel,
    stateViewModel: StateViewModel
) {
    val context = LocalContext.current
    val formats = listOf("Text (.txt)", "Word (.docx)", "PowerPoint (.pptx)", "RTF (.rtf)", "DOC (.doc)", "HTML (.html)")

    val pdfUri by stateViewModel.selectedPdfUri.collectAsState()
    val fileName by stateViewModel.selectedPdfFileName.collectAsState()

    var selectedFormat by remember { mutableStateOf(formats[0]) }
    var expanded by remember { mutableStateOf(false) }
    var convertedMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var isConverting by remember { mutableStateOf(false) }
    var triggerConversionDone by remember { mutableStateOf(false) }

    BackHandler {
        stateViewModel.clearSelectedPdf()
        navController.navigate("mainScreen")
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                DocumentFile.fromSingleUri(context, it)?.name?.let { it1 ->
                    stateViewModel.setSelectedPdf(it,
                        it1
                    )
                }
            }
        }
    )


    if (showSuccessMessage) {
        LaunchedEffect(showSuccessMessage) {
            delay(4000)
            showSuccessMessage = false
            convertedMessage = null
        }
    }

    if (triggerConversionDone) {
        LaunchedEffect(triggerConversionDone) {
            delay(3000)
            isConverting = false
            convertedMessage = "Converted to $selectedFormat\nSaved as: ${fileName?.substringBeforeLast('.') ?: "output"}"
            showSuccessMessage = true
            triggerConversionDone = false
        }
    }

    val backgroundColor = Color(0xFFF5F5F5)
    val buttonColor = Color(0xFFD32F2F)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Select & Convert PDF",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("mainScreen")
                        stateViewModel.clearSelectedPdf()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = buttonColor)
            )
        },
        containerColor = backgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { launcher.launch(arrayOf("application/pdf")) },
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Choose PDF", color = Color.White)
            }

            Spacer(modifier = Modifier.height(24.dp))

            fileName?.let {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .clickable {
                            pdfUri?.let { uri ->
                                val encoded = Uri.encode(uri.toString())
                                navController.navigate("view_pdf?uri=$encoded")
                            }
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "PDF File",
                        tint = buttonColor,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Selected File:", color = Color.Black, fontWeight = FontWeight.SemiBold)
                        Text(it, color = Color.Black)
                    }
                    IconButton(onClick = {
                        convertedMessage = null
                        showSuccessMessage = false
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove PDF",
                            tint = Color.Red
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                TextField(
                    value = selectedFormat,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Convert to", color = Color.Black) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = ExposedDropdownMenuDefaults.textFieldColors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    formats.forEach { format ->
                        DropdownMenuItem(
                            text = { Text(format, color = Color.White) },
                            onClick = {
                                selectedFormat = format
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isConverting) {
                CircularProgressIndicator(color = buttonColor)
            } else {
                Button(
                    onClick = {
                        val baseName = fileName?.substringBeforeLast('.') ?: "output"
                        if (pdfUri != null) {
                            isConverting = true
                            when (selectedFormat) {
                                "Text (.txt)" -> pdfConverterViewModel.convertPdfToText(context, pdfUri!!, baseName)
                                "Word (.docx)" -> pdfConverterViewModel.convertPdfToWord(context, pdfUri!!, baseName)
                                "PowerPoint (.pptx)" -> pdfConverterViewModel.convertPdfToPpt(context, pdfUri!!, baseName)
                                "RTF (.rtf)" -> pdfConverterViewModel.convertPdfToRtf(context, pdfUri!!, baseName)
                                "DOC (.doc)" -> pdfConverterViewModel.convertPdfToDoc(context, pdfUri!!, baseName)
                                "HTML (.html)" -> pdfConverterViewModel.convertPdfToHtml(context, pdfUri!!, baseName)
                            }
                            triggerConversionDone = true
                        }
                    },
                    enabled = pdfUri != null,
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Convert", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            convertedMessage?.let { message ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "✅ Conversion Successful!",
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = message, color = Color.Black)
                    }
                }
            }
        }
    }
}
