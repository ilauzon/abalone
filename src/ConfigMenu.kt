package abalone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The view for the configuration menu. The config menu is the first screen
 * that the user is greeted with. It allows the user to select:
 * - The type of board layout
 * - The game mode
 * - The colour the user will play as
 * - The move limit of the game
 * - The time limit of the game, per player
 */
@Composable
fun ConfigMenu(viewModel: AbaloneViewModel, onApplySettings: () -> Unit, onGeneratorView: () -> Unit) {
    /** The board layout (Standard, Belgian Daisy, German Daisy). */
    var selectedLayout by remember { mutableStateOf("Standard") }
    /** The game mode (vs. Human, vs. Computer). */
    var selectedMode by remember { mutableStateOf("Bot Vs. Human") }
    /** The color the user will play as during the game (Black, White). */
    var player1Color by remember { mutableStateOf("Black") }
    /** The move limit of the game. */
    var moveLimit by remember { mutableStateOf(40f) }
    /** The time limit of the game. TODO: allow the user to specify time limit per player.
     * */
    var p1TimeLimit by remember { mutableStateOf(10f) }
    var p2TimeLimit by remember { mutableStateOf(10f) }

    Column(
        modifier = Modifier.padding(16.dp).fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ){
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text("Game Settings", style = MaterialTheme.typography.h3)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Initial Layout:", style = MaterialTheme.typography.h6)
                BoardLayoutSelection(selectedLayout) {selectedLayout = it}
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Game mode:", style = MaterialTheme.typography.h6)
                ModeSelection(selectedMode) {selectedMode = it}
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
                ) {
                Text("Player 1 Color:", style = MaterialTheme.typography.h6)
                ColorSelection(player1Color) { player1Color = it }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Move Limit: ${moveLimit.toInt()}", style = MaterialTheme.typography.h6)
                Slider(
                    value = moveLimit,
                    onValueChange = { moveLimit = it },
                    valueRange = 10f..100f)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("P1 Time Limit: ${p1TimeLimit.toInt()} sec (${(p1TimeLimit / 60).toInt()} min)", style = MaterialTheme.typography.h6)
                Slider(
                    value = p1TimeLimit,
                    onValueChange = { newValue ->
                        p1TimeLimit = newValue
                    },
                    valueRange = 5f..180f)

                Text("P2 Time Limit: ${p2TimeLimit.toInt()} sec (${(p2TimeLimit / 60).toInt()} min)", style = MaterialTheme.typography.h6)
                Slider(
                    value = p2TimeLimit,
                    onValueChange = { newValue ->
                        p2TimeLimit = newValue
                    },
                    valueRange = 5f..60f)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = {
                viewModel.updateSettings(p1TimeLimit, p2TimeLimit, selectedLayout, selectedMode, player1Color, moveLimit)
                onApplySettings()
                println("Settings applied! $selectedLayout")
            }) {
                Text("Apply Settings")
            }
            Button(onClick = {onGeneratorView()}){
                Text("Go to State Space Generator")
            }
        }
    }
}

/**
 * The view for the board layout selection menu (Standard, German Daisy, Belgian Daisy)
 *
 * @param selectedLayout the initially selected layout.
 * TODO: make board layout (Standard, etc.) an enum in the data model.
 * @param onSelect a lambda to call when an option is selected. First arg is the string
 * representing the board layout selected.
 */
@Composable
private fun BoardLayoutSelection(selectedLayout: String, onSelect: (String) -> Unit) {
    Column {
        listOf("Standard", "German Daisy", "Belgian Daisy").forEach { layout ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = (selectedLayout == layout),
                    onClick = { onSelect(layout) }
                )
                Text(layout, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

/**
 * The view for the colour selection menu (Black, White)
 *
 * @param selectedColor the initially selected colour.
 * @param onColorSelected lambda to call when an option is selected. First arg is the string
 * representing the colour selected.
 */
@Composable
private fun ColorSelection(selectedColor: String, onColorSelected: (String) -> Unit) {
    listOf("Black", "White").forEach { color ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = (selectedColor == color), onClick = { onColorSelected(color) })
            Text(color, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

/**
 * The view for the mode selection menu (vs. Human, vs. Computer)
 *
 * @param selectedMode the initially selected mode.
 * @param onModeSelected lambda to call when an option is selected. First arg is the string
 * representing the mode selected.
 */
@Composable
private fun ModeSelection(selectedMode: String, onModeSelected: (String) -> Unit) {
    listOf("Human Vs. Human", "Human Vs. Bot", "Bot Vs. Human", "Bot Vs. Bot").forEach { mode ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = (selectedMode == mode), onClick = { onModeSelected(mode) })
            Text(mode, modifier = Modifier.padding(start = 8.dp))
        }
    }
}
