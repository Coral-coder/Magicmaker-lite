package com.magicmaker.lite

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.magicmaker.lite.ble.BleAdvertiser
import com.magicmaker.lite.core.WandTransmitter
import com.magicmaker.lite.data.LitePrefs
import com.magicmaker.lite.ui.AppTheme
import com.magicmaker.lite.ui.WandScreen

/**
 * MagicMaker Lite is one screen and one job: transmit wand codes. No scanning, no service, no
 * background work — the radio only runs while this activity is in front of the user.
 */
class MainActivity : ComponentActivity() {

    private lateinit var prefs: LitePrefs
    private lateinit var transmitter: WandTransmitter

    private var status by mutableStateOf("")
    private var bluetoothOn by mutableStateOf(false)
    private var advertiseGranted by mutableStateOf(false)

    /** True once the system stops offering the permission prompt — the only way back is Settings. */
    private var permissionBlocked by mutableStateOf(false)

    /** Bumped whenever the radio is taken down behind the UI's back, so it can drop REPEAT. */
    private var stopTick by mutableIntStateOf(0)

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                bluetoothOn = isBluetoothOn()
                if (!bluetoothOn) stopTransmitting()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = LitePrefs(this)
        transmitter = WandTransmitter(
            BleAdvertiser(
                advertiserProvider = ::bleAdvertiser,
                onStatus = { status = it },
            ),
        )
        advertiseGranted = hasAdvertisePermission()
        bluetoothOn = isBluetoothOn()

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) {
            advertiseGranted = hasAdvertisePermission()
            permissionBlocked = !advertiseGranted && !shouldShowAdvertiseRationale()
        }

        enableEdgeToEdge()
        setContent {
            AppTheme {
                WandScreen(
                    prefs = prefs,
                    transmitter = transmitter,
                    advertiseGranted = advertiseGranted,
                    permissionBlocked = permissionBlocked,
                    bluetoothOn = bluetoothOn,
                    stopTick = stopTick,
                    status = status,
                    onRequestPermission = {
                        // Once the prompt is exhausted, sending them round in circles is useless —
                        // take them straight to the app's permission page instead.
                        if (permissionBlocked) openAppSettings()
                        else permissionLauncher.launch(requiredPermissions())
                    },
                    onOpenBluetoothSettings = {
                        startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                    },
                )
            }
        }

        if (!advertiseGranted) permissionLauncher.launch(requiredPermissions())
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this,
            bluetoothStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onResume() {
        super.onResume()
        advertiseGranted = hasAdvertisePermission()
        bluetoothOn = isBluetoothOn()
    }

    override fun onStop() {
        super.onStop()
        // Never keep broadcasting once the app is out of sight.
        stopTransmitting()
        runCatching { unregisterReceiver(bluetoothStateReceiver) }
    }

    /** Take the radio down and tell the UI, so REPEAT doesn't claim to be running. */
    private fun stopTransmitting() {
        transmitter.stop()
        status = ""
        stopTick++
    }

    private fun openAppSettings() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)),
        )
    }

    private fun shouldShowAdvertiseRationale(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_ADVERTISE)

    private fun bluetoothAdapter(): BluetoothAdapter? =
        (getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private fun bleAdvertiser(): BluetoothLeAdvertiser? =
        bluetoothAdapter()?.takeIf { it.isEnabled }?.bluetoothLeAdvertiser

    private fun isBluetoothOn(): Boolean = bluetoothAdapter()?.isEnabled == true

    private fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_ADVERTISE)
        } else {
            // Pre-12 the Bluetooth permissions are install-time; nothing to ask for.
            emptyArray()
        }

    private fun hasAdvertisePermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
}
