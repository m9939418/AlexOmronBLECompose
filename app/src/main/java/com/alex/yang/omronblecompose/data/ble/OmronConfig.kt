@file:Suppress("SpellCheckingInspection")

package com.alex.yang.omronblecompose.data.ble

import java.util.UUID

/**
 * Created by AlexYang on 2025/12/31.
 *
 *
 */
object OmronConfig {
    // HEM-7141T1（D4:F2:DE:EF:74:DC）
    val SERVICE_UUID = UUID.fromString("0000fe4a-0000-1000-8000-00805f9b34fb")

    val DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    val CHARACTERISTIC_C1 = UUID.fromString("b305b680-aee7-11e1-a730-0002a5d5c51b")
    val CHARACTERISTIC_C2 = UUID.fromString("db5b55e0-aee7-11e1-965e-0002a5d5c51b")
    val CHARACTERISTIC_C3 = UUID.fromString("49123040-aee8-11e1-a74d-0002a5d5c51b")

}