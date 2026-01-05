package com.alex.yang.omronblecompose.presentation.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alex.yang.omronblecompose.domain.model.ConnectionState
import com.alex.yang.omronblecompose.presentation.HomeViewModel
import com.alex.yang.omronblecompose.ui.theme.AlexOmronBLEComposeTheme

/**
 * Created by AlexYang on 2026/1/5.
 *
 *
 */
@Composable
fun Action2Card(
    state: HomeViewModel.UiState,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                text = "Step2. 配對裝置"
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                ),
                text = "bond + gatt + discover + notify"
            )

            Button(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
                Text(text = "CONNECT")
            }

            // ✅ 顯示連接狀態
            state.connectionState?.let { connectionState ->
                HorizontalDivider(modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    text = when (connectionState) {
                        is ConnectionState.Bonding -> "⏳ 配對中..."
                        is ConnectionState.Bonded -> "✅ 已配對"
                        is ConnectionState.Connecting -> "⏳ 連接中..."
                        is ConnectionState.Connected -> "✅ 已連接"
                        is ConnectionState.DiscoveringServices -> "⏳ 發現服務中..."
                        is ConnectionState.EnablingNotification -> "⏳ 啟用通知中..."
                        is ConnectionState.Ready -> "🎉 連接完成！"
                        is ConnectionState.Error -> "❌ 錯誤: ${connectionState.message}"
                        else -> ""
                    }
                )
            }

            // TODO: 顯示血壓數據
            state.bloodPressureData?.let { data ->
                HorizontalDivider(modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(6.dp))

                BloodPressureCard(data = data)
            }
        }
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
fun Action2CardPreview() {
    AlexOmronBLEComposeTheme {
        Action2Card(
            state = HomeViewModel.UiState(
                connectionState = ConnectionState.Connected
            )
        )
    }
}