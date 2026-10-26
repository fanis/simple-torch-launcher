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
 * somewhere else, e.g. from the quick settings tile.
 */
class Torch(context: Context) {

    private val cameraManager = context.getSystemService(CameraManager::class.java)
    private val cameraId = findFlashCamera()
    private var isOn = false

    private val callback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(id: String, enabled: Boolean) {
            if (id == cameraId) isOn = enabled
        }

        override fun onTorchModeUnavailable(id: String) {
            if (id == cameraId) isOn = false
        }
    }

    init {
        cameraManager.registerTorchCallback(callback, Handler(Looper.getMainLooper()))
    }

    fun toggle() {
        val id = cameraId ?: return
        try {
            cameraManager.setTorchMode(id, !isOn)
        } catch (_: CameraAccessException) {
            // The camera is busy, e.g. another app is using it.
        } catch (_: IllegalArgumentException) {
            // The camera went away.
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
