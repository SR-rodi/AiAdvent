package ru.sr.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import ru.sr.presentation.ui.components.AgentSidebar
import ru.sr.presentation.ui.components.InputBar
import ru.sr.presentation.ui.components.MessageBubble
import ru.sr.presentation.ui.components.SettingsSidebar
import ru.sr.presentation.ui.components.TopBar

@Composable
fun ChatScreen(viewModel: ChatViewModel = koinInject()) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.clear() }
    }

    Scaffold(
        topBar = {
            TopBar(
                agentName = state.currentAgentName,
                tokenStats = state.tokenStats,
                onToggleSettings = viewModel::toggleSettingsPanel,
            )
        },
    ) { padding ->
        Row(modifier = Modifier.padding(padding).fillMaxSize()) {
            AgentSidebar(
                agentNames = state.agentNames,
                currentAgent = state.currentAgentName,
                onAgentClick = viewModel::switchAgent,
                onNewAgentClick = viewModel::showNewAgentDialog,
                showDialog = state.showNewAgentDialog,
                onCreateAgent = { name, settings -> viewModel.createAgent(name, settings) },
                onDismissDialog = viewModel::dismissNewAgentDialog,
            )
            VerticalDivider()
            Column(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                ) {
                    items(state.messages, key = { it.id }) { message ->
                        MessageBubble(message)
                    }
                }
                InputBar(
                    text = state.inputText,
                    onTextChange = viewModel::onInputChanged,
                    onSend = viewModel::sendInput,
                    enabled = !state.isStreaming,
                )
            }
            if (state.isSettingsPanelVisible) {
                VerticalDivider()
                SettingsSidebar(
                    settings = state.settings,
                    onTemperatureChanged = viewModel::onTemperatureChanged,
                    onMaxTokensChanged = viewModel::onMaxTokensChanged,
                    onTopPChanged = viewModel::onTopPChanged,
                    onFrequencyPenaltyChanged = viewModel::onFrequencyPenaltyChanged,
                    onPresencePenaltyChanged = viewModel::onPresencePenaltyChanged,
                    onStopChanged = viewModel::onStopChanged,
                    onReset = viewModel::onSettingsReset,
                )
            }
        }
    }
}
