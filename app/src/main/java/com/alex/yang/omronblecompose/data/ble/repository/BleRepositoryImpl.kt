package com.alex.yang.omronblecompose.data.ble.repository

import com.alex.yang.omronblecompose.data.ble.connector.BleConnector
import com.alex.yang.omronblecompose.data.ble.scanner.BleScanner
import com.alex.yang.omronblecompose.domain.model.Device
import com.alex.yang.omronblecompose.domain.model.ScanState
import com.alex.yang.omronblecompose.domain.repository.BleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Created by AlexYang on 2025/12/31.
 *
 *
 */
class BleRepositoryImpl @Inject constructor(
    private val bleScanner: BleScanner,
    private val bleConnector: BleConnector,
) : BleRepository {
    override fun startScan(): Flow<ScanState> = bleScanner.scan()

    override fun connect(device: Device) = bleConnector.connect(device)
}