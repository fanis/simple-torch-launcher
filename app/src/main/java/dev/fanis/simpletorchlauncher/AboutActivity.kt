package dev.fanis.simpletorchlauncher

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class AboutActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        fitToSystemBars(findViewById(R.id.root))

        findViewById<TextView>(R.id.version).text =
            getString(R.string.about_version, BuildConfig.VERSION_NAME)
    }
}
