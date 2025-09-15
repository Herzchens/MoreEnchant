package com.herzchen.moreenchant.utils

import kotlin.random.Random

object FortuneUtils {
    fun applyFortune(baseAmount: Int, fortuneLevel: Int): Int {
        if (fortuneLevel <= 0) return baseAmount

        val r = Random.nextInt(0, fortuneLevel + 2)
        val multiplier = if (r <= 1) 1 else r
        return baseAmount * multiplier
    }
}