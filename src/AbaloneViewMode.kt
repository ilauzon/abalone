package abalone

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import abalone.model.AbaloneFileIO.Companion.parseState
import abalone.model.Action
import abalone.model.Coordinate
import abalone.model.*
import abalone.model.search.CalvinHeuristic
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import abalone.model.search.CarolHeuristic
import abalone.model.search.IsaacHeuristic
import abalone.model.search.NicoleHeuristic
import abalone.model.search.StateSearcher
import kotlinx.coroutines.delay

/**
 * Change Heuristic, please scroll down to AI move. Or search aiHeuristic1 and aiHeuristic2.
 * You may also want to change depth. You can find it in AImove1() and AImove2().
 */

//  In this class, blue related variable is P1(black), red related variable is P2(white)
class AbaloneViewModel : ViewModel() {
    var selectedLayout by mutableStateOf("Standard")

    var boardState = mutableStateOf(createBoard(selectedLayout))
    var currentPlayer = mutableStateOf(Piece.Black)
    var blueMoveNumber = mutableStateOf(0)
    var redMoveNumber = mutableStateOf(0)
    var bluePiecesTaken = mutableStateOf(0)
    var redPiecesTaken = mutableStateOf(0)
    var moveStartTime = mutableStateOf(System.currentTimeMillis())
    var moveDuration = mutableStateOf(0L)

    val totalTimePerPlayer = 30 * 60 * 1000L
    var blueTimeRemaining = mutableStateOf(totalTimePerPlayer)
    var redTimeRemaining = mutableStateOf(totalTimePerPlayer)

    var isPaused = MutableStateFlow(false)

    var p1TimeLimit by mutableStateOf(60f)
    var p1MaxTimePerTurn by mutableStateOf(60f)
    var p2TimeLimit by mutableStateOf(60f)
    var p2MaxTimePerTurn by mutableStateOf(60f)
    var timerJob: Job? = null


    var selectedMode by mutableStateOf("Bot Vs. Bot")
    var player1Color by mutableStateOf("Black")
    var moveLimit by mutableStateOf(40f)

    var pausedTimeRemaining = 0L
    var deepCopiedBoard = boardState.value.map { row -> row.map { it.copy() } }
    val moveHistory = mutableStateListOf<MoveRecord>()

    var botGameStarted by mutableStateOf(false)
    var switchPlayerJob: Job? = null
    var waitForHumanHelp by mutableStateOf(false)

    var isBlueWin by mutableStateOf(false)
    var isRedWin by mutableStateOf(false)


    data class BoardCycle(val blackState: BoardState, val whiteState: BoardState)

    private val recentCycles = mutableListOf<BoardCycle>()
    private var lastBlackBoard: BoardState? = null
    private var lastWhiteBoard: BoardState? = null


    fun resetGame() {
        selectedLayout = selectedLayout
        boardState.value = createBoard(selectedLayout)
        currentPlayer.value = Piece.Black
        blueMoveNumber.value = 0
        redMoveNumber.value = 0
        bluePiecesTaken.value = 0
        redPiecesTaken.value = 0
        blueTimeRemaining.value = totalTimePerPlayer
        redTimeRemaining.value = totalTimePerPlayer
        moveStartTime.value = System.currentTimeMillis()
        isPaused.value = false
        p1TimeLimit = p1MaxTimePerTurn
        println("p1TimeLimit after reset: $p1TimeLimit")
        p2TimeLimit = p2MaxTimePerTurn
        println("p2TimeLimit after reset: $p2TimeLimit")
        timerJob?.cancel()
        moveHistory.clear()
        botGameStarted = false
        switchPlayerJob?.cancel()
//        println(boardState)
    }

    fun pauseOrResumeGame() {
        val currentTime = System.currentTimeMillis()

        if (isPaused.value) {
            // Resume the game
            moveStartTime.value = currentTime
            if (currentPlayer.value == Piece.Black) {
                blueTimeRemaining.value = pausedTimeRemaining
            } else {
                redTimeRemaining.value = pausedTimeRemaining
            }
            if (selectedMode == "Bot Vs. Bot" && !waitForHumanHelp) {
                botGameStarted = true
                switchPlayer() // This will pick up AI turn again
            }

        } else {
            // Pause the game
            pausedTimeRemaining = if (currentPlayer.value == Piece.Black) {
                blueTimeRemaining.value - (currentTime - moveStartTime.value)
            } else {
                redTimeRemaining.value - (currentTime - moveStartTime.value)
            }
        }
        isPaused.value = !isPaused.value
    }

    data class MoveRecord(
        val previousState: List<List<Cell>>,
        val previousPlayer: Piece,
        val blueMoveNumber: Int,
        val redMoveNumber: Int,
        val bluePiecesTaken: Int,
        val redPiecesTaken: Int,
        val blueTimeRemaining: Long,
        val redTimeRemaining: Long,
        val movePath: String,
        val moveDuration: Long
    )

    fun saveGameState(movePath: String, moveDuration: Long) {

        moveHistory.add(
            MoveRecord(
                previousState = deepCopiedBoard.map { row -> row.map { it.copy() } },
                currentPlayer.value,
                blueMoveNumber.value,
                redMoveNumber.value,
                bluePiecesTaken.value,
                redPiecesTaken.value,
                blueTimeRemaining.value,
                redTimeRemaining.value,
                movePath,
                moveDuration
            )
        )
    }


    fun undoLastMove() {
        if (moveHistory.isNotEmpty()) {
            val lastMove = moveHistory.removeLast()
            deepCopiedBoard = lastMove.previousState.map { row -> row.map { it.copy() } }
            boardState.value = deepCopiedBoard

            currentPlayer.value = lastMove.previousPlayer
            blueMoveNumber.value = lastMove.blueMoveNumber
            redMoveNumber.value = lastMove.redMoveNumber
            bluePiecesTaken.value = lastMove.bluePiecesTaken
            redPiecesTaken.value = lastMove.redPiecesTaken
            blueTimeRemaining.value = lastMove.blueTimeRemaining
            redTimeRemaining.value = lastMove.redTimeRemaining

        }
    }

    /** this is for selecting marbles, including choose one or two or three.
    when choosing the second one, it will check if it is a neighbor cell. if not, it will clear the list.
    when choosing the third one, it will check if the third one is on the line and the three cells are in order. if not, it will clear the list.
     */
    fun selectMarbles(selectedCells: MutableList<Cell>, cell: Cell) {
        if (cell.piece != currentPlayer.value) {
            return
        } else if (isPaused.value) {
            return
        } else {
            when (selectedCells.size) {
                0 -> selectedCells.add(cell)
                1 -> handleSelection(
                    selectedCells,
                    cell
                ) { cells, newCell -> isCellNeighbor(cells.last(), newCell) }

                2 -> handleSelection(selectedCells, cell) { cells, newCell ->
                    isThirdCell(
                        cells,
                        newCell
                    )
                }

                else -> selectedCells.clear()
            }
        }
    }

    private fun handleSelection(
        selectedCells: MutableList<Cell>,
        cell: Cell,
        validation: (MutableList<Cell>, Cell) -> Boolean
    ) {
        if (validation(selectedCells, cell) && cell.piece == currentPlayer.value) {
            selectedCells.add(cell)
        } else {
            selectedCells.clear()
            selectedCells.add(cell)
        }
    }

    /**  Move marbles method includes move one piece to empty cell; and two or three
    pieces to empty cell and push opponent piece.
     */
    fun moveMarbles(selectedCells: MutableList<Cell>, targetCell: Cell) {
        if (waitForHumanHelp) {
            println("Human helped the AI. Resuming bot match.")
            waitForHumanHelp = false
            botGameStarted = true
        }

        if (selectedCells.isEmpty()) {
            return
        }


        moveDuration.value = System.currentTimeMillis() - moveStartTime.value
        if (currentPlayer.value == Piece.Black) {
            blueTimeRemaining.value -= moveDuration.value
        } else {
            redTimeRemaining.value -= moveDuration.value
        }


        val (letterDiff, numberDiff, moveOrder) = determineMoveDirection(
            selectedCells,
            targetCell
        ) ?: return

        if (targetCell.piece == Piece.Empty) {
            if (selectedCells.size == 1 && isCellNeighbor(selectedCells[0], targetCell)) {
                val movedPiece = selectedCells[0].piece
                val startPos =
                    "${selectedCells[0].letter}${selectedCells[0].number}${movedPiece.name[0]}"
                val endPos = "${targetCell.letter}${targetCell.number}${movedPiece.name[0]}"
                targetCell.piece = movedPiece
                selectedCells[0].piece = Piece.Empty
                val movePath = "[$startPos] -> [$endPos]"
                saveGameState(movePath, moveDuration.value)
                incrementMoveCount()
                selectedCells.clear()
                switchPlayer()
            } else if (selectedCells.size in 2..3) {
                val possibleMoveList =
                    twoOrThreeMarbleMovePossibilities(selectedCells, boardState.value)
                if (targetCell !in possibleMoveList) return
                performMove(moveOrder, letterDiff, numberDiff)
            }
        } else if (targetCell.piece != currentPlayer.value) {
            handlePushMove(selectedCells, targetCell, letterDiff, numberDiff, moveOrder)

        }
    }

    // This helper method check if the target cell is a neighbor cell of the first selected cell or the last selected cell.
    private fun determineMoveDirection(
        selectedCells: MutableList<Cell>,
        targetCell: Cell
    ): Triple<Int, Int, List<Cell>>? {
        if (selectedCells.isEmpty()) {
            return null
        }
        val letterDiff: Int
        val numberDiff: Int
        val moveOrder: List<Cell>

        return if (isCellNeighbor(selectedCells[0], targetCell)) {
            letterDiff = targetCell.letter - selectedCells[0].letter
            numberDiff = targetCell.number - selectedCells[0].number
            moveOrder = selectedCells
            Triple(letterDiff, numberDiff, moveOrder)
        } else if (isCellNeighbor(selectedCells.last(), targetCell)) {
            letterDiff = targetCell.letter - selectedCells.last().letter
            numberDiff = targetCell.number - selectedCells.last().number
            moveOrder = selectedCells.reversed()
            Triple(letterDiff, numberDiff, moveOrder)
        } else {
            null
        }
    }

    // This helper method moves the selected pieces to the target cells.
    private fun performMove(moveOrder: List<Cell>, letterDiff: Int, numberDiff: Int) {
        val movingPieces = moveOrder.map { it to it.piece }
        val startPositions = moveOrder.map { "${it.letter}${it.number}${it.piece.name[0]}" }
        movingPieces.forEach { (cell, _) -> cell.piece = Piece.Empty }

        val newPositions = mutableListOf<Pair<Cell, Cell>>()
        for ((cell, originalPiece) in movingPieces) {
            val newCell = boardState.value.flatten().find {
                it.letter == cell.letter + letterDiff && it.number == cell.number + numberDiff
            }

            if (newCell == null || (newCell.piece != Piece.Empty && newCell !in movingPieces.map { it.first })) {
                movingPieces.forEach { (oldCell, piece) -> oldCell.piece = piece }
                return
            } else {
                newPositions.add(cell to newCell)
            }
        }

        newPositions.reversed().forEach { (oldCell, newCell) ->
            newCell.piece = movingPieces.find { it.first == oldCell }?.second ?: Piece.Empty
        }

        val endPositions =
            newPositions.map { "${it.second.letter}${it.second.number}${it.second.piece.name[0]}" }
        val movePath =
            "[${startPositions.joinToString(", ")}] -> [${endPositions.joinToString(", ")}]"

        saveGameState(movePath, moveDuration.value)
        incrementMoveCount()
        switchPlayer()

    }

    // this method handles pushing opponent piece.
    private fun handlePushMove(
        selectedCells: MutableList<Cell>,
        targetCell: Cell,
        letterDiff: Int,
        numberDiff: Int,
        moveOrder: List<Cell>
    ) {
        val nextCell = boardState.value.flatten()
            .find { it.letter == targetCell.letter + letterDiff && it.number == targetCell.number + numberDiff }
        val nextnextCell = boardState.value.flatten()
            .find { it.letter == targetCell.letter + 2 * letterDiff && it.number == targetCell.number + 2 * numberDiff }

        if (selectedCells.size == 2) {
            if (nextCell == null) {
                targetCell.piece = Piece.Empty
                performMove(moveOrder, letterDiff, numberDiff)
                updatePiecesTaken()
            } else if (nextCell.piece == Piece.Empty) {
                nextCell.piece = targetCell.piece
                targetCell.piece = Piece.Empty
                performMove(moveOrder, letterDiff, numberDiff)
            }
        } else if (selectedCells.size == 3) {
            if (nextCell == null) {
                targetCell.piece = Piece.Empty
                performMove(moveOrder, letterDiff, numberDiff)
                updatePiecesTaken()
            } else if (nextCell.piece == Piece.Empty) {
                nextCell.piece = targetCell.piece
                targetCell.piece = Piece.Empty
                performMove(moveOrder, letterDiff, numberDiff)
            } else if (nextCell.piece == targetCell.piece) {
                if (nextnextCell == null) {
                    targetCell.piece = Piece.Empty
                    performMove(moveOrder, letterDiff, numberDiff)
                    updatePiecesTaken()
                } else if (nextnextCell.piece == Piece.Empty) {
                    nextnextCell.piece = targetCell.piece
                    nextCell.piece = targetCell.piece
                    targetCell.piece = Piece.Empty
                    performMove(moveOrder, letterDiff, numberDiff)
                }
            }
        }
    }

    private fun updatePiecesTaken() {
        if (currentPlayer.value == Piece.Black) redPiecesTaken.value++ else bluePiecesTaken.value++
        if (redPiecesTaken.value == 6) isRedWin = true
        if (bluePiecesTaken.value == 6) isBlueWin = true
    }

    private fun incrementMoveCount() {
        if (currentPlayer.value == Piece.Black) blueMoveNumber.value++ else redMoveNumber.value++
    }

    fun switchPlayer() {
        println("Switch")
        deepCopiedBoard = boardState.value.map { row -> row.map { it.copy() } }
        currentPlayer.value = if (currentPlayer.value == Piece.Black) Piece.White else Piece.Black
        // Only start timer immediately if a human is going next
        if ((selectedMode == "Human Vs. Human") ||
            (selectedMode == "Human Vs. Bot" && currentPlayer.value == Piece.Black) ||
            (selectedMode == "Bot Vs. Human" && currentPlayer.value == Piece.White)

        ) {
            moveStartTime.value = System.currentTimeMillis()
        }
        // solution for repeated moves
        val currentBoard = BoardState(
            boardState.value.flatten().associateBy {
                Coordinate.get(
                    LetterCoordinate.valueOf(it.letter.toString()),
                    NumberCoordinate.entries[it.number]
                )
            }.mapValues { it.value.piece }
        )

        if (currentPlayer.value == Piece.White) {
            lastBlackBoard = currentBoard
        } else if (currentPlayer.value == Piece.Black && lastBlackBoard != null) {
            lastWhiteBoard = currentBoard
            val cycle = BoardCycle(lastBlackBoard!!, lastWhiteBoard!!)

            if (recentCycles.size >= 5) recentCycles.removeFirst()
            recentCycles.add(cycle)

            val repetitions = recentCycles.count { it == cycle }
            if (repetitions >= 3) {
                println("!!!!!!Repeated move sequence 3 times — waiting for human assistance!")
                botGameStarted = false
                waitForHumanHelp = true
                return  // Stop the loop before triggering the next bot
            }
        }
        println("start to check mode")

        // Continue with AI turns if game is not paused or stopped
        switchPlayerJob?.cancel()
        switchPlayerJob = viewModelScope.launch {
            println("cancel jobs")
            println("mode:$selectedMode")
            delay(300)
            if (isPaused.value) return@launch

            // Only require botGameStarted for Bot vs. Bot mode
            if (selectedMode == "Bot Vs. Bot" && !botGameStarted) return@launch
            moveStartTime.value = System.currentTimeMillis()

            if ((selectedMode == "Bot Vs. Bot" && currentPlayer.value == Piece.Black) ||
                (selectedMode == "Bot Vs. Human" && currentPlayer.value == Piece.Black))
                {
                println("AI-1 move")
                AImove1(firstMove = false)
            }

            if (selectedMode == "Bot Vs. Bot" || selectedMode == "Human Vs. Bot") {
                println("mode verified")
                if (currentPlayer.value == Piece.White) {
                    println("AI-2 move")
                    AImove2()
                }
            }
        }
    }

    fun updateSettings(
        p1Time: Float,
        p2Time: Float,
        layout: String,
        mode: String,
        p1Color: String,
        moves: Float
    ) {
        p1TimeLimit = p1Time
        p1MaxTimePerTurn = p1Time
        p2TimeLimit = p2Time
        p2MaxTimePerTurn = p2Time
        selectedLayout = layout
        selectedMode = mode
        player1Color = p1Color
        moveLimit = moves
        boardState = mutableStateOf(createBoard(selectedLayout))
        resetGame()
    }

    /**
     * 1. test your own heuristic, choose human vs. bot, and in AbaloneViewModel file change aiHeuristic2 to your heuristic.
     * 2. compete with others, choose bot vs. bot mode, and in AbaloneViewModel file change both aiHeuristic1 and aiHeuristic2
     */

    //----AI move start from here---------------------------------------------
    // Black side. when choose bot vs. bot mode, need also change this to anther heuristic
    val aiHeuristic1 = IsaacHeuristic()
    val searcher1 = StateSearcher(aiHeuristic1)

    fun AImove1(firstMove: Boolean = false) {
        println("mode: $selectedMode")
        val start = System.currentTimeMillis()
        if (isPaused.value || !botGameStarted || waitForHumanHelp) return
        if ((selectedMode == "Bot Vs. Bot"&& currentPlayer.value == Piece.Black) ||
            (selectedMode == "Bot Vs. Human" && currentPlayer.value == Piece.Black)) {

            val pair = outputState(boardState.value, currentPlayer.value)
            val output = System.currentTimeMillis()
            println("AI-1 took ${output - start}ms output")
            val state = parseState(pair.first, pair.second)
            val parse = System.currentTimeMillis()
            println("AI-1 took ${parse - output}ms parse")
            val bestAction = searcher1.search(state, depth = 6, firstMove)
            println("AI-1 chose action: $bestAction")
            val mid = System.currentTimeMillis()
            println("AI-1 took ${mid - parse}ms search")
            applyAIMove(bestAction)
            val end = System.currentTimeMillis()
            println("AI-1 took ${end - mid}ms move")
        }
    }


    // White side, if choose human vs. bot, only change this to your heuristic.
    val aiHeuristic2 = IsaacHeuristic()
    val searcher2 = StateSearcher(aiHeuristic2)
    fun AImove2() {
        println("In search")
        val start = System.currentTimeMillis()
        if (isPaused.value || waitForHumanHelp) return
        if ((selectedMode == "Bot Vs. Bot" || selectedMode == "Human Vs. Bot") && currentPlayer.value == Piece.White) {
            val pair = outputState(boardState.value, currentPlayer.value)
            val output = System.currentTimeMillis()
            println("AI-2 took ${output - start}ms output")

            val state = parseState(pair.first, pair.second)
            val parse = System.currentTimeMillis()
            println("AI-2 took ${parse - output}ms parse")

            val bestAction = searcher2.search(state, depth = 6)

            val mid = System.currentTimeMillis()
            println("AI-2 took ${mid - parse}ms search")
            println("AI-2 chose action: $bestAction")
            applyAIMove(bestAction)
            val end = System.currentTimeMillis()
            println("AI-2 took ${end - mid}ms move")
        }
    }

    private fun applyAIMove(action: Action) {
        val (cells, target) = convertDirectionToTargetCell(action)
        if (cells != null && target != null) {
            println("target: $target")
            moveMarbles(cells.toMutableList(), target)

            // Allow UI to reflect move
            viewModelScope.launch {
                delay(400)
            }
        }
    }

    fun convertDirectionToTargetCell(action: Action): Pair<List<Cell>?, Cell?> {
        val board = boardState.value.flatten()
        val allCoordinates = action.coordinates
        if (allCoordinates.isEmpty()) return null to null

        val selectedCells = allCoordinates.mapNotNull { coord ->
            board.find { cell ->
                LetterCoordinate.valueOf(cell.letter.toString()) == coord.letter &&
                        NumberCoordinate.entries[cell.number] == coord.number &&
                        cell.piece == currentPlayer.value
            }
        }.sortedWith(compareBy({ it.letter }, { it.number }))

        if (selectedCells.isEmpty()) return null to null

        println("sorted selected cell: $selectedCells")

        val first = selectedCells.first()
        val last = selectedCells.last()
        val (diffLetter, diffNumber) = when (action.direction) {
            MoveDirection.PosX -> 0 to 1
            MoveDirection.NegX -> 0 to -1
            MoveDirection.PosY -> 1 to 0
            MoveDirection.NegY -> -1 to 0
            MoveDirection.PosZ -> 1 to 1
            MoveDirection.NegZ -> -1 to -1
        }

        val target: Cell? = when (selectedCells.size) {
            1 -> {
                val possible = oneMarbleMovePossibilities(first, boardState.value)
                board.find { it.letter == first.letter + diffLetter && it.number == first.number + diffNumber }
                    ?.takeIf { it in possible }
            }

            in 2..3 -> {
                val possible = twoOrThreeMarbleMovePossibilities(
                    selectedCells.toMutableList(),
                    boardState.value
                )
                val firstTarget =
                    board.find { it.letter == first.letter + diffLetter && it.number == first.number + diffNumber }
                val lastTarget =
                    board.find { it.letter == last.letter + diffLetter && it.number == last.number + diffNumber }
                when {
                    firstTarget in possible -> firstTarget
                    lastTarget in possible -> lastTarget
                    else -> null
                }
            }

            else -> null
        }

        return selectedCells to target

    }

//    fun startGame(){
//        if (selectedMode == "Bot Vs. Bot"){
//            botGameStarted = true
//            val showOnBoard = boardState.value.flatten()
//            val selectedCell = showOnBoard.filter {
//                (it.letter == 'I' && it.number == 7) || (it.letter == 'H' && it.number == 7) || (it.letter == 'G' && it.number == 7)
//            }.toMutableList()
//            val targetCell = showOnBoard.find { it.letter == 'F' && it.number == 7 } ?: return
//            moveMarbles(selectedCell, targetCell)
//        }
//    }

    fun startGame() {
        if (selectedMode == "Bot Vs. Bot" || selectedMode == "Bot Vs. Human") {
            botGameStarted = true
            moveStartTime.value = System.currentTimeMillis()
            AImove1(true)
        }
    }
}

