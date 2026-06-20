package abalone

import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/**
 * Launches a new desktop application.
 */
fun main() = application {
    // The `application` function takes a lambda where the UI is initialized.
    val viewModel: AbaloneViewModel = remember {AbaloneViewModel()}
    var showGameWindow by remember { mutableStateOf(false) }
    var showConfigWindow by remember { mutableStateOf(true) }
    var showStateSpaceGeneratorWindow by remember { mutableStateOf(false)}

    if (showGameWindow) {
        Window(
            onCloseRequest = ::exitApplication,
            title = "abalone",
            state = rememberWindowState(
                width = 1280.dp,
                height = 720.dp,
            )
        ) {
            App(viewModel, onShowConfig = {showConfigWindow = true})
        }
    }

    if (showConfigWindow) {
        Window(
            onCloseRequest = ::exitApplication,
            title = "config menu",
            state = rememberWindowState(
                width = 720.dp,
                height = 720.dp
            )
        ) {
            ConfigMenu(viewModel,
                onApplySettings = {
                    showGameWindow = true
                    showConfigWindow = false
                    showStateSpaceGeneratorWindow = false
            },
                onGeneratorView = {
                    showGameWindow = false
                    showConfigWindow = false
                    showStateSpaceGeneratorWindow = true
                })
        }
    }

    if (showStateSpaceGeneratorWindow) {
        Window(
            onCloseRequest = ::exitApplication,
            title = "abalone",
            state = rememberWindowState(
                width = 720.dp,
                height = 720.dp,
            )
        ) {
            StateSpaceGenerator()
        }

    }
}
