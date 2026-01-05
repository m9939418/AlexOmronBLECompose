package com.alex.yang.omronblecompose.domain.repository

import com.alex.yang.omronblecompose.domain.model.ConnectionState
import com.alex.yang.omronblecompose.domain.model.Device
import com.alex.yang.omronblecompose.domain.model.ScanState
import kotlinx.coroutines.flow.Flow

/**
 * Created by AlexYang on 2025/12/31.
 *
 *
 */
interface BleRepository {
    fun startScan(): Flow<ScanState>

    fun connect(device: Device): Flow<ConnectionState>
}