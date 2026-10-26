package dev.fanis.simpletorchlauncher

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView

/** The settings screen: on/off, accessibility status, a torch test button, and the options. */
class MainActivity : Activity() {

    private lateinit var torch: Torch
    private lateinit var torchButton: Button
    private lateinit var accessibilityStatus: TextView
    private lateinit var restrictedHelp: View
    private lateinit var options: List<View>

    // Whether the user has been sent to the accessibility settings, so may have hit Android's
    // "Restricted setting" block there.
    private var openedAccessibilitySettings = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fitToSystemBars(findViewById(R.id.root))
        openedAccessibilitySettings = savedInstanceState?.getBoolean(STATE_OPENED_SETTINGS) ?: false

        findViewById<View>(R.id.about).setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        accessibilityStatus = findViewById(R.id.accessibility_status)
        restrictedHelp = findViewById(R.id.restricted_help)
        findViewById<View>(R.id.open_accessibility_settings).setOnClickListener {
            openedAccessibilitySettings = true
            open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<View>(R.id.open_app_info).setOnClickListener {
            open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
        }

        torchButton = findViewById(R.id.torch)
        torch = Torch(this) { showTorchState() }
        torchButton.setOnClickListener { torch.toggle() }
        showTorchState()

        val button = findViewById<RadioGroup>(R.id.button)
        button.check(if (Prefs.useVolumeDown(this)) R.id.volume_down else R.id.volume_up)
        button.setOnCheckedChangeListener { _, id ->
            Prefs.setUseVolumeDown(this, id == R.id.volume_down)
        }

        val screenOff = findViewById<Switch>(R.id.screen_off)
        screenOff.isChecked = Prefs.screenOffEnabled(this)
        screenOff.setOnCheckedChangeListener { _, checked ->
            Prefs.setScreenOffEnabled(this, checked)
        }

        options = listOf(findViewById<View>(R.id.volume_up), findViewById<View>(R.id.volume_down), screenOff)
        val enabled = findViewById<Switch>(R.id.enabled)
        enabled.isChecked = Prefs.enabled(this)
        enabled.setOnCheckedChangeListener { _, checked ->
            Prefs.setEnabled(this, checked)
            showOptionsEnabled(checked)
        }
        showOptionsEnabled(enabled.isChecked)
    }

    override fun onResume() {
        super.onResume()
        val serviceOn = isServiceEnabled()
        accessibilityStatus.setText(if (serviceOn) R.string.accessibility_on else R.string.accessibility_off)
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
        torch.release()
        super.onDestroy()
    }

    private fun showTorchState() {
        torchButton.isEnabled = torch.isAvailable
        torchButton.setText(
            when {
                !torch.isAvailable -> R.string.torch_missing
                torch.isOn -> R.string.torch_turn_off
                else -> R.string.torch_turn_on
            }
        )
    }

    private fun showOptionsEnabled(enabled: Boolean) {
        options.forEach { it.isEnabled = enabled }
    }

    private fun isServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val service = ComponentName(this, TorchService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == service }
    }

    private fun open(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
        }
    }

    private companion object {
        const val STATE_OPENED_SETTINGS = "opened_accessibility_settings"
    }
}
