package com.omarea.gesture.core.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.omarea.gesture.core.model.Action
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "edge_gesture_config")

data class BasicConfig(
    val hoverTimeMs: Int = 180,
    val vibratorUseSystem: Boolean = true,
    val vibratorTapTimeMs: Int = 10,
    val vibratorHoverTimeMs: Int = 16
)

data class OtherConfig(
    val gameOptimization: Boolean = true,
    val lowPowerMode: Boolean = false,
    val hideStartIcon: Boolean = false,
    val windowWatch: Boolean = true,
    val appSwitchBlacklist: Set<String> = emptySet()
)

data class WhiteBarConfig(
    val enabled: Boolean = true,
    val landscapeEnabled: Boolean = true,
    val portraitEnabled: Boolean = true,
    val widthDp: Float = 140f,
    val heightDp: Float = 4f,
    val touchHeightDp: Float = 24f,
    val touchWidthDp: Float = 200f,
    val bottomMarginDp: Float = 6f,
    val radiusDp: Float = 2f,
    val color: Long = 0xFFFFFFFF,
    val alpha: Float = 0.85f,
    val batteryLevelEnabled: Boolean = false,
    val batterySmoothGradient: Boolean = true,
    val burnInProtection: Boolean = true,
    val clickAction: Action = Action.Home,
    val longPressAction: Action = Action.None,
    val swipeLeftAction: Action = Action.SwitchPreviousApp,
    val swipeRightAction: Action = Action.SwitchPreviousApp,
    val swipeUpAction: Action = Action.Home,
    val swipeUpHoldAction: Action = Action.Recents,
    val hapticsEnabled: Boolean = true,
    val shizukuEnabled: Boolean = false
)

data class SideGestureConfig(
    val leftEnabled: Boolean = true,
    val leftLandscape: Boolean = false,
    val leftPortrait: Boolean = true,
    val leftWidthDp: Float = 18f,
    val leftHeightPercent: Float = 0.65f,
    val leftYOffsetPercent: Float = 0.25f,
    val leftSlideAction: Action = Action.Back,
    val leftHoverAction: Action = Action.Recents,

    val rightEnabled: Boolean = true,
    val rightLandscape: Boolean = false,
    val rightPortrait: Boolean = true,
    val rightWidthDp: Float = 18f,
    val rightHeightPercent: Float = 0.65f,
    val rightYOffsetPercent: Float = 0.25f,
    val rightSlideAction: Action = Action.Back,
    val rightHoverAction: Action = Action.Recents,

    // 底部边缘手势
    val bottomEnabled: Boolean = false,
    val bottomLandscape: Boolean = false,
    val bottomPortrait: Boolean = false,
    val bottomHeightDp: Float = 18f,
    val bottomWidthPercent: Float = 1.0f,
    val bottomSlideAction: Action = Action.Home,
    val bottomHoverAction: Action = Action.Recents,

    // 视觉反馈与热区
    val edgeSideWidthDp: Float = 18f,
    val edgeColor: Long = 0x80E1CBFF
)

class AppConfigRepository(private val context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {

    companion object {
        // 小白条配置键
        private val KEY_WB_ENABLED = booleanPreferencesKey("wb_enabled")
        private val KEY_WB_WIDTH = floatPreferencesKey("wb_width")
        private val KEY_WB_HEIGHT = floatPreferencesKey("wb_height")
        private val KEY_WB_TOUCH_HEIGHT = floatPreferencesKey("wb_touch_height")
        private val KEY_WB_TOUCH_WIDTH = floatPreferencesKey("wb_touch_width")
        private val KEY_WB_BOTTOM_MARGIN = floatPreferencesKey("wb_bottom_margin")
        private val KEY_WB_RADIUS = floatPreferencesKey("wb_radius")
        private val KEY_WB_COLOR = longPreferencesKey("wb_color")
        private val KEY_WB_ALPHA = floatPreferencesKey("wb_alpha")
        private val KEY_WB_BATTERY_LEVEL = booleanPreferencesKey("wb_battery_level")

        private val KEY_WB_CLICK = stringPreferencesKey("wb_click_action")
        private val KEY_WB_LONG_PRESS = stringPreferencesKey("wb_long_press_action")
        private val KEY_WB_SWIPE_LEFT = stringPreferencesKey("wb_swipe_left_action")
        private val KEY_WB_SWIPE_RIGHT = stringPreferencesKey("wb_swipe_right_action")
        private val KEY_WB_SWIPE_UP = stringPreferencesKey("wb_swipe_up_action")
        private val KEY_WB_SWIPE_UP_HOLD = stringPreferencesKey("wb_swipe_up_hold_action")

        private val KEY_HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        private val KEY_SHIZUKU_ENABLED = booleanPreferencesKey("shizuku_enabled")

        private val KEY_WB_LANDSCAPE = booleanPreferencesKey("wb_landscape")
        private val KEY_WB_PORTRAIT = booleanPreferencesKey("wb_portrait")
        private val KEY_WB_BATTERY_SMOOTH = booleanPreferencesKey("wb_battery_smooth_gradient")
        private val KEY_WB_BURN_IN = booleanPreferencesKey("wb_burn_in_protection")

        // 基础配置键
        private val KEY_HOVER_TIME = intPreferencesKey("hover_time_ms")
        private val KEY_VIBRATOR_SYSTEM = booleanPreferencesKey("vibrator_system")
        private val KEY_VIBRATOR_TAP = intPreferencesKey("vibrator_tap_ms")
        private val KEY_VIBRATOR_HOVER = intPreferencesKey("vibrator_hover_ms")

        // 其它配置键
        private val KEY_GAME_OPT = booleanPreferencesKey("game_opt")
        private val KEY_LOW_POWER = booleanPreferencesKey("low_power")
        private val KEY_HIDE_ICON = booleanPreferencesKey("hide_icon")
        private val KEY_WINDOW_WATCH = booleanPreferencesKey("window_watch")
        private val KEY_APP_BLACKLIST = stringSetPreferencesKey("app_blacklist")

        // 边缘手势配置键
        private val KEY_SIDE_LEFT_ENABLED = booleanPreferencesKey("side_left_enabled")
        private val KEY_SIDE_LEFT_LANDSCAPE = booleanPreferencesKey("side_left_landscape")
        private val KEY_SIDE_LEFT_PORTRAIT = booleanPreferencesKey("side_left_portrait")
        private val KEY_SIDE_LEFT_WIDTH = floatPreferencesKey("side_left_width")
        private val KEY_SIDE_LEFT_HEIGHT = floatPreferencesKey("side_left_height")
        private val KEY_SIDE_LEFT_OFFSET = floatPreferencesKey("side_left_offset")
        private val KEY_SIDE_LEFT_SLIDE = stringPreferencesKey("side_left_slide")
        private val KEY_SIDE_LEFT_HOVER = stringPreferencesKey("side_left_hover")

        private val KEY_SIDE_RIGHT_ENABLED = booleanPreferencesKey("side_right_enabled")
        private val KEY_SIDE_RIGHT_LANDSCAPE = booleanPreferencesKey("side_right_landscape")
        private val KEY_SIDE_RIGHT_PORTRAIT = booleanPreferencesKey("side_right_portrait")
        private val KEY_SIDE_RIGHT_WIDTH = floatPreferencesKey("side_right_width")
        private val KEY_SIDE_RIGHT_HEIGHT = floatPreferencesKey("side_right_height")
        private val KEY_SIDE_RIGHT_OFFSET = floatPreferencesKey("side_right_offset")
        private val KEY_SIDE_RIGHT_SLIDE = stringPreferencesKey("side_right_slide")
        private val KEY_SIDE_RIGHT_HOVER = stringPreferencesKey("side_right_hover")

        private val KEY_SIDE_BOTTOM_ENABLED = booleanPreferencesKey("side_bottom_enabled")
        private val KEY_SIDE_BOTTOM_LANDSCAPE = booleanPreferencesKey("side_bottom_landscape")
        private val KEY_SIDE_BOTTOM_PORTRAIT = booleanPreferencesKey("side_bottom_portrait")
        private val KEY_SIDE_BOTTOM_HEIGHT = floatPreferencesKey("side_bottom_height")
        private val KEY_SIDE_BOTTOM_WIDTH = floatPreferencesKey("side_bottom_width")
        private val KEY_SIDE_BOTTOM_SLIDE = stringPreferencesKey("side_bottom_slide")
        private val KEY_SIDE_BOTTOM_HOVER = stringPreferencesKey("side_bottom_hover")

        private val KEY_EDGE_WIDTH = floatPreferencesKey("edge_side_width")
        private val KEY_EDGE_COLOR = longPreferencesKey("edge_color")

        @Volatile
        private var INSTANCE: AppConfigRepository? = null

        fun getInstance(context: Context): AppConfigRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppConfigRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    val basicConfig: StateFlow<BasicConfig> = context.dataStore.data.map { pref ->
        BasicConfig(
            hoverTimeMs = pref[KEY_HOVER_TIME] ?: 180,
            vibratorUseSystem = pref[KEY_VIBRATOR_SYSTEM] ?: true,
            vibratorTapTimeMs = pref[KEY_VIBRATOR_TAP] ?: 10,
            vibratorHoverTimeMs = pref[KEY_VIBRATOR_HOVER] ?: 16
        )
    }.stateIn(scope, SharingStarted.Eagerly, BasicConfig())

    val otherConfig: StateFlow<OtherConfig> = context.dataStore.data.map { pref ->
        OtherConfig(
            gameOptimization = pref[KEY_GAME_OPT] ?: true,
            lowPowerMode = pref[KEY_LOW_POWER] ?: false,
            hideStartIcon = pref[KEY_HIDE_ICON] ?: false,
            windowWatch = pref[KEY_WINDOW_WATCH] ?: true,
            appSwitchBlacklist = pref[KEY_APP_BLACKLIST] ?: emptySet()
        )
    }.stateIn(scope, SharingStarted.Eagerly, OtherConfig())

    val whiteBarConfig: StateFlow<WhiteBarConfig> = context.dataStore.data.map { pref ->
        WhiteBarConfig(
            enabled = pref[KEY_WB_ENABLED] ?: true,
            landscapeEnabled = pref[KEY_WB_LANDSCAPE] ?: true,
            portraitEnabled = pref[KEY_WB_PORTRAIT] ?: true,
            widthDp = pref[KEY_WB_WIDTH] ?: 140f,
            heightDp = pref[KEY_WB_HEIGHT] ?: 4f,
            touchHeightDp = pref[KEY_WB_TOUCH_HEIGHT] ?: 24f,
            touchWidthDp = pref[KEY_WB_TOUCH_WIDTH] ?: 200f,
            bottomMarginDp = pref[KEY_WB_BOTTOM_MARGIN] ?: 6f,
            radiusDp = pref[KEY_WB_RADIUS] ?: 2f,
            color = pref[KEY_WB_COLOR] ?: 0xFFFFFFFF,
            alpha = pref[KEY_WB_ALPHA] ?: 0.85f,
            batteryLevelEnabled = pref[KEY_WB_BATTERY_LEVEL] ?: false,
            batterySmoothGradient = pref[KEY_WB_BATTERY_SMOOTH] ?: true,
            burnInProtection = pref[KEY_WB_BURN_IN] ?: true,
            clickAction = Action.fromString(pref[KEY_WB_CLICK] ?: Action.toString(Action.Home)),
            longPressAction = Action.fromString(pref[KEY_WB_LONG_PRESS] ?: Action.toString(Action.None)),
            swipeLeftAction = Action.fromString(pref[KEY_WB_SWIPE_LEFT] ?: Action.toString(Action.SwitchPreviousApp)),
            swipeRightAction = Action.fromString(pref[KEY_WB_SWIPE_RIGHT] ?: Action.toString(Action.SwitchPreviousApp)),
            swipeUpAction = Action.fromString(pref[KEY_WB_SWIPE_UP] ?: Action.toString(Action.Home)),
            swipeUpHoldAction = Action.fromString(pref[KEY_WB_SWIPE_UP_HOLD] ?: Action.toString(Action.Recents)),
            hapticsEnabled = pref[KEY_HAPTICS_ENABLED] ?: true,
            shizukuEnabled = pref[KEY_SHIZUKU_ENABLED] ?: false
        )
    }.stateIn(scope, SharingStarted.Eagerly, WhiteBarConfig())

    val sideGestureConfig: StateFlow<SideGestureConfig> = context.dataStore.data.map { pref ->
        SideGestureConfig(
            leftEnabled = pref[KEY_SIDE_LEFT_ENABLED] ?: true,
            leftLandscape = pref[KEY_SIDE_LEFT_LANDSCAPE] ?: false,
            leftPortrait = pref[KEY_SIDE_LEFT_PORTRAIT] ?: true,
            leftWidthDp = pref[KEY_SIDE_LEFT_WIDTH] ?: 18f,
            leftHeightPercent = pref[KEY_SIDE_LEFT_HEIGHT] ?: 0.65f,
            leftYOffsetPercent = pref[KEY_SIDE_LEFT_OFFSET] ?: 0.25f,
            leftSlideAction = Action.fromString(pref[KEY_SIDE_LEFT_SLIDE] ?: Action.toString(Action.Back)),
            leftHoverAction = Action.fromString(pref[KEY_SIDE_LEFT_HOVER] ?: Action.toString(Action.Recents)),

            rightEnabled = pref[KEY_SIDE_RIGHT_ENABLED] ?: true,
            rightLandscape = pref[KEY_SIDE_RIGHT_LANDSCAPE] ?: false,
            rightPortrait = pref[KEY_SIDE_RIGHT_PORTRAIT] ?: true,
            rightWidthDp = pref[KEY_SIDE_RIGHT_WIDTH] ?: 18f,
            rightHeightPercent = pref[KEY_SIDE_RIGHT_HEIGHT] ?: 0.65f,
            rightYOffsetPercent = pref[KEY_SIDE_RIGHT_OFFSET] ?: 0.25f,
            rightSlideAction = Action.fromString(pref[KEY_SIDE_RIGHT_SLIDE] ?: Action.toString(Action.Back)),
            rightHoverAction = Action.fromString(pref[KEY_SIDE_RIGHT_HOVER] ?: Action.toString(Action.Recents)),

            bottomEnabled = pref[KEY_SIDE_BOTTOM_ENABLED] ?: false,
            bottomLandscape = pref[KEY_SIDE_BOTTOM_LANDSCAPE] ?: false,
            bottomPortrait = pref[KEY_SIDE_BOTTOM_PORTRAIT] ?: false,
            bottomHeightDp = pref[KEY_SIDE_BOTTOM_HEIGHT] ?: 18f,
            bottomWidthPercent = pref[KEY_SIDE_BOTTOM_WIDTH] ?: 1.0f,
            bottomSlideAction = Action.fromString(pref[KEY_SIDE_BOTTOM_SLIDE] ?: Action.toString(Action.Home)),
            bottomHoverAction = Action.fromString(pref[KEY_SIDE_BOTTOM_HOVER] ?: Action.toString(Action.Recents)),

            edgeSideWidthDp = pref[KEY_EDGE_WIDTH] ?: 18f,
            edgeColor = pref[KEY_EDGE_COLOR] ?: 0x80E1CBFF
        )
    }.stateIn(scope, SharingStarted.Eagerly, SideGestureConfig())

    suspend fun updateWhiteBarEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_WB_ENABLED] = enabled }
    }

    suspend fun updateWhiteBarDimensions(
        widthDp: Float,
        heightDp: Float,
        touchHeightDp: Float,
        touchWidthDp: Float,
        bottomMarginDp: Float,
        radiusDp: Float
    ) {
        context.dataStore.edit {
            it[KEY_WB_WIDTH] = widthDp
            it[KEY_WB_HEIGHT] = heightDp
            it[KEY_WB_TOUCH_HEIGHT] = touchHeightDp
            it[KEY_WB_TOUCH_WIDTH] = touchWidthDp
            it[KEY_WB_BOTTOM_MARGIN] = bottomMarginDp
            it[KEY_WB_RADIUS] = radiusDp
        }
    }

    suspend fun updateWhiteBarBurnInProtection(enabled: Boolean) {
        context.dataStore.edit {
            it[KEY_WB_BURN_IN] = enabled
        }
    }

    suspend fun updateWhiteBarAppearance(color: Long, alpha: Float) {
        context.dataStore.edit {
            it[KEY_WB_COLOR] = color
            it[KEY_WB_ALPHA] = alpha
        }
    }

    suspend fun updateWhiteBarBatteryLevel(enabled: Boolean) {
        context.dataStore.edit {
            it[KEY_WB_BATTERY_LEVEL] = enabled
        }
    }

    suspend fun updateWhiteBarBatterySmoothGradient(enabled: Boolean) {
        context.dataStore.edit {
            it[KEY_WB_BATTERY_SMOOTH] = enabled
        }
    }

    suspend fun updateClickAction(action: Action) {
        context.dataStore.edit { it[KEY_WB_CLICK] = Action.toString(action) }
    }

    suspend fun updateLongPressAction(action: Action) {
        context.dataStore.edit { it[KEY_WB_LONG_PRESS] = Action.toString(action) }
    }

    suspend fun updateSwipeLeftAction(action: Action) {
        context.dataStore.edit { it[KEY_WB_SWIPE_LEFT] = Action.toString(action) }
    }

    suspend fun updateSwipeRightAction(action: Action) {
        context.dataStore.edit { it[KEY_WB_SWIPE_RIGHT] = Action.toString(action) }
    }

    suspend fun updateSwipeUpAction(action: Action) {
        context.dataStore.edit { it[KEY_WB_SWIPE_UP] = Action.toString(action) }
    }

    suspend fun updateSwipeUpHoldAction(action: Action) {
        context.dataStore.edit { it[KEY_WB_SWIPE_UP_HOLD] = Action.toString(action) }
    }

    suspend fun updateHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_HAPTICS_ENABLED] = enabled }
    }

    suspend fun updateShizukuEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SHIZUKU_ENABLED] = enabled }
    }

    suspend fun updateSideLeftEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SIDE_LEFT_ENABLED] = enabled }
    }

    suspend fun updateSideRightEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SIDE_RIGHT_ENABLED] = enabled }
    }

    suspend fun updateSideBottomEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SIDE_BOTTOM_ENABLED] = enabled }
    }

    suspend fun updateSideBottomDimensions(heightDp: Float, widthPercent: Float) {
        context.dataStore.edit {
            it[KEY_SIDE_BOTTOM_HEIGHT] = heightDp
            it[KEY_SIDE_BOTTOM_WIDTH] = widthPercent
        }
    }

    suspend fun updateSideBottomActions(slideAction: Action, hoverAction: Action) {
        context.dataStore.edit {
            it[KEY_SIDE_BOTTOM_SLIDE] = Action.toString(slideAction)
            it[KEY_SIDE_BOTTOM_HOVER] = Action.toString(hoverAction)
        }
    }

    suspend fun updateSideLeftDimensions(widthDp: Float, heightPercent: Float, yOffsetPercent: Float) {
        context.dataStore.edit {
            it[KEY_SIDE_LEFT_WIDTH] = widthDp
            it[KEY_SIDE_LEFT_HEIGHT] = heightPercent
            it[KEY_SIDE_LEFT_OFFSET] = yOffsetPercent
        }
    }

    suspend fun updateSideRightDimensions(widthDp: Float, heightPercent: Float, yOffsetPercent: Float) {
        context.dataStore.edit {
            it[KEY_SIDE_RIGHT_WIDTH] = widthDp
            it[KEY_SIDE_RIGHT_HEIGHT] = heightPercent
            it[KEY_SIDE_RIGHT_OFFSET] = yOffsetPercent
        }
    }

    suspend fun updateSideLeftActions(slideAction: Action, hoverAction: Action) {
        context.dataStore.edit {
            it[KEY_SIDE_LEFT_SLIDE] = Action.toString(slideAction)
            it[KEY_SIDE_LEFT_HOVER] = Action.toString(hoverAction)
        }
    }

    suspend fun updateSideRightActions(slideAction: Action, hoverAction: Action) {
        context.dataStore.edit {
            it[KEY_SIDE_RIGHT_SLIDE] = Action.toString(slideAction)
            it[KEY_SIDE_RIGHT_HOVER] = Action.toString(hoverAction)
        }
    }

    suspend fun updateHoverTime(timeMs: Int) {
        context.dataStore.edit { it[KEY_HOVER_TIME] = timeMs }
    }

    suspend fun updateVibratorUseSystem(useSystem: Boolean) {
        context.dataStore.edit { it[KEY_VIBRATOR_SYSTEM] = useSystem }
    }

    suspend fun updateVibratorCustom(tapMs: Int, hoverMs: Int) {
        context.dataStore.edit {
            it[KEY_VIBRATOR_TAP] = tapMs
            it[KEY_VIBRATOR_HOVER] = hoverMs
        }
    }

    suspend fun updateGameOptimization(enabled: Boolean) {
        context.dataStore.edit { it[KEY_GAME_OPT] = enabled }
    }

    suspend fun updateLowPowerMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_LOW_POWER] = enabled }
    }

    suspend fun updateHideStartIcon(enabled: Boolean) {
        context.dataStore.edit { it[KEY_HIDE_ICON] = enabled }
    }

    suspend fun updateWindowWatch(enabled: Boolean) {
        context.dataStore.edit { it[KEY_WINDOW_WATCH] = enabled }
    }

    suspend fun updateAppBlacklist(blacklist: Set<String>) {
        context.dataStore.edit { it[KEY_APP_BLACKLIST] = blacklist }
    }

    suspend fun updateWhiteBarOrientation(landscape: Boolean, portrait: Boolean) {
        context.dataStore.edit {
            it[KEY_WB_LANDSCAPE] = landscape
            it[KEY_WB_PORTRAIT] = portrait
        }
    }

    suspend fun updateSideLeftOrientation(landscape: Boolean, portrait: Boolean) {
        context.dataStore.edit {
            it[KEY_SIDE_LEFT_LANDSCAPE] = landscape
            it[KEY_SIDE_LEFT_PORTRAIT] = portrait
        }
    }

    suspend fun updateSideRightOrientation(landscape: Boolean, portrait: Boolean) {
        context.dataStore.edit {
            it[KEY_SIDE_RIGHT_LANDSCAPE] = landscape
            it[KEY_SIDE_RIGHT_PORTRAIT] = portrait
        }
    }

    suspend fun updateSideBottomOrientation(landscape: Boolean, portrait: Boolean) {
        context.dataStore.edit {
            it[KEY_SIDE_BOTTOM_LANDSCAPE] = landscape
            it[KEY_SIDE_BOTTOM_PORTRAIT] = portrait
        }
    }

    suspend fun updateEdgeSideWidth(widthDp: Float) {
        context.dataStore.edit { it[KEY_EDGE_WIDTH] = widthDp }
    }

    suspend fun updateEdgeColor(color: Long) {
        context.dataStore.edit { it[KEY_EDGE_COLOR] = color }
    }
}
