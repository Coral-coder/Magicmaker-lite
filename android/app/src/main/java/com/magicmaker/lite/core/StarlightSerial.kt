package com.magicmaker.lite.core

/**
 * Starlight wand packet layout: 8301 + marker + 00 + 22-char wand serial + 1-byte color index.
 * @see [Starlight wand format](https://emcot.world/Disney%27s_Starlight_Wand)
 */
object StarlightSerial {
    const val SERIAL_HEX_LEN = 22
    /** Serial the app ships with — the default wand identity every code is built from. */
    const val DEFAULT_SERIAL = "C42922EFD819F22A62"

    fun normalizeSerial(raw: String): String? {
        val s = raw.uppercase().filter { it in "0123456789ABCDEF" }
        if (s.length < SERIAL_HEX_LEN) return null
        return s.take(SERIAL_HEX_LEN)
    }

    fun buildHex(serial: String, colorIndex: Int, marker: String = "CF9B"): String {
        val ser = normalizeSerial(serial) ?: DEFAULT_SERIAL
        val color = "%02X".format(colorIndex and 0x1F)
        val tag = if (marker == "CF0B") "CF0B" else "CF9B"
        return "8301${tag}00$ser$color"
    }

    fun validatedSerial(raw: String?): String = normalizeSerial(raw.orEmpty()) ?: DEFAULT_SERIAL
}
