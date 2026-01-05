package com.alex.yang.omronblecompose.data.ble.core

import android.util.Log
import com.alex.yang.omronblecompose.presentation.BloodPressure

/**
 * Created by AlexYang on 2026/1/3.
 *
 *
 */
object OmronBloodPressureParser {
    fun parse(raw: ByteArray): BloodPressure? {
        if (raw.size != 20) return null

        // 取得原始 Byte 並轉為正整數
        val b4 = raw[4].toInt() and 0xFF
        val b5 = raw[5].toInt() and 0xFF
        val b7 = raw[7].toInt() and 0xFF

        // Omron HEM-7141T1 偏移量計算
        val sys = b4 + 30
        val dia = b5 + 26
        val pulse = (b7 and 0x3F) + 60

        // 排除掉 0x80 (無效位元) 或 0xFF
        if (b4 == 0x80 || b4 == 0xFF) return null

        Log.d("Parser", "✅ 還原數值: $sys/$dia Pulse:$pulse (Raw: $b4, $b5, $b7)")
        return BloodPressure(sys, dia, pulse)
    }
}