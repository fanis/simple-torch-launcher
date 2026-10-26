package dev.fanis.simpletorchlauncher

import android.content.Context
import android.view.KeyEvent

/** The two user settings, shared by the settings screen and [TorchService]. */
object Prefs {
    private const val KEY_USE_VOLUME_DOWN = "use_volume_down"
    private const val KEY_SCREEN_OFF = "screen_off"

    private fun prefs(context: Context) =
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun useVolumeDown(context: Context): Boolean =
        prefs(context).getBoolean(KEY_USE_VOLUME_DOWN, false)

    fun setUseVolumeDown(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(KEY_USE_VOLUME_DOWN, value).apply()

    fun triggerKeyCode(context: Context): Int =
        if (useVolumeDown(context)) KeyEvent.KEYCODE_VOLUME_DOWN else KeyEvent.KEYCODE_VOLUME_UP

    fun screenOffEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SCREEN_OFF, true)

    fun setScreenOffEnabled(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(KEY_SCREEN_OFF, value).apply()
}
