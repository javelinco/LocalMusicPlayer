package com.javelinco.localmusicplayer.playback.service

import kotlin.math.pow

object ReplayGain {
    fun parseLinearGain(tag: String?): Float {
        val decibels = tag?.trim()?.substringBefore(' ')?.toFloatOrNull() ?: return 1f
        if (!decibels.isFinite() || decibels !in -30f..30f) return 1f
        return 10f.pow(decibels / 20f).coerceIn(0.03162278f, 31.622776f)
    }
}
