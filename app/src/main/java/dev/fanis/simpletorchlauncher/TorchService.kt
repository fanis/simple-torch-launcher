package dev.fanis.simpletorchlauncher

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Toggles the torch when the chosen volume button is double-pressed.
 *
 * While the screen is on, the accessibility framework passes us every key event before anything
 * else sees it. Presses of the chosen button are held back for [DOUBLE_PRESS_MS]: if a second press
 * follows, the torch toggles; otherwise the press is replayed as a normal volume change. This way a
 * double-press doesn't also change the volume.
 *
 * While the screen is off, [ScreenOffVolumeKeys] reports presses instead. There the volume doesn't
 * change on a press anyway, so nothing is held back.
 *
 * While the app is switched off in its settings, the service stops asking for key events, so key
 * presses skip it entirely.
 */
class TorchService : AccessibilityService(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val handler = Handler(Looper.getMainLooper())
    private var torch: Torch? = null
    private var screenOffKeys: ScreenOffVolumeKeys? = null

    // Screen on: a first press, waiting to see whether a second one follows.
    private var pendingPress: Runnable? = null

    // Screen on: the second press of a double-press is still held, so its key repeats must not
    // change the volume.
    private var ignoreRepeats = false

    // Screen off: when the previous press arrived.
    private var lastScreenOffPress = 0L

    override fun onServiceConnected() {
        torch = Torch(this)
        screenOffKeys = ScreenOffVolumeKeys(this, ::onScreenOffPress).also { it.start() }
        Prefs.registerListener(this, this)
        updateKeyFiltering()

        // The user just turned us on from the setup screen: take them back to it.
        if (SetupActivity.awaitingService) {
            SetupActivity.awaitingService = false
            startActivity(
                Intent(this, SetupActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            )
        }
    }

    override fun onDestroy() {
        Prefs.unregisterListener(this, this)
        pendingPress?.let { handler.removeCallbacks(it) }
        pendingPress = null
        screenOffKeys?.stop()
        torch?.release()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {}

    override fun onInterrupt() {}

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        updateKeyFiltering()
        screenOffKeys?.update()
    }

    private fun updateKeyFiltering() {
        val info = serviceInfo ?: return
        val filtering = (info.flags and AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS) != 0
        if (filtering == Prefs.enabled(this)) return
        info.flags = info.flags xor AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        serviceInfo = info
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!Prefs.enabled(this) || event.keyCode != Prefs.triggerKeyCode(this)) return false
        if (event.action == KeyEvent.ACTION_DOWN) onTriggerKeyDown(event)
        // Swallow the whole press, key-up included. Volume changes are replayed by hand.
        return true
    }

    private fun onTriggerKeyDown(event: KeyEvent) {
        val direction = directionOf(event.keyCode)
        val pending = pendingPress
        when {
            event.repeatCount > 0 -> {
                // The button is being held down: behave like a normal volume button.
                if (ignoreRepeats) return
                if (pending != null) {
                    handler.removeCallbacks(pending)
                    pending.run()
                }
                adjustVolume(direction)
            }

            pending != null -> {
                handler.removeCallbacks(pending)
                pendingPress = null
                ignoreRepeats = true
                torch?.toggle()
            }

            else -> {
                ignoreRepeats = false
                val press = Runnable {
                    pendingPress = null
                    adjustVolume(direction)
                }
                pendingPress = press
                handler.postDelayed(press, DOUBLE_PRESS_MS)
            }
        }
    }

    private fun onScreenOffPress(direction: Int) {
        if (direction != directionOf(Prefs.triggerKeyCode(this))) return
        val now = SystemClock.uptimeMillis()
        // Presses closer together than MIN_PRESS_GAP_MS are key repeats from holding the button.
        if (now - lastScreenOffPress in MIN_PRESS_GAP_MS..DOUBLE_PRESS_MS) {
            lastScreenOffPress = 0
            torch?.toggle()
        } else {
            lastScreenOffPress = now
        }
    }

    private fun adjustVolume(direction: Int) {
        try {
            getSystemService(AudioManager::class.java).adjustSuggestedStreamVolume(
                direction, AudioManager.USE_DEFAULT_STREAM_TYPE, AudioManager.FLAG_SHOW_UI
            )
        } catch (_: SecurityException) {
            // Android refuses changes that would switch Do Not Disturb on or off.
        }
    }

    private fun directionOf(keyCode: Int) =
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER

    companion object {
        private const val DOUBLE_PRESS_MS = 400L
        private const val MIN_PRESS_GAP_MS = 60L

        /** Whether the user has turned this service on in the accessibility settings. */
        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val service = ComponentName(context, TorchService::class.java)
            return enabled.split(':').any { ComponentName.unflattenFromString(it) == service }
        }
    }
}
