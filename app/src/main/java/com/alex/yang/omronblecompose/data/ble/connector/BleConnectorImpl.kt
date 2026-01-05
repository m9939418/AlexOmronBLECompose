@file:OptIn(InternalCoroutinesApi::class)

package com.alex.yang.omronblecompose.data.ble.connector

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.alex.yang.omronblecompose.TAG
import com.alex.yang.omronblecompose.data.ble.OmronConfig
import com.alex.yang.omronblecompose.data.ble.core.BleException
import com.alex.yang.omronblecompose.data.ble.core.OmronCommands
import com.alex.yang.omronblecompose.domain.model.ConnectionState
import com.alex.yang.omronblecompose.domain.model.Device
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Created by AlexYang on 2025/12/31.
 *
 *
 */
interface BleConnector {
    fun connect(device: Device): Flow<ConnectionState>

    fun observeNotifications(): Flow<ByteArray>

    suspend fun writeCommand(command: ByteArray)
}

class BleConnectorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BleConnector {
    private val bluetoothManager by lazy { context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    private val adapter by lazy { bluetoothManager.adapter }

    private var currentGatt: BluetoothGatt? = null

    // ─────────────────────────────────────────────
    // Continuations
    // ─────────────────────────────────────────────
    private var connectCont: CancellableContinuation<BluetoothGatt>? = null
    private var discoverCont: CancellableContinuation<Unit>? = null
    private var enableIndCont: CancellableContinuation<Unit>? = null
    private var writeCommandCont: CancellableContinuation<Unit>? = null

    // ─────────────────────────────────────────────
    // Write Command 通知資料 Flow
    // ─────────────────────────────────────────────
    private val _notificationFlow = MutableSharedFlow<ByteArray>(
        replay = 1,
        extraBufferCapacity = 32
    )

    // ─────────────────────────────────────────────
    // GATT Callback（只負責回報結果）
    // ─────────────────────────────────────────────
    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.d(TAG, "📋 onConnectionStateChange: status=$status, newState=$newState")

            connectCont?.let { continuation ->
                if (status == BluetoothGatt.GATT_SUCCESS &&
                    newState == BluetoothProfile.STATE_CONNECTED
                ) {
                    Log.d(TAG, "✅ GATT 已連接")
                    continuation.resume(gatt)
                } else {
                    continuation.resumeWithException(BleException.ConnectFailed(status, newState))
                }

                connectCont = null
                return
            }

            // 非預期的連接狀態變化（例如設備主動斷線）
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.e(TAG, "❌ GATT 已斷線")
                cleanup()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            Log.d(TAG, "📋 onServicesDiscovered: status=$status")

            discoverCont?.let { continuation ->
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(BleException.DiscoverFailed(status))
                }

                discoverCont = null
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            Log.d(
                TAG,
                "📋 onDescriptorWrite: descriptor=${descriptor?.characteristic?.uuid.toString()}, status=$status"
            )

            enableIndCont?.let { continuation ->
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(BleException.EnableNotificationFailed(status))
                }
                enableIndCont = null
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            Log.d(TAG, "📩 onCharacteristicWrite: status=$status, char=${characteristic.uuid}")

            writeCommandCont?.let { continuation ->
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "✅ 命令寫入成功")
                    continuation.resume(Unit)
                } else {
                    Log.e(TAG, "❌ 命令寫入失敗: status=$status")
                    continuation.resumeWithException(BleException.WriteCommandFailed(status))
                }
                writeCommandCont = null
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            Log.d(TAG, "📩 收到通知: ${characteristic.uuid}")
            Log.d(TAG, "📩 資料: ${value.toHexString()}")

            when (characteristic.uuid) {
                OmronConfig.CHARACTERISTIC_C3 -> {
                    _notificationFlow.tryEmit(value) // 血壓
                }

                else -> {
                    Log.d(TAG, "忽略非血壓通知: ${characteristic.uuid}")
                }
            }
        }
    }

    override fun connect(device: Device): Flow<ConnectionState> = flow {
        val remoteDevice = adapter.getRemoteDevice(device.address)

        // ════════════════════════════════════════════════════════════════
        // 1. createBond
        // ════════════════════════════════════════════════════════════════

        // 1.1. Bonding
        emit(ConnectionState.Bonding)

        if (remoteDevice.bondState != BluetoothDevice.BOND_BONDED) {
            val result = remoteDevice.createBond()
            Log.d(TAG, "📱 createBond() result: $result")
        }

        // 1.2. 等待 Bond 結果
        val bondResult = if (remoteDevice.bondState == BluetoothDevice.BOND_BONDED) {
            true
        } else {
            val started = remoteDevice.createBond()
            Log.d(TAG, "📱 createBond() started: $started")
            remoteDevice.awaitBond(context)
        }
        if (!bondResult) {
            emit(ConnectionState.Error("配對失敗"))
            return@flow
        }

        emit(ConnectionState.Bonded)

        // ════════════════════════════════════════════════════════════════
        // 2. Connect GATT
        // ════════════════════════════════════════════════════════════════
        emit(ConnectionState.Connecting)
        val gatt = awaitConnect(remoteDevice)
        emit(ConnectionState.Connected)

        // ════════════════════════════════════════════════════════════════
        // 3. Discover Services
        // ════════════════════════════════════════════════════════════════
        emit(ConnectionState.DiscoveringServices)
        gatt.awaitDiscoverServices()

        // ════════════════════════════════════════════════════════════════
        // 4. Enable Notification (C3, C1)
        // ════════════════════════════════════════════════════════════════
        emit(ConnectionState.EnablingNotification)
        gatt.enableCCCD(OmronConfig.CHARACTERISTIC_C3)
        gatt.enableCCCD(OmronConfig.CHARACTERISTIC_C1)

        // ════════════════════════════════════════════════════════════════
        // 5. Write Command
        // ════════════════════════════════════════════════════════════════

        // Step 1: 查詢設備狀態
        emit(ConnectionState.ExecutingCommand("Step 1: 查詢設備狀態..."))
        writeCommand(OmronCommands.QUERY_DEVICE_STATUS)
        delay(500)
        emit(ConnectionState.CommandSuccess("✅ Step 1 完成"))

        // Step 2: 讀取血壓數據
        emit(ConnectionState.ExecutingCommand("Step 2: 診斷讀取模式..."))
        writeCommand(OmronCommands.READ_BLOOD_PRESSURE)
        delay(500)

        // 嘗試顯示一筆資料
        val bpList = _notificationFlow.replayCache.filter { it.looksLikeOmronBp() }
        val recentData = bpList.lastOrNull()
        if (recentData != null && recentData.size >= 7) {
            Log.d(TAG, "📊 收到數據 (${recentData.size} bytes): ${recentData.toHexString()}")

            emit(ConnectionState.BloodPressureData(recentData))
            emit(ConnectionState.CommandSuccess("✅ Step 2 完成：讀取到血壓數據"))
        } else {
            emit(ConnectionState.CommandSuccess("⚠️ Step 2 完成：未收到數據"))
            if (recentData != null) {
                Log.d(TAG, "收到的數據: ${recentData.toHexString()}")
            }
        }

        // ─────────────────────────────────────────────
        // Step 3: 結束通信
        // ─────────────────────────────────────────────
        emit(ConnectionState.ExecutingCommand("Step 3: 結束通信..."))

        writeCommand(OmronCommands.END_COMMUNICATION)
        delay(500)

        emit(ConnectionState.CommandSuccess("✅ Step 3 完成：通信結束"))
        emit(ConnectionState.Ready)

    }.catch { e ->
        Log.e(TAG, "❌ connect error", e)
        emit(ConnectionState.Error(e.message ?: "unknown error"))
    }

    override fun observeNotifications(): Flow<ByteArray> = _notificationFlow.asSharedFlow()

    // ─────────────────────────────────────────────
    // Action 5: Write Command
    // ─────────────────────────────────────────────
    override suspend fun writeCommand(command: ByteArray) =
        suspendCancellableCoroutine<Unit> { continuation ->
            val gatt = currentGatt
                ?: run {
                    Log.e(TAG, "❌ GATT not connected")
                    continuation.resumeWithException(IllegalStateException("GATT not connected"))
                    return@suspendCancellableCoroutine
                }

            val service = gatt.getService(OmronConfig.SERVICE_UUID)
                ?: run {
                    Log.e(TAG, "❌ 找不到 Service: ${OmronConfig.SERVICE_UUID}")
                    continuation.resumeWithException(
                        BleException.ServiceNotFound(OmronConfig.SERVICE_UUID)
                    )
                    return@suspendCancellableCoroutine
                }

            val characteristic = service.getCharacteristic(OmronConfig.CHARACTERISTIC_C2)
                ?: run {
                    Log.e(TAG, "❌ 找不到 Characteristic C2")
                    continuation.resumeWithException(BleException.CharacteristicNotFound(OmronConfig.CHARACTERISTIC_C2))
                    return@suspendCancellableCoroutine
                }

            Log.d(TAG, "✍️ 寫入命令到 C2: ${command.toHexString()}")

            characteristic.value = command

            writeCommandCont = continuation

            val writeResult = gatt.writeCharacteristic(characteristic)
            if (!writeResult) {
                writeCommandCont = null
                continuation.resumeWithException(BleException.WriteCommandFailed(-1))
                return@suspendCancellableCoroutine
            }

            continuation.invokeOnCancellation {
                writeCommandCont = null
            }
        }

    // ─────────────────────────────────────────────
    // Action 1: Bond
    // ─────────────────────────────────────────────
    private suspend fun BluetoothDevice.awaitBond(context: Context): Boolean =
        suspendCancellableCoroutine { continuation ->

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                        val bondDevice =
                            intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        if (bondDevice?.address == address) {
                            val state = intent.getIntExtra(
                                BluetoothDevice.EXTRA_BOND_STATE,
                                BluetoothDevice.ERROR
                            )
                            val prevState = intent.getIntExtra(
                                BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,
                                BluetoothDevice.ERROR
                            )

                            Log.d(TAG, "📡 配對狀態變化: $prevState → $state")

                            when (state) {
                                BluetoothDevice.BOND_BONDING -> {
                                    Log.d(TAG, "⏳ 配對中...")
                                    Log.d(TAG, "💡 血壓機螢幕：圈圈應該在轉動")
                                }

                                BluetoothDevice.BOND_BONDED -> {
                                    Log.d(TAG, "")
                                    Log.d(TAG, "╔═══════════════════════════════════════╗")
                                    Log.d(TAG, "║   ✅ 配對成功！                      ║")
                                    Log.d(TAG, "╚═══════════════════════════════════════╝")
                                    Log.d(TAG, "💡 血壓機螢幕：圈圈應該停止")
                                    Log.d(TAG, "💡 血壓機螢幕：應該顯示 OK 或方塊")
                                    Log.d(TAG, "")
                                    context.unregisterReceiver(this)
                                    continuation.resume(true)
                                }

                                BluetoothDevice.BOND_NONE -> {
                                    if (prevState == BluetoothDevice.BOND_BONDING) {
                                        Log.e(TAG, "❌ 配對失敗")
                                        context.unregisterReceiver(this)
                                        continuation.resume(false)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            context.registerReceiver(receiver, filter)

            continuation.invokeOnCancellation {
                try {
                    context.unregisterReceiver(receiver)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 取消註冊失敗: ${e.message}")
                }
            }
        }


    // ─────────────────────────────────────────────
    // Action 2: Connect
    // ─────────────────────────────────────────────
    private suspend fun awaitConnect(device: BluetoothDevice): BluetoothGatt =
        suspendCancellableCoroutine { continuation ->
            connectCont = continuation

            currentGatt = device.connectGatt(
                context,
                false,
                gattCallback,
                BluetoothDevice.TRANSPORT_LE
            )

//            continuation.invokeOnCancellation {
//                cleanup()
//            }
        }

    // ─────────────────────────────────────────────
    // Action 3: Discover Services
    // ─────────────────────────────────────────────
    private suspend fun BluetoothGatt.awaitDiscoverServices() =
        suspendCancellableCoroutine<Unit> { continuation ->
            discoverCont = continuation

            if (!this.discoverServices()) {
                discoverCont = null
                continuation.resumeWithException(BleException.DiscoverFailed(-1))
                return@suspendCancellableCoroutine
            }

//            continuation.invokeOnCancellation {
//                cleanup()
//            }
        }

    // ─────────────────────────────────────────────
    // Action 4: Enable Notification
    // ─────────────────────────────────────────────
    private suspend fun BluetoothGatt.enableCCCD(characteristicUuid: UUID) =
        suspendCancellableCoroutine<Unit> { continuation ->

            val service = this.getService(OmronConfig.SERVICE_UUID)
                ?: run {
                    continuation.resumeWithException(BleException.ServiceNotFound(OmronConfig.SERVICE_UUID))
                    return@suspendCancellableCoroutine
                }

            // get characteristic
            val characteristic = service.getCharacteristic(characteristicUuid)
                ?: run {
                    Log.e(TAG, "❌ 找不到 Characteristic: $characteristicUuid")
                    continuation.resumeWithException(
                        BleException.CharacteristicNotFound(characteristicUuid)
                    )
                    return@suspendCancellableCoroutine
                }

            // Enable notifications
            val settingResult = this.setCharacteristicNotification(characteristic, true)
            if (!settingResult) {
                continuation.resumeWithException(BleException.EnableNotificationFailed(-1))
                return@suspendCancellableCoroutine
            }

            // get descriptor
            val descriptor = characteristic.getDescriptor(OmronConfig.DESCRIPTOR_UUID)
                ?: run {
                    continuation.resumeWithException(BleException.CccdNotFound())
                    return@suspendCancellableCoroutine
                }
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            enableIndCont = continuation

            // 寫入 CCCD
            val writeResult = this.writeDescriptor(descriptor)
            if (!writeResult) {
                enableIndCont = null
                continuation.resumeWithException(BleException.EnableNotificationFailed(-1))
                return@suspendCancellableCoroutine
            }

//            continuation.invokeOnCancellation {
//                cleanup()
//            }
        }

    private fun cleanup() {
        Handler(Looper.getMainLooper()).post {
            try {
                currentGatt?.disconnect()
                currentGatt?.close()
            } catch (t: Throwable) {
                Log.e(TAG, "❌ Cleanup error", t)
            } finally {
                currentGatt = null
                connectCont = null
                discoverCont = null
                enableIndCont = null
            }
        }
    }

    fun ByteArray.looksLikeOmronBp(): Boolean {
        // 只接受 20 bytes 的血壓數據包
        if (size != 20) return false

        // 檢查第一個 byte 是否為血壓數據標記
        val byte0 = this[0].toInt() and 0xFF
        if (byte0 != 0x34 && byte0 != 0x20) return false

        // 排除「無數據」回應
        if (size >= 2) {
            val byte1 = this[1].toInt() and 0xFF
            if (byte0 == 0x08 && byte1 == 0x81) return false
        }

        return true
    }
}