package abalone

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import abalone.model.AbaloneFileIO
import abalone.model.Action
import abalone.model.StateRepresentation
import abalone.model.StateSpaceGenerator
import java.io.File
import java.nio.file.Paths

@Composable
fun StateSpaceGenerator(){
    val files:SnapshotStateList<String?> = getAllFiles()
    var fileNameDropdownInput by remember{ mutableStateOf("")}
    var fileName by remember{ mutableStateOf("")}
    var isFileLoaded by remember{ mutableStateOf(false)}
    var isOutputFileLoaded by remember{ mutableStateOf(false)}
    Box(modifier = Modifier.padding(15.dp)) {

        Column {
            Text("State Space Generator", style = MaterialTheme.typography.h3)

            Text("To generate all possible moves and board configurations, please select" +
                    " a file below and press Enter. If you don't see a file here that you'd like" +
                    " to test, please move that file into the same folder as the executable, then" +
                    " rerun the executable.",
                style = MaterialTheme.typography.body1)
            Box(
                modifier = Modifier.padding(16.dp)
            ) {
                LazyColumn {
                    items(files.size) {
                        Box{
                            val text = if (files[it] == null) "" else files[it]
                            if (text != null) {
                                Box(modifier = Modifier
                                    .safeContentPadding()
                                    .clickable {
                                        fileNameDropdownInput = text
                                }) {
                                    Text(text, style = MaterialTheme.typography.body1)
                                }
                            }
                        }
                    }
                }
            }
            Button(onClick = {
                fileName = fileNameDropdownInput
                isFileLoaded = true
            }) {
                Text("Enter")
            }
            Column(modifier = Modifier.safeContentPadding()){
                if (isFileLoaded and fileName.isNotEmpty()) {
                    // create output file names
                    val outputMoveFile = "${fileName.split(".")[0]}.move"
                    val outputBoardFile = "${fileName.split(".")[0]}.board"

                    // read the file input
                    val text = AbaloneFileIO.readDataFile(fileName)

                    //parse board from file
                    val boardState = AbaloneFileIO.parseState(text[0], text[1])

                    // feed board into StateSpaceGenerator
                    val boards:MutableList<StateRepresentation> = mutableListOf()
                    val actions: List<Action> = StateSpaceGenerator.actions(boardState).toList()
                    for (action in actions) {
                        boards.add(StateSpaceGenerator.result(boardState, action))
                    }

                    // turn actions and boards into strings for file output
                    val actionsStrings = AbaloneFileIO.stringifyActions(actions)
                    val boardStrings = AbaloneFileIO.stringifyBoards(boards)

                    // write output to respective files
                    AbaloneFileIO.writeDataFile(outputMoveFile, actionsStrings){
                        AbaloneFileIO.writeDataFile(outputBoardFile, boardStrings){
                            isOutputFileLoaded = true
                        }
                    }
                    // generate file success message, highlight file selected
                    if (isOutputFileLoaded) {
                        Box(modifier = Modifier.safeContentPadding(),
                            contentAlignment = Alignment.Center) {
                            Text("Your $outputMoveFile and $outputBoardFile files are ready!" +
                                    " They will be found in the same file as this executable is located." +
                                    " To generate more state spaces, choose another file and press Enter again.")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Returns a list of all the .input files in the current directory.
 *
 * @return .input files as a SnapShotStateList<String> object
 */
private fun getAllFiles(): SnapshotStateList<String?> {
    val path = Paths.get("").toAbsolutePath().toString()
    val files = File(path).listFiles()
    val fileNames = arrayOfNulls<String>(files?.size ?: 0 )
    files?.mapIndexed { index, item ->
        fileNames[index] = item?.name
    }
    fileNames.filterNotNull()
    return fileNames.filter { it!!.contains(".input") }.toMutableList().toMutableStateList()
}
