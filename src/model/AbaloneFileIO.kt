package abalone.model

import abalone.AbaloneViewModel
import abalone.Piece
import java.io.File
import java.util.Date

/**
 * Static class that provides functions for reading, parsing, and writing .input, .board, and .move
 * files.
 */
class AbaloneFileIO private constructor() {

    companion object {
        private const val MAX_PIECES = 14
        private const val SAMPLE_MOVE_TIME = 30
        private const val BLACK_MOVE_REMAINING = 30
        private const val WHITE_MOVE_REMAINING = 31

        /**
         * Writes the given list of states to a text file.
         *
         * @param path a string
         * @param outputList a List<String>
         */
        fun writeDataFile(path: String, outputList: List<String>, callback: () -> Unit = { }) {
            File(path).writeText(outputList.joinToString("\n"))
            callback()
        }

        /**
         * Reads the given text file and returns the contents.
         *
         * @param path a string
         * @return file contents as a string
         */
        fun readDataFile(path: String): List<String>{
            val text = File(path).readLines()
            return text
        }

        /**
         * Parses the given board as a string into a StateRepresentation object.
         *
         * @param board as a string
         * @return board as a StateRepresentation object
         */
        fun parseState(currentPlayer: String, board: String): StateRepresentation {
            val marbleLayout: MutableMap<Coordinate, Piece> = mutableMapOf()
            val boardList: List<String> = board.split(",")
            var blackPieceCount = 0
            var whitePieceCount = 0
            for (cell in boardList) {
                val coordinate = Coordinate.get(
                    LetterCoordinate.convertLetter(cell.substring(0, 1)),
                    NumberCoordinate.convertNumber(cell.substring(1, 2))
                )
                val piece: Piece = Piece.convertPiece(cell.substring(2))
                if (piece == Piece.Black) blackPieceCount++
                if (piece == Piece.White) whitePieceCount++
                marbleLayout[coordinate] = piece
            }
            val marbleLayoutMap: Map<Coordinate, Piece> = marbleLayout.toMap()
            val boardState = BoardState(marbleLayoutMap)
            val blackPlayer = Player(MAX_PIECES - whitePieceCount, SAMPLE_MOVE_TIME)
            val whitePlayer = Player(MAX_PIECES - blackPieceCount, SAMPLE_MOVE_TIME)
            val players = mapOf(Piece.Black to blackPlayer, Piece.White to whitePlayer)
            val currentPlayerPiece = if (currentPlayer == "b") Piece.Black else Piece.White
            val movesRemaining = if (currentPlayer == "b") BLACK_MOVE_REMAINING else WHITE_MOVE_REMAINING
            return StateRepresentation(boardState, players, movesRemaining, currentPlayerPiece)
        }

        /**
         * Returns the given board as a string
         *
         * @param board a StateRepresentation object
         * @return board as a string
         */
        fun stringifyBoard(board: StateRepresentation): String {
            var boardList: MutableList<String> = mutableListOf()
            val currentBoard = board.getBoardState()
            val cells = currentBoard.cells
            cells.forEach { (coordinate, piece) ->
                if (piece != Piece.Empty && coordinate != Coordinate.offBoard) {
                    boardList.add("$coordinate$piece")
                }
            }
            boardList =  boardList.sortedWith(compareBy(
                { it[2] },
                { it[0] },
                { it[1].digitToInt() }
            )).toMutableList()
            val boardString = boardList.joinToString(",")
            return boardString
        }

        /**
         * Returns the given list of boards as a list of strings.
         *
         * @param boards List<StateRepresentation>
         * @return boards as a List<String>
         */
        fun stringifyBoards(boards: List<StateRepresentation>): List<String> {
            val stringBoards: MutableList<String> = mutableListOf()
            boards.forEach {
                stringBoards.add(stringifyBoard(it))
            }
            return stringBoards
        }

        /**
         * Returns the action as a string
         *
         * @param action an Action object
         * @return action as a string
         */
        fun stringifyAction(action: Action): String {
            return "${action.coordinates} ${action.direction}"
        }

        /**
         * Returns the given list of actions as a list of strings.
         *
         * @param actions List<Action>
         * @return actions as a List<String>
         */
        fun stringifyActions(actions: List<Action>): List<String> {
            val stringActions: MutableList<String> = mutableListOf()
            actions.forEach {
                stringActions.add(stringifyAction(it))
            }
            return stringActions
        }

        /**
         * Returns the move as a string
         *
         * @param move a MoveRecord object
         * @return move as a string
         */
        fun stringifyMove(move: AbaloneViewModel.MoveRecord): String {
            return move.movePath
        }

        /**
         * Returns the given list of moves as a list of strings.
         *
         * @param moves List<MoveRecord>
         * @return actions as a List<String>
         */
        fun stringifyMoves(moves: List<AbaloneViewModel.MoveRecord>): List<String> {
            val stringActions: MutableList<String> = mutableListOf()
            moves.forEach {
                stringActions.add(stringifyMove(it))
            }
            return stringActions
        }

        /**
         * Reads the board states in the string stored in a .board file.
         *
         * @param lines the string stored in the file.
         * @return a list of the board states described in the file.
         */
        fun readBoardsString(lines: List<String>): List<BoardState> {
            val boards: MutableList<BoardState> = mutableListOf()
            for (line in lines) {
                val board = BoardState()
                for (token in line.split(",").map { it.trim() }.filter { it.length == 3 }) {
                    val letter = LetterCoordinate.entries[token[0] - 'A' + 1]
                    val number = NumberCoordinate.entries[token[1] - '0']
                    val colour = if (token[2] == 'b') Piece.Black else Piece.White
                    board.cells[Coordinate.get(letter, number)] = colour
                }
                boards.add(board)
            }
            return boards.toList()
        }
    }
}
