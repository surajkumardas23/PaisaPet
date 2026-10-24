package com.example.paisapet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.example.paisapet.debugtools.DebugTools
import com.example.paisapet.ui.theme.PaisaPetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as PaisaPetApp).container
        setContent {
            PaisaPetTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Debug builds show Simulate SMS for now; later phases replace this with Home.
                    if (DebugTools.isAvailable) {
                        DebugTools.Screen(container, Modifier.padding(innerPadding))
                    } else {
                        Text("Paisa Pet", modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}
