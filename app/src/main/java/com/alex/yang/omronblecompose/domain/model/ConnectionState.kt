package com.alex.yang.omronblecompose.domain.model

/**
 * Created by AlexYang on 2025/12/31.
 *
 *
 */
sealed interface ConnectionState {
    data object Idle : ConnectionState

    data object Bonding : ConnectionState
    data object Bonded : ConnectionState

    data object Connecting : ConnectionState
    data object Connected : ConnectionState

    data object DiscoveringServices : ConnectionState

    data object EnablingNotification : ConnectionState

    data class ExecutingCommand(val message: String) : ConnectionState
    data class CommandSuccess(val message: String) : ConnectionState
    data class BloodPressureData(val data: ByteArray) : ConnectionState

    data object Ready : ConnectionState

    data class Error(val message: String) : ConnectionState
}