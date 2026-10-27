package dev.fanis.simpletorchlauncher

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.TextView

class AboutActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        fitToSystemBars(findViewById(R.id.root))

        findViewById<TextView>(R.id.version).text = BuildConfig.VERSION_NAME
        // Links open in the browser, so the app itself still needs no internet access.
        findViewById<View>(R.id.source).setOnClickListener { openWebPage(SOURCE_URL) }
        findViewById<View>(R.id.license).setOnClickListener { openWebPage(LICENSE_URL) }
    }

    private companion object {
        const val SOURCE_URL = "https://github.com/fanis/simple-torch-launcher"
        const val LICENSE_URL = "$SOURCE_URL/blob/HEAD/LICENSE"
    }
}
