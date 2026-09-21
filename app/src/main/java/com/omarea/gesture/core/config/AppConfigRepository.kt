package com.omarea.gesture.core.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.omarea.gesture.core.model.Action
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "edge_gesture_config")

data class WhiteBarConfig(
    val enabled: Boolean = true,
    val widthDp: Float = 140f,
    val heightDp: Float = 4f,
    val bottomMarginDp: Float = 6f,
    val radiusDp: Float = 2f,
    val color: Long = 0xFFFFFFFF,
    val alpha: Float = 0.85f,
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
    val leftEnabled: Boolean = false,
    val leftWidthDp: Float = 18f,
    val leftHeightPercent: Float = 0.5f,
    val leftYOffsetPercent: Float = 0.25f,
    val leftSlideAction: Action = Action.Back,
    val leftHoverAction: Action = Action.SwitchPreviousApp,

    val rightEnabled: Boolean = false,
    val rightWidthDp: Float = 18f,
    val rightHeightPercent: Float = 0.5f,
    val rightYOffsetPercent: Float = 0.25f,
    val rightSlideAction: Action = Action.Back,
    val rightHoverAction: Action = Action.SwitchPreviousApp
)

class AppConfigRepository(private val context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {

    companion object {
        // 小白条配置键
        private val KEY_WB_ENABLED = booleanPreferencesKey("wb_enabled")
        private val KEY_WB_WIDTH = floatPreferencesKey("wb_width")
        private val KEY_WB_HEIGHT = floatPreferencesKey("wb_height")
        private val KEY_WB_BOTTOM_MARGIN = floatPreferencesKey("wb_bottom_margin")
        private val KEY_WB_RADIUS = floatPreferencesKey("wb_radius")
        private val KEY_WB_COLOR = longPreferencesKey("wb_color")
        private val KEY_WB_ALPHA = floatPreferencesKey("wb_alpha")

        private val KEY_WB_CLICK = stringPreferencesKey("wb_click_action")
        private val KEY_WB_LONG_PRESS = stringPreferencesKey("wb_long_press_action")
        private val KEY_WB_SWIPE_LEFT = stringPreferencesKey("wb_swipe_left_action")
        private val KEY_WB_SWIPE_RIGHT = stringPreferencesKey("wb_swipe_right_action")
        private val KEY_WB_SWIPE_UP = stringPreferencesKey("wb_swipe_up_action")
        private val KEY_WB_SWIPE_UP_HOLD = stringPreferencesKey("wb_swipe_up_hold_action")

        private val KEY_HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        private val KEY_SHIZUKU_ENABLED = booleanPreferencesKey("shizuku_enabled")

        // 边缘手势配置键
        private val KEY_SIDE_LEFT_ENABLED = booleanPreferencesKey("side_left_enabled")
        private val KEY_SIDE_LEFT_WIDTH = floatPreferencesKey("side_left_width")
        private val KEY_SIDE_LEFT_HEIGHT = floatPreferencesKey("side_left_height")
        private val KEY_SIDE_LEFT_OFFSET = floatPreferencesKey("side_left_offset")
        private val KEY_SIDE_LEFT_SLIDE = stringPreferencesKey("side_left_slide")
        private val KEY_SIDE_LEFT_HOVER = stringPreferencesKey("side_left_hover")

        private val KEY_SIDE_RIGHT_ENABLED = booleanPreferencesKey("side_right_enabled")
        private val KEY_SIDE_RIGHT_WIDTH = floatPreferencesKey("side_right_width")
        private val KEY_SIDE_RIGHT_HEIGHT = floatPreferencesKey("side_right_height")
        private val KEY_SIDE_RIGHT_OFFSET = floatPreferencesKey("side_right_offset")
        private val KEY_SIDE_RIGHT_SLIDE = stringPreferencesKey("side_right_slide")
        private val KEY_SIDE_RIGHT_HOVER = stringPreferencesKey("side_right_hover")

        @Volatile
        private var INSTANCE: AppConfigRepository? = null

        fun getInstance(context: Context): AppConfigRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppConfigRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    val whiteBarConfig: StateFlow<WhiteBarConfig> = context.dataStore.data.map { pref ->
        WhiteBarConfig(
            enabled = pref[KEY_WB_ENABLED] ?: true,
            widthDp = pref[KEY_WB_WIDTH] ?: 140f,
            heightDp = pref[KEY_WB_HEIGHT] ?: 4f,
            bottomMarginDp = pref[KEY_WB_BOTTOM_MARGIN] ?: 6f,
            radiusDp = pref[KEY_WB_RADIUS] ?: 2f,
            color = pref[KEY_WB_COLOR] ?: 0xFFFFFFFF,
            alpha = pref[KEY_WB_ALPHA] ?: 0.85f,
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
            leftEnabled = pref[KEY_SIDE_LEFT_ENABLED] ?: false,
            leftWidthDp = pref[KEY_SIDE_LEFT_WIDTH] ?: 18f,
            leftHeightPercent = pref[KEY_SIDE_LEFT_HEIGHT] ?: 0.5f,
            leftYOffsetPercent = pref[KEY_SIDE_LEFT_OFFSET] ?: 0.25f,
            leftSlideAction = Action.fromString(pref[KEY_SIDE_LEFT_SLIDE] ?: Action.toString(Action.Back)),
            leftHoverAction = Action.fromString(pref[KEY_SIDE_LEFT_HOVER] ?: Action.toString(Action.SwitchPreviousApp)),

            rightEnabled = pref[KEY_SIDE_RIGHT_ENABLED] ?: false,
            rightWidthDp = pref[KEY_SIDE_RIGHT_WIDTH] ?: 18f,
            rightHeightPercent = pref[KEY_SIDE_RIGHT_HEIGHT] ?: 0.5f,
            rightYOffsetPercent = pref[KEY_SIDE_RIGHT_OFFSET] ?: 0.25f,
            rightSlideAction = Action.fromString(pref[KEY_SIDE_RIGHT_SLIDE] ?: Action.toString(Action.Back)),
            rightHoverAction = Action.fromString(pref[KEY_SIDE_RIGHT_HOVER] ?: Action.toString(Action.SwitchPreviousApp))
        )
    }.stateIn(scope, SharingStarted.Eagerly, SideGestureConfig())

    suspend fun updateWhiteBarEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_WB_ENABLED] = enabled }
    }

    suspend fun updateWhiteBarDimensions(widthDp: Float, heightDp: Float, bottomMarginDp: Float, radiusDp: Float) {
        context.dataStore.edit {
            it[KEY_WB_WIDTH] = widthDp
            it[KEY_WB_HEIGHT] = heightDp
            it[KEY_WB_BOTTOM_MARGIN] = bottomMarginDp
            it[KEY_WB_RADIUS] = radiusDp
        }
    }

    suspend fun updateWhiteBarAppearance(color: Long, alpha: Float) {
        context.dataStore.edit {
            it[KEY_WB_COLOR] = color
            it[KEY_WB_ALPHA] = alpha
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
}
