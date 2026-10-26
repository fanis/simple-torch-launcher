package dev.fanis.simpletorchlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.media.VolumeProvider
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager

/**
 * Best-effort volume button detection while the screen is off.
 *
 * Accessibility services get no key events while the screen is off. Instead, Android hands volume
 * button presses to the media session that is currently playing, if there is one. So while the
 * screen is off we keep a media session that pretends to be playing on a remote device, and Android
 * reports each volume press to it as a request to change that remote device's volume. Not every
 * device routes the keys this way.
 *
 * The session only exists while the screen is off, no audio is playing and there is no call, so it
 * never takes the volume buttons away from a music player, a call, or normal use.
 */
class ScreenOffVolumeKeys(
    private val context: Context,
    private val onPress: (direction: Int) -> Unit,
) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val delayedUpdate = Runnable { update() }

    private var screenOff = false
    private var session: MediaSession? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            screenOff = intent.action == Intent.ACTION_SCREEN_OFF
            update()
        }
    }

    // Lets us step aside as soon as other audio starts playing. Not available before Android 8, where
    // we only check for playing audio when the screen turns off.
    private val playbackCallback = if (Build.VERSION.SDK_INT >= 26) {
        object : AudioManager.AudioPlaybackCallback() {
            override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) {
                update()
                // isMusicActive can lag behind this callback, so check again a moment later.
                handler.removeCallbacks(delayedUpdate)
                handler.postDelayed(delayedUpdate, 1000)
            }
        }
    } else {
        null
    }

    fun start() {
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF).apply {
            addAction(Intent.ACTION_SCREEN_ON)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(screenReceiver, filter)
        }
        if (Build.VERSION.SDK_INT >= 26) {
            audio.registerAudioPlaybackCallback(playbackCallback!!, handler)
        }
        screenOff = !context.getSystemService(PowerManager::class.java).isInteractive
        update()
    }

    fun stop() {
        context.unregisterReceiver(screenReceiver)
        if (Build.VERSION.SDK_INT >= 26) {
            audio.unregisterAudioPlaybackCallback(playbackCallback!!)
        }
        handler.removeCallbacks(delayedUpdate)
        setSessionActive(false)
    }

    private fun update() {
        setSessionActive(
            screenOff && Prefs.screenOffEnabled(context) &&
                !audio.isMusicActive && audio.mode == AudioManager.MODE_NORMAL
        )
    }

    private fun setSessionActive(active: Boolean) {
        if (active == (session != null)) return
        if (!active) {
            session?.release()
            session = null
            return
        }
        session = MediaSession(context, "SimpleTorchLauncher").apply {
            setPlaybackToRemote(object : VolumeProvider(VolumeProvider.VOLUME_CONTROL_RELATIVE, 100, 50) {
                override fun onAdjustVolume(direction: Int) {
                    // Key-up arrives as a zero adjustment.
                    if (direction != 0) onPress(direction)
                }
            })
            // Volume changes are only delivered while a callback is set, even an empty one.
            setCallback(object : MediaSession.Callback() {}, handler)
            // Only a session that is playing gets the volume buttons.
            setPlaybackState(
                PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING, 0, 1f).build()
            )
            isActive = true
        }
    }
}
