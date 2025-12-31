package com.alex.yang.omronblecompose.data.ble.scanner

import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.alex.yang.omronblecompose.TAG
import com.alex.yang.omronblecompose.data.ble.OmronConfig
import com.alex.yang.omronblecompose.domain.model.Device
import com.alex.yang.omronblecompose.domain.model.ScanState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/**
 * Created by AlexYang on 2025/12/31.
 *
 *
 */
class BleScannerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BleScanner {
    private val bluetoothManager by lazy { context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    private val adapter by lazy { bluetoothManager.adapter }

    override fun scan() = callbackFlow {
        if (!adapter.isEnabled) {
            trySend(ScanState.Error("Bluetooth is disabled"))
            close()
            return@callbackFlow
        }

        val scanner = adapter.bluetoothLeScanner
            ?: run {
                trySend(ScanState.Error("BluetoothLeScanner is null"))
                close()
                return@callbackFlow
            }

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                val device = result?.device ?: return
                if (device.address.equals(
                        OmronConfig.SERVICE_UUID.toString(),
                        ignoreCase = true
                    )
                ) return

                trySend(
                    ScanState.Found(
                        Device(
                            name = device.name ?: "Unknown",
                            address = device.address,
                            rssi = result.rssi
                        )
                    )
                )

                Log.d(TAG, "📋 scan device:")
                Log.d(TAG, "   name= ${device.name}")
                Log.d(TAG, "   address= ${device.address}")
                Log.d(TAG, "   rssi= ${result.rssi}")
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "❌ scan failed: errorCode=$errorCode")
                trySend(ScanState.Error("Scan failed with error code: $errorCode"))
            }
        }

        trySend(ScanState.Scanning)

        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(OmronConfig.SERVICE_UUID))
                .build()
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .build()
        scanner.startScan(filters, settings, scanCallback)
        Log.d(TAG, "✅ 掃描已啟動")

        awaitClose {
            scanner.stopScan(scanCallback)
            Log.d(TAG, "🛑 停止掃描")
        }
    }
}