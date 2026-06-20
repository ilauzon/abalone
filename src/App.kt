package abalone

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App(viewModel: AbaloneViewModel, onShowConfig: () -> Unit = {}) {
    MaterialTheme {
        AbaloneGame(viewModel, onShowConfig)
    }
}
