package com.prismsoft.testtiles.service

import android.util.Log
import com.prismsoft.testtiles.service.MathUtils.constrain
import com.prismsoft.testtiles.service.MathUtils.exp
import com.prismsoft.testtiles.service.MathUtils.lerp
import com.prismsoft.testtiles.service.MathUtils.norm
import com.prismsoft.testtiles.service.MathUtils.sq
import java.lang.Integer.max
import java.lang.Integer.min
import kotlin.math.*

object BrightnessUtils {
    const val GAMMA_SPACE_MAX = 65535

    private const val R = 0.5f
    private const val A = 0.17883277f
    private const val B = 0.28466892f
    private const val C = 0.55991073f

    fun convertGammaToLinear(value: Int, min: Int = 1, max: Int = 255): Int {
        val normalizedVal = norm(0, GAMMA_SPACE_MAX, value)
        val ret: Float = if (normalizedVal <= R) {
            sq(normalizedVal / R)
        } else {
            exp((normalizedVal - C) / A) + B
        }

//        Math.round(lerp(min, max, normalizedRet / 12))
        // HLG is normalized to the range [0, 12], ensure that value is within that range,
        // it shouldn't be out of bounds.
//        val normalizedRet = constrain(ret, 0f, 12f)
        // Re-normalize to the range [0, 1]
        // in order to derive the correct setting value.
        val result = lerp(min, max, ret)
        Log.i("BrightnessUtil", "for value: $value normalizedval/ret: $normalizedVal | ret: $ret\nresult: $result")
        return result.roundToInt()
    }

    fun convertLinearToGamma(value: Float, min: Float = 1f, max: Float = 255f): Int {
        // For some reason, HLG normalizes to the range [0, 12] rather than [0, 1]
        val normalizedVal = norm(min, max, value) * 12.0f
        val ret: Float = if (normalizedVal <= 1f) {
            MathUtils.sqrt(normalizedVal) * R
        } else {
            A * MathUtils.log(normalizedVal - B) + C
        }
        val result = lerp(0, GAMMA_SPACE_MAX, ret).roundToInt()
        Log.d("QuickSettingsBrightnessService", "normalized: $normalizedVal \nret: $ret \nlerp / result: $result")
        return result
    }

    fun convertRawToPercentagePixel(value: Int) = 1.052.pow(value)
    fun convertPercentageToRaw(percentage: Float): Int {
        return (log(percentage.toDouble(), 10.0) / log(1.052, 10.0)).toInt()
    }
}

object MathUtils {
    fun norm(start: Int, stop: Int, value: Int) = (value - start) / (stop - start)
    fun norm(start: Float, stop: Float, value: Float) = (value - start) / (stop - start)
        //(min(stop, max(start, value)) - start).toFloat() / (stop - start)

    fun sq(value: Float) = value * value
    fun sqrt(value: Float) = kotlin.math.sqrt(value)
    fun log(value: Float) = kotlin.math.log10(value)// ln(value)
    fun lerp(start: Int, stop: Int, amount: Float) = start + (stop - start) * amount
    fun exp(value: Float) = kotlin.math.exp(value)

    fun constrain(amount: Float, low: Float, high: Float) = if (amount < low) low else if (amount > high) high else amount


}