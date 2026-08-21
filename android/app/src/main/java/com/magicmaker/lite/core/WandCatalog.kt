package com.magicmaker.lite.core

/** Every default code the app can transmit, for both wand types. */
object WandCatalog {

    /**
     * All 32 Starlight palette codes for a given wand serial. The serial is the wand's identity;
     * the last byte is the color, so the full palette is generated rather than learned.
     */
    fun starlight(serial: String = StarlightSerial.DEFAULT_SERIAL): List<WandCode> {
        val ser = StarlightSerial.validatedSerial(serial)
        return StarlightColors.palette.keys.sorted().map { idx ->
            WandCode(
                id = idx.toLong(),
                hex = StarlightSerial.buildHex(ser, idx),
                name = StarlightColors.nameFor(idx),
                colorIndex = idx,
                wand = WandType.STARLIGHT,
            )
        }
    }

    fun newWand(): List<WandCode> = NewWandCatalog.codes

    fun forType(type: WandType, serial: String): List<WandCode> = when (type) {
        WandType.STARLIGHT -> starlight(serial)
        WandType.NEW -> newWand()
    }
}
