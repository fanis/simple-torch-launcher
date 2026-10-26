package dev.fanis.simpletorchlauncher

import android.os.Build
import android.view.View
import android.view.WindowInsets

/**
 * Pads [view] so its content stays clear of the status bar, navigation bar and display cutout.
 * Apps targeting Android 15 and newer always draw behind those.
 */
fun fitToSystemBars(view: View) {
    val left = view.paddingLeft
    val top = view.paddingTop
    val right = view.paddingRight
    val bottom = view.paddingBottom
    view.setOnApplyWindowInsetsListener { v, insets ->
        if (Build.VERSION.SDK_INT >= 30) {
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            v.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom)
        } else {
            @Suppress("DEPRECATION")
            v.setPadding(
                left + insets.systemWindowInsetLeft,
                top + insets.systemWindowInsetTop,
                right + insets.systemWindowInsetRight,
                bottom + insets.systemWindowInsetBottom,
            )
        }
        insets
    }
}
