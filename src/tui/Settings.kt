package tui

import abalone.model.BoardState
import abalone.model.Piece

data class Settings(
    var MaxSearchDepth: Int = 7,
    var botGoesFirst: Boolean = true,
    var maxMoves: Int = 300,
    var layout: BoardState.Layout = BoardState.Layout.STANDARD,
) {
    val botPiece get() = if (botGoesFirst) Piece.Black else Piece.White
    val humanPiece get() = if (botGoesFirst) Piece.White else Piece.Black
    companion object {
        const val GAME_VERSION = "v1.0.0"
        const val WIDTH = 80
        const val HEIGHT = 40
        const val UI_WIDTH = WIDTH - 2
    }
}