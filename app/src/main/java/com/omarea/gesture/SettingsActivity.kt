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

    override fun onResume() {
        super.onResume()
        com.omarea.gesture.util.GlobalState.testMode = true
        com.omarea.gesture.ui.gesture.ModernSideGestureBar.instance?.refreshTestMode()
        com.omarea.gesture.ui.whitebar.ModernWhiteBar.instance?.refreshTestMode()
        notifyConfigChanged()
    }

    override fun onPause() {
        super.onPause()
        com.omarea.gesture.util.GlobalState.testMode = false
        com.omarea.gesture.ui.gesture.ModernSideGestureBar.instance?.refreshTestMode()
        com.omarea.gesture.ui.whitebar.ModernWhiteBar.instance?.refreshTestMode()
        notifyConfigChanged()
    }

    private fun notifyConfigChanged() {
        try {
            val intent = android.content.Intent(getString(R.string.action_config_changed)).apply {
                setPackage(packageName)
            }
            sendBroadcast(intent)
        } catch (_: Exception) {
        }
    }
}
