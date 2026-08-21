package com.javelinco.localmusicplayer.playback.queue

import java.security.SecureRandom
import java.util.Random

fun interface RandomSource {
    fun nextInt(bound: Int): Int
}

class SecureRandomSource(
    private val random: SecureRandom = SecureRandom(),
) : RandomSource {
    override fun nextInt(bound: Int): Int = random.nextInt(bound)
}

class SeededRandom(seed: Long) : RandomSource {
    private val random = Random(seed)
    override fun nextInt(bound: Int): Int = random.nextInt(bound)
}

class SequenceRandom(vararg values: Int) : RandomSource {
    private val values = ArrayDeque(values.toList())
    override fun nextInt(bound: Int): Int {
        require(bound > 0)
        val value = if (values.isEmpty()) 0 else values.removeFirst()
        return Math.floorMod(value, bound)
    }
}

fun <T> List<T>.uniformlyShuffled(random: RandomSource): List<T> = toMutableList().apply {
    for (index in lastIndex downTo 1) {
        val swapIndex = random.nextInt(index + 1)
        val item = this[index]
        this[index] = this[swapIndex]
        this[swapIndex] = item
    }
}
