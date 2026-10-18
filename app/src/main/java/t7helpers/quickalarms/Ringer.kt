package t7helpers.quickalarms

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/** Sound, vibration and a wake lock for one ringing alarm. */
class Ringer(private val context: Context, private val settings: Settings) {
    private val handler = Handler(Looper.getMainLooper())
    private val audio = context.getSystemService(AudioManager::class.java)
    private val wakeLock = context.getSystemService(PowerManager::class.java)
        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "QuickAlarms:ringing")
    private var player: MediaPlayer? = null
    private var tones: ToneGenerator? = null
    private var vibrator: Vibrator? = null
    private var focus: AudioFocusRequest? = null

    fun start() {
        wakeLock.acquire((settings.ringMinutes + 1) * 60_000L)
        focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(ATTRIBUTES)
            .build()
            .also { audio.requestAudioFocus(it) }
        if (!startPlayer()) startTones()
        if (settings.vibrate) startVibration()
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        player?.let {
            runCatching { it.stop() }
            it.release()
        }
        player = null
        tones?.release()
        tones = null
        vibrator?.cancel()
        vibrator = null
        focus?.let { audio.abandonAudioFocusRequest(it) }
        focus = null
        if (wakeLock.isHeld) wakeLock.release()
    }

    /** Tries the chosen sound, then the system defaults; false if none plays. */
    private fun startPlayer(): Boolean {
        val uris = listOfNotNull(
            settings.ringtone?.let(Uri::parse),
            runCatching { RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM) }.getOrNull(),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
        ).distinct()
        for (uri in uris) {
            val mp = MediaPlayer()
            try {
                mp.setAudioAttributes(ATTRIBUTES)
                mp.setDataSource(context, uri)
                mp.isLooping = true
                mp.prepare()
                mp.start()
                player = mp
                return true
            } catch (e: Exception) {
                Log.w(TAG, "Cannot play $uri", e)
                mp.release()
            }
        }
        return false
    }

    /** The last resort, for when no sound file can be read (for example before the first unlock). */
    private fun startTones() {
        val generator = runCatching { ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME) }
            .getOrNull() ?: return
        tones = generator
        handler.post(object : Runnable {
            override fun run() {
                generator.startTone(ToneGenerator.TONE_PROP_BEEP2, 500)
                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun startVibration() {
        val v = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 800, 600), 0)
        if (Build.VERSION.SDK_INT >= 33) {
            v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(effect, ATTRIBUTES)
        }
        vibrator = v
    }

    private companion object {
        const val TAG = "QuickAlarms"
        val ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }
}
