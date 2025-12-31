package com.alex.yang.omronblecompose.domain.model

/**
 * Created by AlexYang on 2025/12/31.
 *
 *
 */
sealed interface ScanState {
    data object Idle : ScanState

    data object Scanning : ScanState

    data class Found(val device: Device) : ScanState

    data class Error(val message: String) : ScanState
}

data class Device(
    val name: String?,
    val address: String,
    val rssi: Int
)