package com.example.pdfconverter.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pdfconverter.screens.MainScreen
import com.example.pdfconverter.screens.PdfMergingScreen
import com.example.pdfconverter.screens.PdfPickerScreen
import com.example.pdfconverter.screens.PdfSplitScreen
import com.example.pdfconverter.screens.PdfViewerScreen
import com.example.pdfconverter.viewmodels.PdfConverterViewModel
import androidx.core.net.toUri
import com.example.pdfconverter.viewmodels.StateViewModel


@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun MyAppNavigation( modifier : Modifier, pdfConverterViewModel: PdfConverterViewModel,stateViewModel: StateViewModel){

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "mainScreen" ) {


        composable("mainScreen"){

            MainScreen(navController)
        }

        composable("pdfConverterScreen"){

            PdfPickerScreen(navController,pdfConverterViewModel,stateViewModel)
        }

        composable("pdfMergingScreen"){

            PdfMergingScreen(navController, pdfConverterViewModel, stateViewModel)
        }

        composable("pdfSplitScreen"){

            PdfSplitScreen(navController, pdfConverterViewModel ,stateViewModel)
        }


        composable("view_pdf?uri={uri}") { backStackEntry ->
            val uriString = backStackEntry.arguments?.getString("uri")
            uriString?.let {
                PdfViewerScreen(it.toUri(), navController)
            }
        }

    }



}