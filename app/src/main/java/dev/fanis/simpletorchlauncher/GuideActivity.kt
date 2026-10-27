package dev.fanis.simpletorchlauncher

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Rational
import android.view.View
import android.widget.Switch
import android.widget.TextView
import java.lang.ref.WeakReference

/**
 * A small guide that floats over Android's accessibility settings in a picture-in-picture window. It
 * loops a mock-up of the steps: tap the app, turn it on, tap Allow. If the user expands it, it
 * offers to open the settings again or close.
 */
class GuideActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var pages: List<View>
    private lateinit var pageList: View
    private lateinit var pageApp: View
    private lateinit var pageDialog: View
    private lateinit var mockSwitch: Switch
    private lateinit var caption: TextView
    private lateinit var buttons: View

    // Set when launched, so the next resume floats the guide and opens the settings.
    private var openSettings = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guide)
        fitToSystemBars(findViewById(R.id.root))
        // Not when Android recreates the guide after the app was killed in the background.
        openSettings = savedInstanceState == null
        current = WeakReference(this)
        shownAt = SystemClock.uptimeMillis()

        pageList = findViewById(R.id.page_list)
        pageApp = findViewById(R.id.page_app)
        pageDialog = findViewById(R.id.page_dialog)
        pages = listOf(pageList, pageApp, pageDialog)
        mockSwitch = findViewById(R.id.mock_switch)
        caption = findViewById(R.id.caption)
        buttons = findViewById(R.id.buttons)

        findViewById<View>(R.id.open_accessibility_settings).setOnClickListener { floatOverSettings() }
        findViewById<View>(R.id.close).setOnClickListener { finish() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openSettings = true
    }

    override fun onResume() {
        super.onResume()
        if (openSettings) {
            openSettings = false
            floatOverSettings()
        }
    }

    override fun onStart() {
        super.onStart()
        play()
    }

    override fun onStop() {
        handler.removeCallbacksAndMessages(null)
        super.onStop()
    }

    override fun onDestroy() {
        if (current?.get() === this) current = null
        super.onDestroy()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        buttons.visibility = if (isInPictureInPictureMode) View.GONE else View.VISIBLE
    }

    private fun floatOverSettings() {
        shownAt = SystemClock.uptimeMillis()
        val floating = Build.VERSION.SDK_INT >= 26 && try {
            val params = PictureInPictureParams.Builder().setAspectRatio(Rational(2, 3))
            // Shrinks smoothly from the mock-up's spot on screen.
            val stage = Rect()
            if (findViewById<View>(R.id.stage).getGlobalVisibleRect(stage)) params.setSourceRectHint(stage)
            if (Build.VERSION.SDK_INT >= 31) {
                // Also floats again if the user expands the guide and then leaves it.
                params.setAutoEnterEnabled(true).setSeamlessResizeEnabled(false)
            }
            enterPictureInPictureMode(params.build())
        } catch (_: IllegalStateException) {
            false
        }
        if (floating) buttons.visibility = View.GONE
        openAccessibilitySettings()
        // Without picture-in-picture there's nothing to show over the settings.
        if (!floating) finish()
    }

    /** Plays the steps once, then starts over. */
    private fun play() {
        handler.removeCallbacksAndMessages(null)
        show(pageList)
        mockSwitch.isChecked = false
        mockSwitch.jumpDrawablesToCurrentState()
        caption.setText(R.string.guide_step_find)

        at(700) { tap(R.id.tap_app) }
        at(1500) {
            show(pageApp)
            caption.setText(R.string.guide_step_switch)
        }
        at(2400) { tap(R.id.tap_switch) }
        at(2700) { mockSwitch.isChecked = true }
        at(3500) {
            pageDialog.animate().alpha(1f).setDuration(FADE_MS)
            caption.setText(R.string.guide_step_allow)
        }
        at(4300) { tap(R.id.tap_allow) }
        at(5100) { pageDialog.animate().alpha(0f).setDuration(FADE_MS) }
        at(6500) { play() }
    }

    private fun at(delayMs: Long, step: () -> Unit) {
        handler.postDelayed(step, delayMs)
    }

    private fun show(page: View) {
        for (p in pages) p.animate().alpha(if (p === page) 1f else 0f).setDuration(FADE_MS)
    }

    /** Pops the tap marker up and fades it out again. */
    private fun tap(markerId: Int) {
        val marker = findViewById<View>(markerId)
        marker.alpha = 0f
        marker.scaleX = 0.5f
        marker.scaleY = 0.5f
        marker.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(0).setDuration(250).withEndAction {
            marker.animate().alpha(0f).setStartDelay(350).setDuration(300)
        }
    }

    companion object {
        private const val FADE_MS = 250L

        private var current: WeakReference<GuideActivity>? = null

        /** When the guide was last sent off to float over the settings. */
        var shownAt = 0L
            private set

        /** Whether this device can float the guide over other apps. */
        fun isSupported(context: Context) =
            Build.VERSION.SDK_INT >= 26 &&
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

        fun close() {
            current?.get()?.finish()
        }
    }
}
