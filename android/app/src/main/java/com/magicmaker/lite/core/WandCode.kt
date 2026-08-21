package com.magicmaker.lite.core

/** One ready-to-transmit wand code. */
data class WandCode(
    val id: Long,
    val hex: String,
    val name: String,
    val colorIndex: Int,
    val wand: WandType,
) {
    /** Short handle shown in lists — the last two bytes are what distinguishes codes. */
    val tail: String get() = hex.uppercase().takeLast(4)
}

enum class WandType(val label: String) {
    STARLIGHT("STARLIGHT"),
    NEW("NEW WAND"),
}
