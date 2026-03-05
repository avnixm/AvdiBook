package com.avnixm.avdibook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.avnixm.avdibook.ui.navigation.AvdiBookApp
import com.avnixm.avdibook.ui.theme.AvdiBookTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AvdiBookTheme {
                AvdiBookApp()
            }
        }
    }
}
