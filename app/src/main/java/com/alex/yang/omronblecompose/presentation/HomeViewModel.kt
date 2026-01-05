package com.alex.yang.omronblecompose.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alex.yang.omronblecompose.TAG
import com.alex.yang.omronblecompose.data.ble.core.OmronBloodPressureParser
import com.alex.yang.omronblecompose.domain.model.ConnectionState
import com.alex.yang.omronblecompose.domain.model.Device
import com.alex.yang.omronblecompose.domain.model.ScanState
import com.alex.yang.omronblecompose.domain.usecase.ConnectUseCase
import com.alex.yang.omronblecompose.domain.usecase.ScanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by AlexYang on 2025/12/31.
 *
 *
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val scanUseCase: ScanUseCase,
    private val connectUseCase: ConnectUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState())
    val uiState = _uiState.asStateFlow()

    private var scanJob: Job? = null
    private var connectJob: Job? = null
    private var actionJob: Job? = null

    fun onAction(action: UiAction) {
        when (action) {
            UiAction.StartScan -> startScan()
            UiAction.StartConnect -> startConnect()
        }
    }

    fun startScan() {
        if (scanJob?.isActive == true) return

        _uiState.update { it.copy(isScanning = true) }

        scanJob = viewModelScope.launch {
            scanUseCase().collect { scanState ->
                when (scanState) {
                    is ScanState.Found -> {
                        _uiState.update {
                            it.copy(
                                isScanning = false,
                                device = scanState.device,
                                scanError = null
                            )
                        }
                        scanJob?.cancel()
                    }

                    is ScanState.Error -> {
                        _uiState.update {
                            it.copy(
                                isScanning = false,
                                device = null,
                                scanError = scanState.message
                            )
                        }
                    }

                    else -> Unit
                }
            }
        }
    }

    private fun startConnect() {
        val device = _uiState.value.device

        // 檢查是否有掃描到的設備
        if (device == null) {
            _uiState.update {
                it.copy(connectError = "請先掃描設備")
            }
            return
        }

        // 檢查是否已經在連接中
        if (connectJob?.isActive == true) return

        // 重置狀態
        _uiState.update {
            it.copy(
                isConnecting = true,
                isConnected = false,
                connectError = null,
                statusMessage = "",
                bloodPressureData = null
            )
        }

        connectJob = viewModelScope.launch {
            connectUseCase(device).collect { connectionState ->
                _uiState.update { it.copy(connectionState = connectionState) }

                when (connectionState) {
                    is ConnectionState.Bonding -> {
                        _uiState.update { it.copy(isConnecting = true, statusMessage = "配對中...") }
                    }

                    is ConnectionState.Bonded -> {
                        _uiState.update { it.copy(statusMessage = "✅ 配對成功") }
                    }

                    is ConnectionState.Connecting -> {
                        _uiState.update { it.copy(statusMessage = "連接中...") }
                    }

                    is ConnectionState.Connected -> {
                        _uiState.update { it.copy(statusMessage = "✅ 已連接") }
                    }

                    is ConnectionState.DiscoveringServices -> {
                        _uiState.update { it.copy(statusMessage = "發現服務中...") }
                    }

                    is ConnectionState.EnablingNotification -> {
                        _uiState.update { it.copy(statusMessage = "啟用通知中...") }
                    }

                    is ConnectionState.ExecutingCommand -> {
                        _uiState.update {
                            it.copy(statusMessage = connectionState.message)
                        }
                    }

                    is ConnectionState.CommandSuccess -> {
                        _uiState.update {
                            it.copy(statusMessage = connectionState.message)
                        }
                    }

                    is ConnectionState.BloodPressureData -> {
                        val bp = OmronBloodPressureParser.parse(connectionState.data)

                        if (bp != null) {
                            Log.d(TAG, "收到有效血壓: ${bp.sys}/${bp.dia}")
                            _uiState.update {
                                it.copy(
                                    allBloodPressureData = it.allBloodPressureData + connectionState.data,
                                    bloodPressureData = bp // 更新 UI 顯示
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(allBloodPressureData = it.allBloodPressureData + connectionState.data)
                            }
                        }
                    }

                    is ConnectionState.Ready -> {
                        Log.d(TAG, "all=${_uiState.value.allBloodPressureData.size}")

                        _uiState.update {
                            it.copy(
                                isConnecting = false,
                                isConnected = true,
                                statusMessage = "🎉 所有流程完成！"
                            )
                        }
                    }

                    is ConnectionState.Error -> {
                        _uiState.update {
                            it.copy(
                                isConnecting = false,
                                isConnected = false,
                                connectError = connectionState.message,
                                statusMessage = ""
                            )
                        }
                    }

                    else -> Unit
                }
            }
        }
    }

    data class UiState(
        val isScanning: Boolean = false,
        val device: Device? = null,
        val scanError: String? = null,
        val connectionState: ConnectionState? = null,
        val connectError: String? = null,

        val isConnecting: Boolean = false,
        val isConnected: Boolean = false,
        val statusMessage: String = "",
        val bloodPressureData: BloodPressure? = null,
        val allBloodPressureData: List<ByteArray> = emptyList()
    )

    sealed interface UiAction {
        data object StartScan : UiAction
        data object StartConnect : UiAction
    }
}

data class BloodPressure(
    val sys: Int,
    val dia: Int,
    val pulse: Int
)
