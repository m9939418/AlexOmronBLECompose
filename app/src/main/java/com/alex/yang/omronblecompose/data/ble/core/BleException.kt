package com.alex.yang.omronblecompose.data.ble.core

import java.util.UUID

/**
 * Created by AlexYang on 2026/1/2.
 *
 *
 */
sealed class BleException(message: String) : Exception(message) {
    class BondFailed(status: Int) : BleException("Bond failed with status: $status")

    class ConnectFailed(status: Int, state: Int) : BleException("Connect failed: status=$status, state=$state")

    class DiscoverFailed(status: Int) : BleException("Discover services failed: status=$status")

    class ServiceNotFound(uuid: UUID) : BleException("Service not found: $uuid")

    class CharacteristicNotFound(uuid: UUID) : BleException("Characteristic not found: $uuid")

    class CccdNotFound : BleException("CCCD descriptor not found")

    class EnableNotificationFailed(status: Int) : BleException("Enable notification failed: status=$status")

    class WriteCommandFailed(status: Int) : BleException("Write command failed: status=$status")
}