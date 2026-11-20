package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.lzacademy.app.LZApp
import com.lzacademy.app.data.preferences.AppPreferences
import com.lzacademy.app.theme.LZTheme
import com.lzacademy.app.theme.ThemeMode
import com.lzacademy.app.theme.isDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val preferences = remember(context) { AppPreferences(context) }
            // Light is the product default; the choice made in "Mon compte" is persisted.
            var themeMode by rememberSaveable { mutableStateOf(preferences.themeMode) }
            LZTheme(darkTheme = themeMode.isDark()) {
                LZApp(
                    themeMode = themeMode,
                    onThemeModeChange = {
                        themeMode = it
                        preferences.themeMode = it
                    },
                )
            }
        }
    }
}
