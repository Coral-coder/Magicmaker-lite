package com.magicmaker.lite.core

/**
 * The second wand type, distinct from the Starlight wands. Its codes are the CF0B family
 *
 *   8301CF0B00C020224EAC7807866E80 .. 8301CF0B00C020224EAC7807866FFF
 *
 * i.e. a fixed prefix with the trailing 3 nibbles sweeping E80→FFF. The set is fully
 * determined by that range, so it's generated on demand rather than stored.
 */
object NewWandCatalog {
    const val PREFIX = "8301CF0B00C020224EAC7807866"
    private const val RANGE_START = 0xE80
    private const val RANGE_END = 0xFFF

    /** Synthetic id base — keeps New Wand ids clear of the Starlight palette indices. */
    const val ID_BASE = 1_000_000L

    /** The "Off" code (tail 6F1D) — handy as a default and as a panic button. */
    const val OFF_HEX = "8301CF0B00C020224EAC7807866F1D"

    // Codes that don't actually exist on the wand and must be skipped (last-2-bytes 6F20..6F7F,
    // i.e. the trailing-nibble value 0xF20..0xF7F).
    private val EXCLUDED = 0xF20..0xF7F

    /** Every real code in the range, low-to-high. */
    val codes: List<WandCode> by lazy {
        (RANGE_START..RANGE_END).filter { it !in EXCLUDED }.map { v ->
            val hex = PREFIX + "%03X".format(v)
            WandCode(
                id = ID_BASE + (v - RANGE_START),
                hex = hex,
                name = StarlightColors.nameFor(StarlightColors.indexOf(hex)),
                colorIndex = StarlightColors.indexOf(hex),
                wand = WandType.NEW,
            )
        }
    }
}
