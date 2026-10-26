package dev.fanis.simpletorchlauncher

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView

/** The settings screen: service status, which button to use, and the screen-off option. */
class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var restrictedHint: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fitToSystemBars(findViewById(R.id.root))

        status = findViewById(R.id.status)
        restrictedHint = findViewById(R.id.restricted_hint)

        findViewById<View>(R.id.open_settings).setOnClickListener { openAccessibilitySettings() }

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

        findViewById<View>(R.id.about).setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        val enabled = isServiceEnabled()
        status.setText(if (enabled) R.string.status_on else R.string.status_off)
        // Android 13+ blocks accessibility for apps it considers sideloaded until the user allows it.
        restrictedHint.visibility =
            if (!enabled && Build.VERSION.SDK_INT >= 33) View.VISIBLE else View.GONE
    }

    private fun isServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val service = ComponentName(this, TorchService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == service }
    }

    private fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (_: ActivityNotFoundException) {
        }
    }
}
