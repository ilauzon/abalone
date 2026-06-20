package abalone.model.search

import abalone.Piece
import abalone.model.BoardState
import abalone.model.Coordinate
import abalone.model.LetterCoordinate
import abalone.model.NumberCoordinate
import abalone.model.StateRepresentation


/**
 * The heuristic function considers five factors that may affect the result:
 * 1. the total number of marbles for each player in every state; more marbles, more possibility to win
 * 2. if the marbles are more closer to center; this factor may be more important in early game which is a strategy to divide opponent's marbles to parts and more possible to push out opponent
 * 3. penalize closer to edge because marbles near edge are more vulnerable
 *
 * the heuristic function considers not only the state is good or not for the current player, but also for the opponent, which gives a relative evaluation to help maximize the current player's advantage and minimize the opponent's
 * the weights for each factor is changing based on the stage of the game. In early game, center control is more important; in late game the piece count is more important.
 */
class CarolHeuristic : Heuristic {

    private val boardCenter = Coordinate.get(LetterCoordinate.E, NumberCoordinate.FIVE)
    private val centerSum = boardCenter.letter.ordinal + boardCenter.number.ordinal

    override fun heuristic(state: StateRepresentation): Double {
        val weights = dynamicWeights(state.movesRemaining)

        val currentScore = weightedScore(state, weights)
        val opponentScore = weightedScore(
            StateRepresentation(
                board = state.board,
                players = state.players,
                movesRemaining = state.movesRemaining - 1,
                currentPlayer = state.currentPlayer.opposite()
            ),
            weights
        )

        return currentScore - opponentScore
    }

    private fun weightedScore(state: StateRepresentation, weights: Map<String, Double>): Double {
        val maxDistFromCenter = maxCenterDistance(state.board)
        return weights["pieceCountWeight"]!! * pieceDifference(state.board, state.currentPlayer) +
                weights["centerDistanceWeight"]!! * centerControl(state.board, state.currentPlayer, maxDistFromCenter) +
                weights["edgePenaltyWeight"]!! * edgePenalty(state.board)
    }

    private fun pieceDifference(board: BoardState, player: Piece): Double {
        val (black, white) = board.cells.values.fold(0 to 0) { (b, w), piece ->
            when (piece) {
                Piece.Black -> b + 1 to w
                Piece.White -> b to w + 1
                else -> b to w
            }
        }

        val diff = if (player == Piece.Black) black - white else white - black
        return ((diff + 6).coerceIn(0, 12)) / 12.0 * 10
    }

    private fun centerControl(board: BoardState, player: Piece, maxDist: Double): Double {
        return board.cells.entries.sumOf { (coord, piece) ->
            if (piece == player) {
                val dist = kotlin.math.abs(coord.letter.ordinal + coord.number.ordinal - centerSum).toDouble()
                (1.0 - dist / maxDist) * 10
            } else 0.0
        }
    }

    private fun edgePenalty(board: BoardState): Double {
        return board.cells.entries.sumOf { (coord, piece) ->
            if ((piece == Piece.Black || piece == Piece.White) && isOnDangerousEdge(coord)) {
                -10.0
            } else 0.0
        }
    }

    private fun isOnDangerousEdge(coord: Coordinate): Boolean {
        val l = coord.letter
        val n = coord.number
        return l == LetterCoordinate.I || l == LetterCoordinate.A ||
                (l == LetterCoordinate.H && (n == NumberCoordinate.FOUR || n == NumberCoordinate.NINE)) ||
                (l == LetterCoordinate.G && (n == NumberCoordinate.THREE || n == NumberCoordinate.NINE)) ||
                (l == LetterCoordinate.F && (n == NumberCoordinate.TWO || n == NumberCoordinate.NINE)) ||
                (l == LetterCoordinate.E && (n == NumberCoordinate.ONE || n == NumberCoordinate.NINE)) ||
                (l == LetterCoordinate.D && (n == NumberCoordinate.ONE || n == NumberCoordinate.EIGHT)) ||
                (l == LetterCoordinate.C && (n == NumberCoordinate.ONE || n == NumberCoordinate.SEVEN)) ||
                (l == LetterCoordinate.B && (n == NumberCoordinate.ONE || n == NumberCoordinate.SIX))
    }

    private fun maxCenterDistance(board: BoardState): Double {
        return board.cells.keys.maxOf {
            kotlin.math.abs(it.letter.ordinal + it.number.ordinal - centerSum).toDouble()
        }
    }

    private fun dynamicWeights(movesLeft: Int): Map<String, Double> = when {
        movesLeft > 30 -> mapOf(
            "pieceCountWeight" to 0.3,
            "centerDistanceWeight" to 0.5,
            "edgePenaltyWeight" to 0.2
        )
        movesLeft > 15 -> mapOf(
            "pieceCountWeight" to 0.4,
            "centerDistanceWeight" to 0.4,
            "edgePenaltyWeight" to 0.2
        )
        else -> mapOf(
            "pieceCountWeight" to 0.6,
            "centerDistanceWeight" to 0.3,
            "edgePenaltyWeight" to 0.1
        )
    }
}
