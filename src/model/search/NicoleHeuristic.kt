package abalone.model.search

import abalone.Piece
import abalone.model.LetterCoordinate
import abalone.model.NumberCoordinate
import abalone.model.StateRepresentation

/**
 * Nicole's heuristic implementation.
 *
 * This heuristic finds the distances that the players' marbles are from the center of the board.
 * The closer the current player's marbles are to the center, the higher their score, and vice versa
 * for the opposing player. The second part is that if there are more of the player's pieces than
 * the opposition's pieces on the board, that will increase the score, and vice versa again.
 * The final value is the player's score minus the opposition's score.
 *
 */
class NicoleHeuristic:Heuristic {

    // Define the center of the board
    private val center = Pair(LetterCoordinate.E, NumberCoordinate.FIVE)

    // Heuristic function to evaluate the board
    override fun heuristic(state: StateRepresentation): Double {
        var playerScore = 0.0
        var opponentScore = 0.0
        var playerPieces = 0
        var opponentPieces = 0
        for ((coordinate, piece) in state.board.cells.entries) {
            if (piece == state.currentPlayer) {
                // Calculate the distance from the center (Chebyshev distance)
                val distance = coordinate.findDistanceFrom(center.first, center.second)
                // Assign a score based on the distance (closer to center = higher score)
                playerScore += 1.0 / (1 + distance) // Inverse distance weighting
                playerPieces++
            } else if (piece != Piece.Empty) {
                // Calculate the distance for the opponent's marbles
                val distance = coordinate.findDistanceFrom(center.first, center.second)
                opponentScore += 1.0 / (1 + distance)
                opponentPieces++
            }
        }

        // Return the difference between player's score and opponent's score weighted with the number
        // of pieces
        return (playerScore - opponentScore) + 10 * (playerPieces - opponentPieces)
    }
}
