package com.magicmaker.lite.core

/**
 * Starlight wand + MagicBand+ 5-bit color palette.
 * @see [EMCOT color table](https://emcot.world/Disney_MagicBand%2B_Bluetooth_Codes)
 */
object StarlightColors {
    /** Full EMCOT 5-bit palette (0x00–0x1F). */
    val palette: Map<Int, String> = mapOf(
        0x00 to "Cyan",
        0x01 to "Purple",
        0x02 to "Blue",
        0x03 to "Midnight Blue",
        0x04 to "Blue",
        0x05 to "Bright Purple",
        0x06 to "Lavender",
        0x07 to "Purple",
        0x08 to "Pink",
        0x09 to "Pink",
        0x0A to "Pink",
        0x0B to "Pink",
        0x0C to "Pink",
        0x0D to "Pink",
        0x0E to "Pink",
        0x0F to "Yellow Orange",
        0x10 to "Off Yellow",
        0x11 to "Yellow Orange",
        0x12 to "Lime",
        0x13 to "Orange",
        0x14 to "Red Orange",
        0x15 to "Red",
        0x16 to "Cyan",
        0x17 to "Cyan",
        0x18 to "Cyan",
        0x19 to "Green",
        0x1A to "Lime Green",
        0x1B to "White",
        0x1C to "White",
        0x1D to "Off",
        0x1E to "Unique",
        0x1F to "Random",
    )

    fun nameFor(index: Int): String =
        palette[index and 0x1F] ?: "Unknown (#${"%02X".format(index and 0x1F)})"

    /** Color index carried by the last byte of a wand code. */
    fun indexOf(hex: String): Int {
        val h = hex.uppercase().filter { it in "0123456789ABCDEF" }
        if (h.length < 2) return 0
        return h.takeLast(2).toInt(16) and 0x1F
    }
}
