package com.magicmaker.lite.ble

import android.annotation.SuppressLint
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.os.Handler
import android.os.Looper
import com.magicmaker.lite.core.HexUtils
import com.magicmaker.lite.core.TransmitTiming
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Raw Disney manufacturer ADV bursts. Timing is decided by
 * [com.magicmaker.lite.core.WandTransmitter]; this class only puts bytes on air.
 *
 * Hardened so a burst doesn't silently vanish: if the radio is mid-settle it waits and retries
 * instead of dropping, and a failed startAdvertising is retried (including the common
 * ALREADY_STARTED race) before giving up. The goal is "a tap always goes on air."
 */
class BleAdvertiser(
    private val advertiserProvider: () -> BluetoothLeAdvertiser?,
    private val onStatus: (String) -> Unit = {},
) {
    private val handler = Handler(Looper.getMainLooper())
    private val busy = AtomicBoolean(false)
    private var activeCallback: AdvertiseCallback? = null

    val isBusy: Boolean get() = busy.get()

    /** Advertise [hex] for [durationMs], then call [onComplete] once the radio is free again. */
    fun burst(hex: String, durationMs: Int, onComplete: () -> Unit = {}) {
        val reject = HexUtils.advertiseRejectReason(hex)
        if (reject != null) {
            onStatus(reject)
            onComplete()
            return
        }
        val payload = HexUtils.advertisePayload(hex)
        if (advertiserProvider() == null) {
            onStatus("Bluetooth off — turn it on to transmit")
            onComplete()
            return
        }
        attemptStart(payload, durationMs, onComplete, tries = 0, busyWaits = 0)
    }

    @SuppressLint("MissingPermission")
    private fun attemptStart(
        payload: ByteArray,
        durationMs: Int,
        onComplete: () -> Unit,
        tries: Int,
        busyWaits: Int,
    ) {
        // Wait (don't drop) if a previous burst is still settling.
        if (!busy.compareAndSet(false, true)) {
            if (busyWaits >= MAX_BUSY_WAITS) {
                onStatus("Radio busy — dropped")
                onComplete()
                return
            }
            handler.postDelayed(
                { attemptStart(payload, durationMs, onComplete, tries, busyWaits + 1) },
                BUSY_WAIT_MS,
            )
            return
        }

        val advertiser = advertiserProvider()
        if (advertiser == null) {
            busy.set(false)
            onStatus("Bluetooth off — turn it on to transmit")
            onComplete()
            return
        }

        // Clean slate — clear any lingering advertise so we don't hit ALREADY_STARTED.
        activeCallback?.let { runCatching { advertiser.stopAdvertising(it) } }
        activeCallback = null

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .setTimeout(0)
            .build()
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addManufacturerData(HexUtils.DISNEY_COMPANY_ID, payload)
            .build()

        val cb = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                onStatus("TX ${durationMs}ms")
                handler.postDelayed({ stop(onComplete) }, durationMs.toLong())
            }

            override fun onStartFailure(errorCode: Int) {
                busy.set(false)
                activeCallback = null
                if (tries < MAX_START_RETRIES) {
                    handler.postDelayed(
                        { attemptStart(payload, durationMs, onComplete, tries + 1, 0) },
                        START_RETRY_MS,
                    )
                } else {
                    onStatus("Advertise error $errorCode")
                    onComplete()
                }
            }
        }
        activeCallback = cb
        runCatching { advertiser.startAdvertising(settings, data, cb) }.onFailure {
            busy.set(false)
            activeCallback = null
            if (tries < MAX_START_RETRIES) {
                handler.postDelayed(
                    { attemptStart(payload, durationMs, onComplete, tries + 1, 0) },
                    START_RETRY_MS,
                )
            } else {
                onStatus("Advertise failed: ${it.message ?: "exception"}")
                onComplete()
            }
        }
    }

    fun stopAll() {
        stop {}
    }

    @SuppressLint("MissingPermission")
    private fun stop(onComplete: () -> Unit) {
        activeCallback?.let { cb -> runCatching { advertiserProvider()?.stopAdvertising(cb) } }
        activeCallback = null
        handler.postDelayed({
            busy.set(false)
            onComplete()
        }, TransmitTiming.RADIO_SETTLE_MS)
    }

    private companion object {
        const val MAX_BUSY_WAITS = 12      // ~1.2s of waiting for the radio to free up
        const val BUSY_WAIT_MS = 100L
        const val MAX_START_RETRIES = 3
        const val START_RETRY_MS = 180L
    }
}
