package com.alex.yang.omronblecompose.di

import com.alex.yang.omronblecompose.data.ble.connector.BleConnector
import com.alex.yang.omronblecompose.data.ble.connector.BleConnectorImpl
import com.alex.yang.omronblecompose.data.ble.repository.BleRepositoryImpl
import com.alex.yang.omronblecompose.data.ble.scanner.BleScanner
import com.alex.yang.omronblecompose.data.ble.scanner.BleScannerImpl
import com.alex.yang.omronblecompose.domain.repository.BleRepository
import com.alex.yang.omronblecompose.domain.usecase.ConnectUseCase
import com.alex.yang.omronblecompose.domain.usecase.ScanUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Created by AlexYang on 2025/12/31.
 *
 *
 */
@Module
@InstallIn(SingletonComponent::class)
object BleModule {
    @Provides
    @Singleton
    fun provideBleScanner(impl: BleScannerImpl): BleScanner = impl

    @Provides
    @Singleton
    fun provideBleRepository(impl: BleRepositoryImpl): BleRepository = impl

    @Provides
    @Singleton
    fun provideScanUseCase(repository: BleRepository): ScanUseCase = ScanUseCase(repository)


    @Provides
    @Singleton
    fun provideBleConnector(impl: BleConnectorImpl): BleConnector = impl

    @Provides
    @Singleton
    fun provideConnectUseCase(repository: BleRepository): ConnectUseCase = ConnectUseCase(repository)
}