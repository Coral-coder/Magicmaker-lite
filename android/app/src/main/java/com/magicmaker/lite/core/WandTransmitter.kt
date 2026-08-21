package com.magicmaker.lite.core

import com.magicmaker.lite.ble.BleAdvertiser

/**
 * The whole job of MagicMaker Lite: put a wand code on air for the configured period, once or
 * on repeat. A new command always wins — the generation counter retires whatever loop was
 * running, and the in-flight burst is dropped at its next boundary.
 */
class WandTransmitter(private val ble: BleAdvertiser) {

    @Volatile
    private var generation = 0L

    /** One burst of [periodMs] milliseconds. */
    fun sendOnce(hex: String, periodMs: Int) {
        generation++
        ble.burst(HexUtils.canonicalHex(hex), TransmitTiming.clampPeriod(periodMs))
    }

    /** Re-broadcast [hex] back to back, one burst per period, until [stop] or a new command. */
    fun startRepeat(hex: String, periodMs: Int) {
        generation++
        val gen = generation
        val canonical = HexUtils.canonicalHex(hex)
        val period = TransmitTiming.clampPeriod(periodMs)
        fun next() {
            if (gen != generation) return
            ble.burst(canonical, period) { next() }
        }
        next()
    }

    /** Stop repeating and take the radio down. */
    fun stop() {
        generation++
        ble.stopAll()
    }
}
