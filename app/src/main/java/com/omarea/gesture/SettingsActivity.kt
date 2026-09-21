package com.omarea.gesture

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.omarea.gesture.core.config.AppConfigRepository
import com.omarea.gesture.core.shizuku.ShizukuManager
import com.omarea.gesture.ui.settings.screens.MainSettingsScreen
import com.omarea.gesture.ui.settings.theme.EdgeGestureTheme

open class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val configRepository = AppConfigRepository.getInstance(this)
        val shizukuManager = ShizukuManager.getInstance(this)

        setContent {
            EdgeGestureTheme {
                MainSettingsScreen(
                    configRepository = configRepository,
                    shizukuManager = shizukuManager
                )
            }
        }
    }
}
