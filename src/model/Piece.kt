package abalone.model

enum class Piece {
    Empty, Black, White, OffBoard;

    fun opposite() = when (this) {
        Empty -> Empty
        Black -> White
        White -> Black
        OffBoard -> OffBoard
    }

    companion object {
        fun convertPiece(colour: String): Piece {
            val pieceColour = when (colour) {
                "b" -> Black
                "w" -> White
                else -> Empty
            }
            return pieceColour
        }
    }

    override fun toString(): String {
        val piece = when (this) {
            Black -> "b"
            White -> "w"
            else -> ""
        }
        return piece
    }
}
