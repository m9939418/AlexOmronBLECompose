package com.alex.yang.omronblecompose.presentation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alex.yang.omronblecompose.presentation.component.Action2Card
import com.alex.yang.omronblecompose.presentation.component.ActionOneCard
import com.alex.yang.omronblecompose.ui.theme.AlexOmronBLEComposeTheme

/**
 * Created by AlexYang on 2025/12/31.
 *
 *
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    state: HomeViewModel.UiState,
    onAction: (HomeViewModel.UiAction) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            text = "OMRON BLE"
        )

        // Action 1: Scan Device
        ActionOneCard(
            state = state,
            onClick = { onAction(HomeViewModel.UiAction.StartScan) }
        )

        // Action 2: Connect Device
        Action2Card(
            state = state,
            onClick = { onAction(HomeViewModel.UiAction.StartConnect) }
        )

        // Action 3: Get Latest Data
//        Action3Card(
//            state = state,
//            onClick = { }
//        )
    }
}

@Preview(
    showBackground = true,
    showSystemUi = false,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    name = "Light Mode"
)
@Preview(
    showBackground = true,
    showSystemUi = false,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Dark Mode"
)
@Composable
fun HomeScreenPreview() {
    AlexOmronBLEComposeTheme {
        HomeScreen(
            state = HomeViewModel.UiState()
        )
    }
}
 