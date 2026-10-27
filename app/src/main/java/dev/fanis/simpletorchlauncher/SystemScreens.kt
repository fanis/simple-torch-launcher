package dev.fanis.simpletorchlauncher

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** Opens Android's accessibility settings, in their own task. */
fun Context.openAccessibilitySettings() =
    startSafely(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

/** Opens a web page in the browser. */
fun Context.openWebPage(url: String) = startSafely(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

/** Opens Android's App info page for this app. */
fun Context.openAppInfo() =
    startSafely(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))

// Some devices leave these screens out, or have no browser.
private fun Context.startSafely(intent: Intent) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
    }
}
