package dev.fanis.simpletorchlauncher

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import kotlin.math.min

/**
 * Lays out its only child at the child's own fixed size, then scales it to fit and centres it, the
 * way a video fits its frame. Used for the guide, which floats in a small picture-in-picture window.
 */
class FitFrame(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val child = getChildAt(0) ?: return
        child.layout(0, 0, child.measuredWidth, child.measuredHeight)
        val scale = min(width.toFloat() / child.measuredWidth, height.toFloat() / child.measuredHeight)
        child.pivotX = 0f
        child.pivotY = 0f
        child.scaleX = scale
        child.scaleY = scale
        child.translationX = (width - child.measuredWidth * scale) / 2
        child.translationY = (height - child.measuredHeight * scale) / 2
    }
}
