package com.magicmaker.lite.data

import android.content.Context
import com.magicmaker.lite.core.NewWandCatalog
import com.magicmaker.lite.core.StarlightSerial
import com.magicmaker.lite.core.TransmitTiming
import com.magicmaker.lite.core.WandType

/** Everything the app remembers between launches — small enough for SharedPreferences. */
class LitePrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("magicmaker_lite", Context.MODE_PRIVATE)

    var periodMs: Int
        get() = TransmitTiming.clampPeriod(prefs.getInt(KEY_PERIOD, TransmitTiming.DEFAULT_PERIOD_MS))
        set(value) = prefs.edit().putInt(KEY_PERIOD, TransmitTiming.clampPeriod(value)).apply()

    var wandType: WandType
        get() = runCatching { WandType.valueOf(prefs.getString(KEY_WAND, null) ?: "") }
            .getOrDefault(WandType.STARLIGHT)
        set(value) = prefs.edit().putString(KEY_WAND, value.name).apply()

    /** Wand serial the Starlight codes are built from. */
    var serial: String
        get() = StarlightSerial.validatedSerial(prefs.getString(KEY_SERIAL, null))
        set(value) = prefs.edit().putString(KEY_SERIAL, StarlightSerial.validatedSerial(value)).apply()

    fun selectedId(type: WandType): Long =
        prefs.getLong(selectedKey(type), if (type == WandType.NEW) newWandDefaultId() else 0L)

    fun setSelectedId(type: WandType, id: Long) {
        prefs.edit().putLong(selectedKey(type), id).apply()
    }

    /** Starred codes, stored by hex so they survive a serial change gracefully. */
    fun favorites(): Set<String> = prefs.getStringSet(KEY_FAVORITES, emptySet())!!.toSet()

    fun toggleFavorite(hex: String): Set<String> {
        val key = hex.uppercase()
        val next = favorites().toMutableSet()
        if (!next.remove(key)) next.add(key)
        prefs.edit().putStringSet(KEY_FAVORITES, next).apply()
        return next
    }

    private fun selectedKey(type: WandType) = "${KEY_SELECTED_PREFIX}${type.name}"

    private fun newWandDefaultId(): Long =
        NewWandCatalog.codes.firstOrNull { it.hex == NewWandCatalog.OFF_HEX }?.id
            ?: NewWandCatalog.codes.first().id

    private companion object {
        const val KEY_PERIOD = "transmit_period_ms"
        const val KEY_WAND = "wand_type"
        const val KEY_SERIAL = "starlight_serial"
        const val KEY_SELECTED_PREFIX = "selected_id_"
        const val KEY_FAVORITES = "favorite_hex"
    }
}
