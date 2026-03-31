package ru.sr.presentation.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    agentName: String,
    onToggleSettings: () -> Unit,
) {
    TopAppBar(
        title = { Text("AiAdvent — $agentName") },
        actions = {
            IconButton(onClick = onToggleSettings) {
                Text(
                    text = "⚙",
                    fontSize = 20.sp,
                )
            }
        },
    )
}
