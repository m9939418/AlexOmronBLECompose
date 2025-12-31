package com.alex.yang.omronblecompose.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alex.yang.omronblecompose.domain.model.Device
import com.alex.yang.omronblecompose.domain.model.ScanState
import com.alex.yang.omronblecompose.domain.usecase.ScanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by AlexYang on 2025/12/31.
 *
 *
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val scanUseCase: ScanUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState())
    val uiState = _uiState.asStateFlow()

    private var scanJob: Job? = null

    fun onAction(action: UiAction) {
        when (action) {
            UiAction.StartScan -> startScan()
            else -> Unit
        }
    }

    fun startScan() {
        if (scanJob?.isActive == true) return

        _uiState.update { it.copy(isScanning = true) }

        scanJob = viewModelScope.launch {
            scanUseCase().collect { scanState ->
                when (scanState) {
                    is ScanState.Found -> {
                        _uiState.update {
                            it.copy(
                                isScanning = false,
                                device = scanState.device,
                                scanError = null
                            )
                        }
                        scanJob?.cancel()
                    }

                    is ScanState.Error -> {
                        _uiState.update {
                            it.copy(
                                isScanning = false,
                                device = null,
                                scanError = scanState.message
                            )
                        }
                    }

                    else -> Unit
                }
            }
        }
    }

    data class UiState(
        val isScanning: Boolean = false,
        val device: Device? = null,
        val scanError: String? = null
    )

    sealed interface UiAction {
        data object StartScan : UiAction
    }
}