package com.prismsoft.testtiles.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.service.quicksettings.Tile
import android.util.Log
import com.prismsoft.testtiles.R

class QuickSettingsSoundService : BaseTileService() {
    private val receiver by lazy { AudioObs { updateUi() } }

    override fun onStartListening() {
        Log.d(TAG, "onStartListening")
        applicationContext.registerReceiver(
            receiver,
            IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION)
        )
    }

    override fun onClick() {
        val am = getSystemService(AudioManager::class.java)

        when (am.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> {
                if(hasPermissions()) {
                    am.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    ding()
                }
            }
            AudioManager.RINGER_MODE_VIBRATE -> {
                when {
                    hasPermissions() -> am.ringerMode = AudioManager.RINGER_MODE_SILENT
                    else -> {
                        am.ringerMode = AudioManager.RINGER_MODE_NORMAL
                        ding()
                    }
                }
            }
            else -> {
                am.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                vibrate()
            }
        }
    }

    override fun onStopListening() {
        applicationContext.unregisterReceiver(receiver)
        Log.d(TAG, "onStopListening")
    }

    private fun ding() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringer = RingtoneManager.getRingtone(applicationContext, uri)
        ringer.play()
    }

    private fun vibrate() {
        val vbr = getSystemService(Vibrator::class.java)
        val attrs = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()

        vbr.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE), attrs)
    }

    override fun updateUi() {
        with(qsTile) {
            val am = getSystemService(AudioManager::class.java)

            when (am.ringerMode) {
                AudioManager.RINGER_MODE_SILENT -> setVolumeOffTile(this)
                AudioManager.RINGER_MODE_VIBRATE -> setVibrateTile(this)
                else -> setVolumeOnTile(this)
            }

            updateTile()
        }
    }

    private fun hasPermissions() = getSystemService(NotificationManager::class.java)
        .isNotificationPolicyAccessGranted

    private fun setVolumeOnTile(tile: Tile) {
        val ic = Icon.createWithResource(applicationContext, R.drawable.ic_baseline_volume_up_24)
        tile.state = Tile.STATE_ACTIVE
        tile.icon = ic
        tile.label = "Volume On"
    }

    private fun setVolumeOffTile(tile: Tile) {
        val ic =
            Icon.createWithResource(applicationContext, R.drawable.ic_baseline_volume_off_24)
        tile.state = Tile.STATE_INACTIVE
        tile.icon = ic
        tile.label = "Volume Mute"
    }

    private fun setVibrateTile(tile: Tile) {
        val ic = Icon.createWithResource(applicationContext, R.drawable.ic_baseline_vibration_24)
        tile.state = Tile.STATE_INACTIVE
        tile.icon = ic
        tile.label = "Vibrate Only"
    }

    private class AudioObs(private val onChanged: () -> Unit) : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) { onChanged() }
    }
}