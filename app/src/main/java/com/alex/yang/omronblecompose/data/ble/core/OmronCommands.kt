package com.alex.yang.omronblecompose.data.ble.core

/**
 * Created by AlexYang on 2026/1/2.
 *
 *
 */
object OmronCommands {
    /**
     * 命令 1: 查詢設備狀態
     * 發送到 C2: 08 00 00 00 00 10 00 18
     * 預期回應 C3: 08 80 00 00 00 10 00 98 (設備就緒)
     */
    val QUERY_DEVICE_STATUS = byteArrayOf(
        0x08, 0x00, 0x00, 0x00, 0x00, 0x10, 0x00, 0x18
    )

    /**
     * 命令 2: 讀取血壓數據
     * 發送到 C2: 08 01 00 02 60 2C 00 47
     * 預期回應 C3:
     * - 34 81 ... (有血壓數據)
     * - 08 81 ... (無數據)
     */
    val READ_BLOOD_PRESSURE = byteArrayOf(0x08, 0x01, 0x00, 0x02, 0x60, 0x2C, 0x00, 0x47)

    /**
     * 命令 3: 結束通信
     * 發送到 C2: 08 0F 00 00 00 00 00 07
     * 預期回應 C3: 08 8F 00 00 00 00 00 87
     */
    val END_COMMUNICATION = byteArrayOf(
        0x08, 0x0F, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07
    )
}