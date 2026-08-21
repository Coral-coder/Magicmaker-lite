package com.magicmaker.lite.core

/**
 * Transmit timing. The one knob the user gets is the transmission period: how long a single
 * tap keeps the code on air, and — in repeat mode — how often it is re-broadcast.
 */
object TransmitTiming {
    const val MIN_PERIOD_MS = 200
    const val MAX_PERIOD_MS = 1000
    const val DEFAULT_PERIOD_MS = 500
    const val PERIOD_STEP_MS = 50

    /** Gap after stopAdvertising before the radio is asked to advertise again. */
    const val RADIO_SETTLE_MS = 120L

    fun clampPeriod(ms: Int): Int = ms.coerceIn(MIN_PERIOD_MS, MAX_PERIOD_MS)
}
