package dev.fanis.simpletorchlauncher

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
            open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<View>(R.id.open_app_info).setOnClickListener {
            open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
        }
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
        if (isFinishing) awaitingService = false
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

    private fun open(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
        }
    }

    companion object {
        private const val STATE_OPENED_SETTINGS = "opened_accessibility_settings"

        /**
         * Set while the user is in the accessibility settings from this screen, so [TorchService] can
         * bring them back here as soon as they turn it on.
         */
        var awaitingService = false
    }
}
