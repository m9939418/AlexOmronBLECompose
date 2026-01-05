package com.alex.yang.omronblecompose.domain.usecase

import com.alex.yang.omronblecompose.domain.model.Device
import com.alex.yang.omronblecompose.domain.repository.BleRepository
import javax.inject.Inject

/**
 * Created by AlexYang on 2025/12/31.
 *
 *
 */
class ConnectUseCase @Inject constructor(
    private val repository: BleRepository
) {
    operator fun invoke(device: Device) = repository.connect(device)
}