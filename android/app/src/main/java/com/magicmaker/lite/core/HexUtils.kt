package com.magicmaker.lite.core

/**
 * Disney BLE packet helpers — just what a transmitter needs.
 *
 * Wand codes are written as a full packet ("8301" + body). Android's advertiser takes the
 * company id separately, so the leading 8301 is stripped before it goes on air.
 */
object HexUtils {
    const val DISNEY_COMPANY_ID = 0x0183
    const val MAX_MFG_PAYLOAD_BYTES = 27

    fun hexToBytes(hex: String): ByteArray {
        val clean = hex.uppercase().filter { it in "0123456789ABCDEF" }
        val byteCount = clean.length / 2
        return ByteArray(byteCount) { i ->
            clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    /** Normalizes user/catalog hex to the stored "8301…" form. */
    fun canonicalHex(hex: String): String {
        val clean = hex.uppercase().filter { it in "0123456789ABCDEF" }
        if (clean.isEmpty()) return clean
        if (clean.startsWith("8301")) return clean
        return "8301$clean"
    }

    fun isStarlightPacket(hex: String): Boolean {
        val h = canonicalHex(hex)
        return h.contains("CF9B") || h.contains("CF0B")
    }

    /** Manufacturer payload for AdvertiseData.addManufacturerData(0x0183, …) — no company id bytes. */
    fun advertisePayload(hex: String): ByteArray {
        val bytes = hexToBytes(canonicalHex(hex))
        return if (bytes.size >= 2 && bytes[0] == 0x83.toByte() && bytes[1] == 0x01.toByte()) {
            bytes.copyOfRange(2, bytes.size)
        } else {
            bytes
        }
    }

    /** Null when the packet fits a legacy 31-byte advertisement; otherwise why it can't be sent. */
    fun advertiseRejectReason(hex: String): String? {
        val payload = advertisePayload(hex)
        if (payload.isEmpty()) return "Empty packet"
        if (payload.size > MAX_MFG_PAYLOAD_BYTES) {
            return "Packet too long for BLE ADV (${payload.size} bytes, max $MAX_MFG_PAYLOAD_BYTES)"
        }
        // 1 length byte + 1 AD type byte + 2 company id bytes + payload, inside the 31-byte PDU.
        if (2 + 2 + payload.size > 31) return "Packet too long for BLE ADV"
        return null
    }
}
