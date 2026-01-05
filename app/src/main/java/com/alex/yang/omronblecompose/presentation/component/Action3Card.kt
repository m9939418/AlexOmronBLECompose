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
import com.alex.yang.omronblecompose.presentation.BloodPressure
import com.alex.yang.omronblecompose.presentation.HomeViewModel
import com.alex.yang.omronblecompose.ui.theme.AlexOmronBLEComposeTheme

/**
 * Created by AlexYang on 2026/1/5.
 *
 *
 */
@Composable
fun Action3Card(
    modifier: Modifier = Modifier,
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
                text = "Step3. 取得一筆資料"
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                ),
                text = "read data"
            )

            Button(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
                Text(text = "同步資料")
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
fun Action3CardPreview() {
    AlexOmronBLEComposeTheme {
        Action3Card(
            state = HomeViewModel.UiState(
                bloodPressureData = BloodPressure(
                    sys = 120,
                    dia = 80,
                    pulse = 72,
                )
            )
        )
    }
}