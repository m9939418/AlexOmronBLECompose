package com.alex.yang.omronblecompose

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alex.yang.omronblecompose.presentation.HomeScreen
import com.alex.yang.omronblecompose.presentation.HomeViewModel
import com.alex.yang.omronblecompose.ui.theme.AlexOmronBLEComposeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestBlePermissions()
        setContent {
            AlexOmronBLEComposeTheme {
                val viewModel = hiltViewModel<HomeViewModel>()
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                HomeScreen(
                    state = state,
                    onAction = viewModel::onAction
                )
            }
        }
    }

    private val requestPerms = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.all { it }
        Log.d(TAG, "Permissions granted=$granted → $result")
    }

    private fun requestBlePermissions() {
        requestPerms.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        )
    }
}