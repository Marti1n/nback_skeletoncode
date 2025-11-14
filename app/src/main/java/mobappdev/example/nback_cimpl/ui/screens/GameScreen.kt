package mobappdev.example.nback_cimpl.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mobappdev.example.nback_cimpl.ui.viewmodels.Feedback
import mobappdev.example.nback_cimpl.ui.viewmodels.GameType
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    vm: GameViewModel,
    onBack: () -> Unit
) {
    val state by vm.gameState.collectAsState()
    val score by vm.score.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("N-Back") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val shown = (state.index + 1).coerceAtLeast(0)
                Text("Event: $shown / ${state.totalEvents}", style = MaterialTheme.typography.titleMedium)
                Text("Correct: $score", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            // Stimuli
            when (state.gameType) {
                GameType.Visual -> {
                    Grid3x3(
                        active = state.eventValue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
                GameType.Audio -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎧 Listening...", style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }

            val targetScale = when (state.feedback) {
                Feedback.SUCCESS -> 1.5f
                Feedback.ERROR -> 0.5f
                null -> 1f
            }
            val scale by animateFloatAsState(targetValue = targetScale, label = "match-scale")

            Button(
                onClick = vm::checkMatch,
                enabled = state.canRegisterForThisEvent,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .scale(scale)
                    .height(56.dp)
            ) {
                Text("MATCH!")
            }
        }
    }
}

@Composable
private fun Grid3x3(
    active: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.SpaceEvenly) {
        repeat(3) { r ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(3) { c ->
                    val idx = r * 3 + c
                    val isActive = idx == active
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(6.dp)
                            .aspectRatio(1f)
                            .background(
                                color = if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                    )
                }
            }
        }
    }
}
