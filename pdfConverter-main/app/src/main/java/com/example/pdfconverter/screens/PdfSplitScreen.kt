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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import com.example.pdfconverter.viewmodels.StateViewModel

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfSplitScreen(
    navController: NavController,
    pdfConverterViewModel: PdfConverterViewModel,
    stateViewModel: StateViewModel
) {

    val context = LocalContext.current
    val selectedUri by stateViewModel.selectedPdfUri.collectAsState()
    val fileName by stateViewModel.selectedPdfFileName.collectAsState()
    val splitFiles by pdfConverterViewModel.splitResultFiles.collectAsState()
    val isLoading by pdfConverterViewModel.isSplitting.collectAsState()

    BackHandler {
        stateViewModel.clearSelectedPdf()
        navController.navigate("mainScreen")
    }

    DisposableEffect(Unit) {
        onDispose {
            pdfConverterViewModel.clearSplitResult()
        }
    }

    val backgroundColor = Color(0xFFF5F5F5)
    val buttonColor = Color(0xFFD32F2F)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                DocumentFile.fromSingleUri(context, it)?.name?.let { name ->
                    stateViewModel.setSelectedPdf(it, name)
                }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Split PDF",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    }
                }, navigationIcon = {
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
            Text(
                text = "Select a PDF file to split into separate pages",
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
                Text("Choose PDF", color = Color.White)
            }

            Spacer(modifier = Modifier.height(24.dp))

            fileName?.let {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = buttonColor,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = it,
                        color = Color.Black,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedUri?.let { uri ->
                                    navController.navigate("view_pdf?uri=${Uri.encode(uri.toString())}")
                                }
                            }
                    )


                    IconButton(onClick = {
                            stateViewModel.clearSelectedPdf()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove selected PDF",
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        selectedUri?.let { uri ->
                            val safeName = fileName?.substringBeforeLast(".") ?: "split_output"
                            pdfConverterViewModel.splitPdfIntoPages(context, uri, safeName)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Split PDF", color = Color.White)
                }
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(color = buttonColor)
            }

            if (splitFiles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Split Files",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.Black
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Divider(color = Color.LightGray)

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 200.dp)
                        ) {
                            items(splitFiles) { fileName ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = Color(0xFF616161),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = fileName,
                                        fontSize = 14.sp,
                                        color = Color.DarkGray,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }



        }
    }
}
