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

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfMergingScreen(
    navController: NavController,
    pdfConverterViewModel: PdfConverterViewModel,
    stateViewModel: StateViewModel
) {
    val context = LocalContext.current
    val selectedUris by stateViewModel.selectedPdfUris.collectAsState()

    val mergedPdfUri by pdfConverterViewModel.mergedPdfUri.collectAsState()
    var outputFileName by remember { mutableStateOf("merged_output.pdf") }
    val loading by pdfConverterViewModel.isMerging.collectAsState()

    val backgroundColor = Color(0xFFF5F5F5)
    val buttonColor = Color(0xFFD32F2F)

    BackHandler {
        stateViewModel.clearSelectedPdfUris()
        navController.navigate("mainScreen")
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            uris.forEach { uri ->
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            stateViewModel.addPdfUris(uris)

        }
    )


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Merge PDFs",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    }
                },  navigationIcon = {
                    IconButton(onClick = {
                        stateViewModel.clearSelectedPdfUris()
                        navController.navigate("mainScreen")
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

            Text(
                text = "Pick two or more PDF files to merge",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { launcher.launch(arrayOf("application/pdf")) },
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select PDFs", color = Color.White)
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (selectedUris.isNotEmpty()) {
                Text(
                    text = "Selected: ${selectedUris.size} PDFs",
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    selectedUris.forEach { uri ->
                        val name = DocumentFile.fromSingleUri(context, uri)?.name ?: "Unknown File"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    val encodedUri = Uri.encode(uri.toString())
                                    navController.navigate("view_pdf?uri=$encodedUri")
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = Color.Red)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(name, modifier = Modifier.weight(1f), color = Color.Black)
                            IconButton(onClick = {
                                stateViewModel.removePdfUri(uri)
                            }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.Red)
                            }
                        }
                    }


                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = outputFileName,
                    onValueChange = { outputFileName = it },
                    label = { Text("Output File Name", color = Color.Black) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedLabelColor = Color.Black,
                        unfocusedLabelColor = Color.Black,
                        cursorColor = Color.Black,
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Conditional buttons based on selection count
                if (selectedUris.size == 1) {
                    Button(
                        onClick = { launcher.launch(arrayOf("application/pdf")) },
                        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Pick Another PDF", color = Color.White)
                    }
                } else if (selectedUris.size >= 2) {
                    Button(
                        onClick = {
                            pdfConverterViewModel.mergeSelectedPdfs(context, selectedUris, outputFileName)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Merge PDFs", color = Color.White)
                    }
                }
            }

            if (mergedPdfUri != null) {
                val mergedFileName = DocumentFile.fromSingleUri(context, mergedPdfUri!!)?.name ?: "Merged PDF"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Merged PDF",
                            tint = Color.Red
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = mergedFileName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                    }
                }
            }


            if (loading) {
                CircularProgressIndicator(
                    color = buttonColor,
                    modifier = Modifier
                        .padding(16.dp)
                        .size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }



        }
    }
}
