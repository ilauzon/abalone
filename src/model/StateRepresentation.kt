package abalone.model

import abalone.model.LetterCoordinate as LetterC
import abalone.model.NumberCoordinate as NumberC

/**
 * The state representation of the game. Contains all the information needed for the search
 * and generation algorithms. Instances of this class may be passed between functions for state
 * space generation and search.
 *
 * Instances are mutable. When used for generation and/or search, it is the
 * responsibility of the dependent to make copies of instances when needed.
 */
class StateRepresentation(
    val board: BoardState,
    val players: Map<Piece, Player> = mapOf(
        Piece.Black to Player(0, 5000),
        Piece.White to Player(0, 5000),
    ),
    val movesRemaining: Int,
    val currentPlayer: Piece
) {
    init {
        if (players.keys.contains(Piece.Empty) || players.keys.contains(Piece.OffBoard)) {
            throw IllegalArgumentException(
                "Players can only be Black or White."
            )
        }

        if (movesRemaining % 2 != 0 && currentPlayer == Piece.Black) {
            throw IllegalArgumentException(
                "movesRemaining must be even on Black's turn, otherwise Black will have one more move than White."
            )
        } else if (movesRemaining % 2 == 0 && currentPlayer == Piece.White) {
            throw IllegalArgumentException(
                "movesRemaining must be odd on White's turn, otherwise Black will have one more move than White."
            )
        }
    }

    fun movesRemaining(colour: Piece): Int =
        when (colour) {
            Piece.Black -> movesRemaining / 2
            Piece.White -> movesRemaining / 2 + if (movesRemaining % 2 == 0) 0 else 1
            Piece.Empty -> throw IllegalArgumentException("Piece.Empty does not have moves.")
            Piece.OffBoard -> throw IllegalArgumentException("Piece.OffBoard does not have moves.")
        }

    fun getBoardState(): BoardState {
        return board
    }

    override fun toString(): String {
        val bl = players[Piece.Black]!!
        val wh = players[Piece.White]!!
        return """
        Black [${bl.score},${movesRemaining(Piece.Black)},${bl.moveTime}] 
        White [${wh.score},${movesRemaining(Piece.White)},${wh.moveTime}] 
        
        """.trimIndent() + board
    }

    /**
     * Pretty-print the game state.
     * @param black The character for black marbles.
     * @param white The character for white marbles.
     *
     * @return The string representation of the game state.
     */
    fun toStringPretty(black: Char = '◯', white: Char = '◉'): String {
        val bl = players[Piece.Black]!!
        val wh = players[Piece.White]!!
        return """
        Black (${black}): score = ${bl.score}, moves left = ${movesRemaining(Piece.Black)}, move time = ${bl.moveTime}
        White (${white}): score = ${wh.score}, moves left = ${movesRemaining(Piece.White)}, move time = ${wh.moveTime}
        
        """.trimIndent() + board.toStringPretty(black = black, white = white)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is StateRepresentation) return false
        return board == other.board
                && players[Piece.Black] == other.players[Piece.Black]
                && players[Piece.White] == other.players[Piece.White]
                && movesRemaining == other.movesRemaining
                && currentPlayer == other.currentPlayer

    }

    override fun hashCode(): Int {
        return board.hashCode() xor movesRemaining
    }
}

/**
 * Represents the state of the game board.
 */
class BoardState {

    /**
     * The game board. The value of a particular cell in accessed via a map, with the coordinates
     * as the key.
     */
    val cells: BoardMap

    constructor() {
        cells = BoardMap()
    }

    constructor(layout: Layout) {
        cells = BoardMap()
        when (layout) {
            Layout.STANDARD -> setBoard(generateStandardLayout())
            Layout.BELGIAN_DAISY -> setBoard(generateBelgianDaisyLayout())
            Layout.GERMAN_DAISY -> setBoard(generateGermanDaisyLayout())
        }
    }

    enum class Layout {
        STANDARD,
        BELGIAN_DAISY,
        GERMAN_DAISY,
    }

    constructor(board: Map<Coordinate, Piece>) {
        cells = BoardMap()
        setBoard(board)
    }

    constructor(boardMap: BoardMap) {
        cells = boardMap
    }

    companion object {
        private fun fillBoardWithEmpty(
            board: MutableMap<Coordinate, Piece>
        ) {
            for (l: LetterC in LetterC.entries.drop(1)) {
                for (n: NumberC in NumberC.entries.slice(l.min.ordinal..l.max.ordinal)) {
                    board[Coordinate.get(l, n)] = Piece.Empty
                }
            }
            board[Coordinate.offBoard] = Piece.OffBoard
        }

        private fun fillBoardLetter(
            board: MutableMap<Coordinate, Piece>,
            letter: LetterC,
            piece: Piece,
            range: Iterable<NumberC>
        ) {
            for (n: NumberC in NumberC.entries.slice(letter.min.ordinal..letter.max.ordinal)) {
                if (n in range) {
                    board[Coordinate.get(letter, n)] = piece
                }
            }
        }

        private fun generateStandardLayout(): Map<Coordinate, Piece> {
            val board = HashMap<Coordinate, Piece>()
            fillBoardWithEmpty(board)
            fillBoardLetter(board, LetterC.A, Piece.Black, LetterC.A.min..LetterC.A.max)
            fillBoardLetter(board, LetterC.B, Piece.Black, LetterC.B.min..LetterC.B.max)
            fillBoardLetter(board, LetterC.C, Piece.Black, LetterC.C.min + 2..LetterC.C.max - 2)
            fillBoardLetter(board, LetterC.G, Piece.White, LetterC.G.min + 2..LetterC.G.max - 2)
            fillBoardLetter(board, LetterC.H, Piece.White, LetterC.H.min..LetterC.H.max)
            fillBoardLetter(board, LetterC.I, Piece.White, LetterC.I.min..LetterC.I.max)
            return board
        }

        private fun generateBelgianDaisyLayout(): Map<Coordinate, Piece> {
            val board = HashMap<Coordinate, Piece>()
            fillBoardWithEmpty(board)
            fillBoardLetter(board, LetterC.A, Piece.Black, LetterC.A.min..LetterC.A.min + 1)
            fillBoardLetter(board, LetterC.B, Piece.Black, LetterC.B.min..LetterC.B.min + 2)
            fillBoardLetter(board, LetterC.C, Piece.Black, LetterC.C.min + 1..LetterC.C.min + 2)
            fillBoardLetter(board, LetterC.A, Piece.White, LetterC.A.max - 1..LetterC.A.max)
            fillBoardLetter(board, LetterC.B, Piece.White, LetterC.B.max - 2..LetterC.B.max)
            fillBoardLetter(board, LetterC.C, Piece.White, LetterC.C.max - 2..LetterC.C.max - 1)
            fillBoardLetter(board, LetterC.G, Piece.White, LetterC.G.min + 1..LetterC.G.min + 2)
            fillBoardLetter(board, LetterC.H, Piece.White, LetterC.H.min..LetterC.H.min + 2)
            fillBoardLetter(board, LetterC.I, Piece.White, LetterC.I.min..LetterC.I.min + 1)
            fillBoardLetter(board, LetterC.G, Piece.Black, LetterC.G.max - 2..LetterC.G.max - 1)
            fillBoardLetter(board, LetterC.H, Piece.Black, LetterC.H.max - 2..LetterC.H.max)
            fillBoardLetter(board, LetterC.I, Piece.Black, LetterC.I.max - 1..LetterC.I.max)
            return board
        }

        private fun generateGermanDaisyLayout(): Map<Coordinate, Piece> {
            val board = HashMap<Coordinate, Piece>()
            fillBoardWithEmpty(board)
            fillBoardLetter(board, LetterC.B, Piece.Black, LetterC.B.min..LetterC.B.min + 1)
            fillBoardLetter(board, LetterC.C, Piece.Black, LetterC.C.min..LetterC.C.min + 2)
            fillBoardLetter(board, LetterC.D, Piece.Black, LetterC.D.min + 1..LetterC.D.min + 2)
            fillBoardLetter(board, LetterC.B, Piece.White, LetterC.B.max - 1..LetterC.B.max)
            fillBoardLetter(board, LetterC.C, Piece.White, LetterC.C.max - 2..LetterC.C.max)
            fillBoardLetter(board, LetterC.D, Piece.White, LetterC.D.max - 2..LetterC.D.max - 1)
            fillBoardLetter(board, LetterC.H, Piece.White, LetterC.H.min..LetterC.H.min + 1)
            fillBoardLetter(board, LetterC.G, Piece.White, LetterC.G.min..LetterC.G.min + 2)
            fillBoardLetter(board, LetterC.F, Piece.White, LetterC.F.min + 1..LetterC.F.min + 2)
            fillBoardLetter(board, LetterC.H, Piece.Black, LetterC.H.max - 1..LetterC.H.max)
            fillBoardLetter(board, LetterC.G, Piece.Black, LetterC.G.max - 2..LetterC.G.max)
            fillBoardLetter(board, LetterC.F, Piece.Black, LetterC.F.max - 2..LetterC.F.max - 1)
            return board
        }
    }

    private fun setBoard(board: Map<Coordinate, Piece>) {
        for (key in board.keys) {
            this.cells[key] = board[key]!!
        }
    }

    override fun toString(): String {
        val letterRowToString = { letter: LetterC ->
            (letter.min..letter.max).joinToString(
                separator = ",",
                prefix = "[",
                postfix = "]",
                transform = {
                    val piece = cells[Coordinate.get(letter, it)]
                    when (piece) {
                        Piece.Empty -> "0"
                        Piece.Black -> "1"
                        Piece.White -> "2"
                        Piece.OffBoard -> " "
                    }
                }
            )
        }

        val returnString = """
                I ${letterRowToString(LetterC.I)} 
               H ${letterRowToString(LetterC.H)} 
              G ${letterRowToString(LetterC.G)} 
             F ${letterRowToString(LetterC.F)} 
            E ${letterRowToString(LetterC.E)} 
             D ${letterRowToString(LetterC.D)}9
              C ${letterRowToString(LetterC.C)}8
               B ${letterRowToString(LetterC.B)}7
                A ${letterRowToString(LetterC.A)}6
                    1 2 3 4 5
        """.trimIndent()

        return returnString
    }

    /**
     * Pretty-print the board.
     * @param black The character for black marbles.
     * @param white The character for white marbles.
     *
     * @return The string representation of the board.
     */
    fun toStringPretty(black: Char = '◯', white: Char = '◉'): String {
        val letterRowToString = { letter: LetterC ->
            val prefix = when (letter) {
                in LetterC.F..LetterC.I -> " "
                in LetterC.A..LetterC.D -> " "
                else -> " "
            }
            val postfix = when (letter) {
                in LetterC.F..LetterC.I -> " "
                in LetterC.A..LetterC.D -> " "
                else -> " "
            }
            (letter.min..letter.max).joinToString(
                separator = " ",
                prefix = prefix,
                postfix = postfix,
                transform = {
                    val piece = cells[Coordinate.get(letter, it)]
                    when (piece) {
                        Piece.Empty -> "∙"
                        Piece.Black -> black.toString()
                        Piece.White -> white.toString()
                        Piece.OffBoard -> " "
                    }
                }
            )
        }

        val returnString = """
                I${letterRowToString(LetterC.I)} 
               H${letterRowToString(LetterC.H)} 
              G${letterRowToString(LetterC.G)} 
             F${letterRowToString(LetterC.F)} 
            E${letterRowToString(LetterC.E)} 
             D${letterRowToString(LetterC.D)}9
              C${letterRowToString(LetterC.C)}8
               B${letterRowToString(LetterC.B)}7
                A${letterRowToString(LetterC.A)}6
                   1 2 3 4 5
        """.trimIndent()

        return returnString
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is BoardState) return false
        for (key in cells.keys) {
            if (cells[key] != other.cells[key]) return false
        }
        return true
    }

    override fun hashCode(): Int {
        return cells.hashCode()
    }
}

/**
 * Represents the data related to a player's state.
 *
 * @property score the number of pieces this player has taken. When this is 6, the player
 * has won the game.
 * @property moveTime the time this player has to move, in milliseconds.
 */
data class Player(
    // TODO make score a calculated value dependent on the number of pieces on the board in the StateRepresentation.
    val score: Int,
    val moveTime: Int,
)
