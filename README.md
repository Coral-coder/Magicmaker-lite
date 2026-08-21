# MagicMaker Lite

An Android wand transmitter, and nothing else.

One screen that broadcasts wand codes over Bluetooth LE. There is no scanning, no capture, no
library, no relay and no background service — the radio runs only while the app is open and in
front of you.

## What it sends

| Wand | Codes | Notes |
| --- | --- | --- |
| **Starlight** | 32 | The full EMCOT 5-bit color palette, built from the wand serial (`8301CF9B00` + serial + color byte). |
| **New Wand** | 288 | The complete `CF0B` code range `…866E80`–`…866FFF`, minus the `…866F20`–`…866F7F` block that the wand does not use. |

Both catalogs are generated, so every default code ships with the app — nothing has to be learned
or captured first.

## Transmission period

One knob, on the main screen: **200–1000 ms**, in 50 ms steps, default 500 ms. It is how long a
single tap keeps the code on air, and in **REPEAT** mode how often the code is re-broadcast. The
setting is remembered between launches.

## Using it

- Tap the wand, or the **SEND** button, to transmit the armed code.
- Tap any color/code in the strip to arm **and** send it in one go.
- **★** pins a code to the front of the strip.
- **ALL CODES** opens a searchable list of the whole catalog.
- **OFF** sends the current wand's off code.
- **REPEAT** keeps broadcasting until you press **STOP** (or leave the app).
- **SETUP** holds the Starlight wand serial, for anyone who has captured their own.

## Requirements

- Android 8.0 (API 26) or newer, with BLE advertising support.
- Bluetooth turned on, and the **Nearby devices** permission granted on Android 12+.
  No location permission is needed, because the app never scans.

## Building

```sh
cd android
./gradlew assembleDebug
```

The APK lands in `android/app/build/outputs/apk/debug/`. CI builds the same target on every push.

## Credit

Code details are documented by the [EMCOT project](https://emcot.world/Disney_MagicBand%2B_Bluetooth_Codes).
