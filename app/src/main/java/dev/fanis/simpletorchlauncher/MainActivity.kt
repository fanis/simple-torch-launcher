package dev.fanis.simpletorchlauncher

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.ToggleButton

/** The settings screen: on/off, accessibility status, a torch test button, and the options. */
class MainActivity : Activity() {

    private lateinit var torch: Torch
    private lateinit var torchButton: ToggleButton
    private lateinit var accessibilityStatus: TextView
    private lateinit var accessibilitySummary: TextView
    private lateinit var setUp: View
    private lateinit var options: List<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fitToSystemBars(findViewById(R.id.root))

        // The app does nothing until accessibility is on, so start with the walkthrough.
        if (savedInstanceState == null && !TorchService.isEnabled(this)) {
            startActivity(Intent(this, SetupActivity::class.java))
        }

        findViewById<View>(R.id.about).setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        accessibilityStatus = findViewById(R.id.accessibility_status)
        accessibilitySummary = findViewById(R.id.accessibility_summary)
        setUp = findViewById(R.id.set_up)
        setUp.setOnClickListener { startActivity(Intent(this, SetupActivity::class.java)) }
        // Removing access is done in Android's settings, on purpose: getting it back takes the whole setup.
        findViewById<View>(R.id.accessibility_row).setOnClickListener {
            if (TorchService.isEnabled(this)) openAccessibilitySettings()
            else startActivity(Intent(this, SetupActivity::class.java))
        }

        torchButton = findViewById(R.id.torch)
        torch = Torch(this) { showTorchState() }
        // The button flips itself on click; put it back if the torch didn't take the request.
        torchButton.setOnClickListener { if (!torch.toggle()) showTorchState() }
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
        val serviceOn = TorchService.isEnabled(this)
        accessibilityStatus.setText(if (serviceOn) R.string.accessibility_on else R.string.accessibility_off)
        accessibilitySummary.setText(
            if (serviceOn) R.string.accessibility_on_summary else R.string.accessibility_off_summary
        )
        setUp.visibility = if (serviceOn) View.GONE else View.VISIBLE
    }

    override fun onDestroy() {
        torch.release()
        super.onDestroy()
    }

    private fun showTorchState() {
        torchButton.isEnabled = torch.isAvailable
        if (!torch.isAvailable) torchButton.textOff = getString(R.string.torch_missing)
        // Also refreshes the label.
        torchButton.isChecked = torch.isOn
    }

    private fun showOptionsEnabled(enabled: Boolean) {
        options.forEach { it.isEnabled = enabled }
    }
}
