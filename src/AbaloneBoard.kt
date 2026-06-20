package abalone

/**
 * Represents the state of a cell on the game board.
 *
 * @property letter the Y coordinate of the cell.
 * @property number the X coordinate of the cell.
 * @property piece the contents of the cell.
 */
data class Cell(val letter: Char, val number: Int, var piece:Piece = Piece.Empty)

/**
 * Initializes the game board piece layout.
 *
 * @return the game board.
 */
fun createBoard(initialPosition: String): List<List<Cell>> {
    val letter = 'I' downTo 'A'
    val columnLength = listOf(5, 6, 7, 8, 9, 8, 7, 6, 5)
    val board = mutableListOf<List<Cell>>()

    for ((index, c) in letter.withIndex()) {
        val cells = mutableListOf<Cell>()
        val start = when(c) {
            'I' -> 5
            'H' -> 4
            'G' -> 3
            'F' -> 2
            else -> 1
        }
        for (i in start until (start + columnLength[index])) {
            if(initialPosition=="Standard") {
                val piece = when (c) {
                    'I', 'H' -> Piece.White
                    'G' -> if (i in 5..7) Piece.White else Piece.Empty
                    'A', 'B' -> Piece.Black
                    'C' -> if (i in 3..5) Piece.Black else Piece.Empty
                    else -> Piece.Empty
                }
                val cell = Cell(c, i, piece)
                cells.add(cell)
            }else if(initialPosition=="German Daisy") {
                val piece = when (c) {
                    'H' -> if (i in 4..5) Piece.White else if(i in 8..9) Piece.Black else Piece.Empty
                    'G' -> if (i in 3..5) Piece.White else if(i in 7..9) Piece.Black else Piece.Empty
                    'F' -> if (i in 3..4) Piece.White else if(i in 7..8) Piece.Black else Piece.Empty
                    'D' -> if (i in 2..3) Piece.Black else if(i in 6..7) Piece.White else Piece.Empty
                    'C' -> if (i in 1..3) Piece.Black else if(i in 5..7) Piece.White else Piece.Empty
                    'B' -> if (i in 1..2) Piece.Black else if(i in 5..6) Piece.White else Piece.Empty
                    else -> Piece.Empty
                }
                val cell = Cell(c, i, piece)
                cells.add(cell)
            }else{
                val piece = when (c) {
                    'I' -> if (i in 5..6) Piece.White else if(i in 8..9) Piece.Black else Piece.Empty
                    'H' -> if (i in 4..6) Piece.White else if(i in 7..9) Piece.Black else Piece.Empty
                    'G' -> if (i in 4..5) Piece.White else if(i in 7..8) Piece.Black else Piece.Empty

                    'C' -> if (i in 2..3) Piece.Black else if(i in 5..6) Piece.White else Piece.Empty
                    'B' -> if (i in 1..3) Piece.Black else if(i in 4..6) Piece.White else Piece.Empty
                    'A' -> if (i in 1..2) Piece.Black else if(i in 4..5) Piece.White else Piece.Empty
                    else -> Piece.Empty
                }
                val cell = Cell(c, i, piece)
                cells.add(cell)
            }
        }
        board.add(cells)
    }
    return board
}

