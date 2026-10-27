package dev.fanis.simpletorchlauncher

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Walks the user through turning on the accessibility service: why it's needed, what the app
 * promises, what Android's warning means, and the steps. Once the service is on, the user can try a
 * double-press right here.
 */
class SetupActivity : Activity() {

    private lateinit var intro: View
    private lateinit var done: View
    private lateinit var restrictedHelp: View
    private lateinit var tryResult: TextView
    private lateinit var torch: Torch

    // The torch reports its current state as soon as we start listening; only later changes count
    // as the user trying it out.
    private var lastTorchOn: Boolean? = null

    // Whether the user has been sent to the accessibility settings, so may have hit Android's
    // "Restricted setting" block there.
    private var openedAccessibilitySettings = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)
        fitToSystemBars(findViewById(R.id.root))
        openedAccessibilitySettings = savedInstanceState?.getBoolean(STATE_OPENED_SETTINGS) ?: false

        intro = findViewById(R.id.intro)
        done = findViewById(R.id.done)
        restrictedHelp = findViewById(R.id.restricted_help)
        tryResult = findViewById(R.id.try_result)

        addPoints(findViewById(R.id.promises), R.array.setup_promises, numbered = false)
        addPoints(findViewById(R.id.steps), R.array.setup_steps, numbered = true)

        findViewById<View>(R.id.open_accessibility_settings).setOnClickListener {
            openedAccessibilitySettings = true
            awaitingService = true
            if (GuideActivity.isSupported(this)) {
                // The guide floats over the settings and opens them itself.
                startActivity(Intent(this, GuideActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } else {
                openAccessibilitySettings()
            }
        }
        findViewById<View>(R.id.open_app_info).setOnClickListener { openAppInfo() }
        findViewById<View>(R.id.not_now).setOnClickListener { finish() }
        findViewById<View>(R.id.finish).setOnClickListener { finishSetup() }

        torch = Torch(this) { on ->
            val changed = lastTorchOn != null && lastTorchOn != on
            lastTorchOn = on
            if (changed && done.visibility == View.VISIBLE) {
                tryResult.visibility = View.VISIBLE
                tryResult.setText(if (on) R.string.setup_try_on else R.string.setup_try_off)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val serviceOn = TorchService.isEnabled(this)
        // The guide only helps over Android's settings, so close it once the user is back here. This
        // screen also shows for a moment while the guide shrinks and the settings open; skip that.
        if (serviceOn || SystemClock.uptimeMillis() - GuideActivity.shownAt > GUIDE_SETTLE_MS) {
            GuideActivity.close()
        }
        if (serviceOn) {
            awaitingService = false
            // Going through setup means the user wants the shortcut, even if they switched it off before.
            if (openedAccessibilitySettings) Prefs.setEnabled(this, true)
            findViewById<TextView>(R.id.try_it).setText(
                if (Prefs.useVolumeDown(this)) R.string.setup_try_down else R.string.setup_try_up
            )
            if (done.visibility != View.VISIBLE) findViewById<View>(R.id.root).scrollTo(0, 0)
        }
        intro.visibility = if (serviceOn) View.GONE else View.VISIBLE
        done.visibility = if (serviceOn) View.VISIBLE else View.GONE
        // Android 13+ may block accessibility for apps it considers sideloaded. There is no public way to
        // tell whether that happened, so offer help once the user has tried and it's still off.
        restrictedHelp.visibility =
            if (!serviceOn && openedAccessibilitySettings && Build.VERSION.SDK_INT >= 33) View.VISIBLE
            else View.GONE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_OPENED_SETTINGS, openedAccessibilitySettings)
    }

    override fun onDestroy() {
        if (isFinishing) {
            awaitingService = false
            GuideActivity.close()
        }
        torch.release()
        super.onDestroy()
    }

    private fun finishSetup() {
        // The service may have opened this screen on its own, with nothing behind it.
        if (isTaskRoot) startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun addPoints(container: LinearLayout, items: Int, numbered: Boolean) {
        resources.getStringArray(items).forEachIndexed { i, text ->
            val row = layoutInflater.inflate(R.layout.item_point, container, false)
            row.findViewById<TextView>(R.id.marker).text =
                if (numbered) getString(R.string.step_number, i + 1) else getString(R.string.bullet)
            row.findViewById<TextView>(R.id.text).text = text
            container.addView(row)
        }
    }

    companion object {
        private const val STATE_OPENED_SETTINGS = "opened_accessibility_settings"
        private const val GUIDE_SETTLE_MS = 2000L

        /**
         * Set while the user is in the accessibility settings from this screen, so [TorchService] can
         * bring them back here as soon as they turn it on.
         */
        var awaitingService = false
    }
}
