package dev.fanis.simpletorchlauncher

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper

/**
 * Switches the camera flash on and off as a torch. Needs no permissions.
 *
 * The torch state is tracked through a callback, so toggling also works when the torch was turned on
 * somewhere else, e.g. from the quick settings tile. [onChange] hears about every change.
 */
class Torch(context: Context, private val onChange: (isOn: Boolean) -> Unit = {}) {

    private val cameraManager = context.getSystemService(CameraManager::class.java)
    private val cameraId = findFlashCamera()

    val isAvailable get() = cameraId != null

    var isOn = false
        private set

    private val callback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(id: String, enabled: Boolean) {
            if (id != cameraId) return
            isOn = enabled
            onChange(enabled)
        }

        override fun onTorchModeUnavailable(id: String) {
            if (id != cameraId) return
            isOn = false
            onChange(false)
        }
    }

    init {
        cameraManager.registerTorchCallback(callback, Handler(Looper.getMainLooper()))
    }

    /** Returns whether the request went through. [onChange] reports the new state shortly after. */
    fun toggle(): Boolean {
        val id = cameraId ?: return false
        return try {
            cameraManager.setTorchMode(id, !isOn)
            true
        } catch (_: CameraAccessException) {
            // The camera is busy, e.g. another app is using it.
            false
        } catch (_: IllegalArgumentException) {
            // The camera went away.
            false
        }
    }

    fun release() {
        cameraManager.unregisterTorchCallback(callback)
    }

    /** Prefers the back camera's flash, falling back to any camera that has one. */
    private fun findFlashCamera(): String? = try {
        val withFlash = cameraManager.cameraIdList.filter {
            cameraManager.getCameraCharacteristics(it)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
        withFlash.firstOrNull {
            cameraManager.getCameraCharacteristics(it)
                .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        } ?: withFlash.firstOrNull()
    } catch (_: CameraAccessException) {
        null
    }
}
