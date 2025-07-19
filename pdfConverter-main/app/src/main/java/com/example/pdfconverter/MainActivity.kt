package com.example.pdfconverter

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.pdfconverter.navigation.MyAppNavigation
import com.example.pdfconverter.screens.PdfPickerScreen
import com.example.pdfconverter.ui.theme.PDFConverterTheme
import com.example.pdfconverter.viewmodels.PdfConverterViewModel
import com.example.pdfconverter.viewmodels.StateViewModel

class MainActivity : ComponentActivity() {

    private val pdfConverterViewModel: PdfConverterViewModel by viewModels()
    private val stateViewModel : StateViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PDFConverterTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MyAppNavigation( modifier = Modifier.padding(innerPadding),pdfConverterViewModel,stateViewModel)
                }
            }
        }
    }
}
