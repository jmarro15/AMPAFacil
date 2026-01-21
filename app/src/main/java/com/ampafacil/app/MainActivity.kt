// File: app/src/main/java/com/ampafacil/app/MainActivity.kt
package com.ampafacil.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ampafacil.app.navigation.AppNavGraph
import com.ampafacil.app.ui.theme.AMPAFacilTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AMPAFacilTheme {
                AppNavGraph()
            }
        }
    }
}
