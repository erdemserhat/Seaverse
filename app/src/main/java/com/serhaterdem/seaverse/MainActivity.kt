package com.serhaterdem.seaverse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.serhaterdem.seaverse.ui.game.SeaverseGameApp
import com.serhaterdem.seaverse.ui.theme.SeaverseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SeaverseTheme(dynamicColor = false) {
                SeaverseGameApp()
            }
        }
    }
}
