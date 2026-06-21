package abalone.model.search

import abalone.model.BoardState
import abalone.model.Piece
import abalone.model.Coordinate
import abalone.model.LetterCoordinate
import abalone.model.NumberCoordinate
import abalone.model.StateRepresentation
import kotlin.math.abs

class CalvinHeuristic : Heuristic {
    override fun heuristic(state: StateRepresentation): Double {
        var factor1 = 0.0
        var factor2 = 0.0
        if (state.movesRemaining < 29) {
            factor1 = distanceToCenter(state.board) * 0.95
            factor2 = pieceCount(state.board, state) * 0.05
        } else {
            factor1 = distanceToCenter(state.board) * 0.2
            factor2 = pieceCount(state.board, state) * 0.8
        }

//        val factor4 = score(state) * scoreWeight
//        val factor5 = blitz(state) * blitzWeight

//        return factor1 + factor2 + factor3 + factor4 + factor5 + killshot(state.board)
        return factor1 + factor2 + killShot(state.board, state)
    }

    private fun distanceToCenter(board: BoardState): Double {
        val black = board.cells.filter { it.value == Piece.Black }
        val white = board.cells.filter { it.value == Piece.White }

        var blackDistance = 0
        black.forEach {
            blackDistance += hexDistance(it.key, Coordinate.get(LetterCoordinate.E, NumberCoordinate.FIVE))
        }
        var whiteDistance = 0
        white.forEach {
            whiteDistance += hexDistance(it.key, Coordinate.get(LetterCoordinate.E, NumberCoordinate.FIVE))
        }
        return (whiteDistance - blackDistance).toDouble()
    }

    private fun hexDistance(c1: Coordinate, c2: Coordinate): Int {
        val axial1 = toAxial(c1)
        val axial2 = toAxial(c2)
        return (abs(axial1.first - axial2.first) +
                abs(axial1.second - axial2.second) +
                abs(axial1.first + axial1.second - axial2.first - axial2.second)) / 2
    }

    private fun toAxial(c: Coordinate): Pair<Int, Int> {
        val q = c.number.ordinal - (c.letter.ordinal / 2)
        val r = c.letter.ordinal
        return Pair(q, r)
    }

    private fun pieceCount(board: BoardState, state: StateRepresentation): Double {
        var blackPieces = 0
        var whitePieces = 0
        board.cells.forEach {
            if (it.value == Piece.Black) {
                blackPieces++
            } else if (it.value == Piece.White) {
                whitePieces++
            }
        }
        return if (state.currentPlayer == Piece.Black) {
            (blackPieces - whitePieces).toDouble()
        } else {
            (whitePieces - blackPieces).toDouble()
        }
    }

    private fun killShot(board: BoardState, state: StateRepresentation): Double {
        val count = pieceCount(board, state)
        return when {
            count >= 6 -> 100000.0
            count <= -6 -> -100000.0
            else -> 0.0
        }
    }
}
