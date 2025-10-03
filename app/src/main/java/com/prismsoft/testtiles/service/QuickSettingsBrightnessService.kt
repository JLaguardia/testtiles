package com.prismsoft.testtiles.service

import android.content.ContentResolver
import android.database.ContentObserver
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.quicksettings.Tile
import android.util.Log
import com.prismsoft.testtiles.R
import kotlin.math.roundToInt


class QuickSettingsBrightnessService : BaseTileService() {

    private val contentObserver by lazy {
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                updateUi()
            }
        }
    }

    private val canWriteSettings: Boolean
        get() = Settings.System.canWrite(applicationContext)

    private val resolver: ContentResolver
        get() = applicationContext.contentResolver

    override fun onClick() {
        val percentage = getBrightnessPercentage()
        if (canWriteSettings) {
            setBrightnessToggle(getBrightnessClass(percentage))
        }

        updateUi()
    }

    override fun onStartListening() {
        Log.d(TAG, "onStartListening")
        resolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
            false,
            contentObserver
        )

        updateUi()
        super.onStartListening()
    }

    override fun onStopListening() {
        resolver.unregisterContentObserver(contentObserver)
        super.onStopListening()
    }

    override fun onTileRemoved() {
        Log.d(TAG, "onTileRemoved")
    }

    override fun updateUi() {
        with(qsTile) {
            val percentage = getBrightnessPercentage()
            setBrightnessTile(this, percentage)
            updateTile()
        }
    }

    private fun getBrightnessPercentage(): Int {
        val rawBrightness = getCurrentBrightness()
        val gamma = BrightnessUtils.convertLinearToGamma(rawBrightness) //to min???
//        val gamma = BrightnessUtils.convertRawToPercentagePixel(rawBrightness)
        val percentage = (gamma.toFloat() - 0f) / (BrightnessUtils.GAMMA_SPACE_MAX.toFloat() - 0f)
//        val percentage = gamma.toPercentValue(BrightnessUtils.GAMMA_SPACE_MAX)
        Log.d(TAG, "rawBrightness / perc: $rawBrightness / $percentage% | new: $gamma")

        if(true) {
            return when (rawBrightness.toInt()) {
                in 0..3 -> 10
                in 4..15 -> 50
                else -> 90
            }
        }
        return gamma.toInt()
    }

    private fun setBrightnessTile(tile: Tile, percentage: Int) {
        val cls = getBrightnessClass(percentage)
        val (state, ic) = when (cls) {
            BrightType.HIGH -> Tile.STATE_ACTIVE to getIcon(R.drawable.ic_baseline_brightness_full)
            BrightType.MED -> Tile.STATE_ACTIVE to getIcon(R.drawable.ic_baseline_brightness_half)
            else -> Tile.STATE_INACTIVE to getIcon(R.drawable.ic_baseline_brightness_empty)
        }

        tile.state = state
        tile.icon = if (canWriteSettings) {
            ic
        } else {
            getIcon(R.drawable.ic_baseline_settings_brightness)
        }
        tile.label = "Brightness"
        tile.subtitle = cls.desc
    }

    private fun getIcon(resId: Int) = Icon.createWithResource(applicationContext, resId)

    private fun setBrightnessToggle(brightClass: BrightType) {
        val mode = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0)
        if(mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
            Settings.System.putInt(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
        } //128 == 100%
          //12 == 50%
          //4 == 25% ?? makes no fucking sense

//        val newValue = when (brightClass) {
//            BrightType.HIGH -> BrightnessUtils.convertGammaToLinear(1 / BrightnessUtils.GAMMA_SPACE_MAX)
//            BrightType.MED -> BrightnessUtils.convertGammaToLinear(BrightnessUtils.GAMMA_SPACE_MAX)
//            else -> BrightnessUtils.convertGammaToLinear((BrightnessUtils.GAMMA_SPACE_MAX * 0.5f).toInt())
//        }
        val newValue = when (brightClass) {
            BrightType.HIGH -> BrightnessUtils.convertPercentageToRaw(1.0f)
            BrightType.MED -> BrightnessUtils.convertPercentageToRaw(100.0f)
            else -> 12//BrightnessUtils.convertPercentageToRaw(50.0f)
        }.coerceAtLeast(1)

//        Log.i(TAG, "class is $brightClass - setting new brightness to $newValue")

        Settings.System.SCREEN_BRIGHTNESS_MODE
        Settings.System.putInt(
            resolver,
            Settings.System.SCREEN_BRIGHTNESS,
            newValue
        )
    }

    private fun getBrightnessClass(percentage: Int) = when (percentage) {
        in 80..100 -> BrightType.HIGH
        in 30..79 -> BrightType.MED
        else -> BrightType.LOW
    }

    private enum class BrightType(val desc: String) { HIGH("High"), MED("Med"), LOW("Low") }

    fun <T: Number> T.toPercentValue(scale: T) = (this.toFloat() * 100 / scale.toFloat()).roundToInt()

    private fun getCurrentBrightness() = Settings.System.getFloat(
        resolver,
        Settings.System.SCREEN_BRIGHTNESS,
        BrightnessUtils.GAMMA_SPACE_MAX / 2.0f
    )
}