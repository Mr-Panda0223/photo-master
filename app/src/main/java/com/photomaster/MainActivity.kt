package com.photomaster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.photomaster.domain.manager.LanguageManager
import com.photomaster.domain.manager.ProvideLanguageManager
import com.photomaster.domain.manager.ProvideThemeManager
import com.photomaster.domain.manager.ThemeManager
import com.photomaster.navigation.AppNavigation
import com.photomaster.ui.theme.PhotoMasterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeManager = remember { ThemeManager(this) }
            val languageManager = remember { LanguageManager(this) }

            val colorScheme = themeManager.getColorScheme()

            ProvideThemeManager(themeManager = themeManager) {
                ProvideLanguageManager(languageManager = languageManager) {
                    MaterialTheme(colorScheme = colorScheme) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            AppNavigation()
                        }
                    }
                }
            }
        }
    }
}
