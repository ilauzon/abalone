package tui

import abalone.model.BoardState
import abalone.model.Piece

class Settings {
    companion object {
        const val GAME_VERSION = "v1.0.0"
        const val WIDTH = 80
        const val HEIGHT = 40
        const val UI_WIDTH = WIDTH - 2
        val botPiece get() = if (Settings.botGoesFirst) Piece.Black else Piece.White
        val humanPiece get() = if (Settings.botGoesFirst) Piece.White else Piece.Black

        var MaxSearchDepth = 7
        var botGoesFirst = true
        var maxMoves = 300
        var layout = BoardState.Layout.STANDARD
    }
}