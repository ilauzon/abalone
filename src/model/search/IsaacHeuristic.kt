package abalone.model.search

import abalone.model.*
import kotlin.math.abs

/**
 * Isaac's heuristic function implementation.
 */
class IsaacHeuristic : Heuristic {

    /**
     * Based on the ABLA agent, described in
     * "A Simple Intelligent Agent for Playing Abalone Game: ABLA" by
     * Ender Ozcan and Berk Hulagu, on pp 6-7.
     *
     * This heuristic function evaluates two features: closeness to the
     * board centre, and how grouped the pieces are.
     *
     * @param state the state being evaluated.
     * @return a value when higher is best for Max, and when lower is best for Min.
     */
    override fun heuristic(state: StateRepresentation): Double {
        val closeness = closenessToCentre(state.board) * CLOSENESS_WEIGHT
        val adjacency = adjacency(state.board) * ADJACENCY_WEIGHT
        val sum = closeness + adjacency
        return sum
    }

    companion object {
        private const val CLOSENESS_WEIGHT = 0.5
        private const val ADJACENCY_WEIGHT = 0.5
        private const val OFFBOARD_DISTANCE = 20

        /**
         * Computes how close the pieces are to the centre of the board, E5.
         * This algorithm uses the sum of the Manhattan distances of the marbles
         * on the board based on colour, and finds the difference between the
         * two.
         * <p>
         * A positive value means that black's pieces are closer to the centre, and
         * a negative value means that white's pieces are closer to the centre.
         *
         * @param board the board state being examined.
         * @return the sum of white's distance to the centre, minus the sum of black's
         * distance to the centre.
         */
        fun closenessToCentre(board: BoardState): Double {
            var blackSum = 0
            var whiteSum = 0
            var blackMarbleCount = 0
            var whiteMarbleCount = 0
            val middle = BoardMap.middle
            for (marble in board.cells.keys) {
                val piece = board.cells[marble]
                if (piece == Piece.Black) {
                    blackMarbleCount++
                    blackSum += dist(marble, middle)
                } else if (piece == Piece.White) {
                    whiteMarbleCount++
                    whiteSum += dist(marble, middle)
                }
            }

            blackSum += OFFBOARD_DISTANCE * (14 - blackMarbleCount)
            whiteSum += OFFBOARD_DISTANCE * (14 - whiteMarbleCount)

            return (whiteSum - blackSum).toDouble()
        }


        /**
         * Computes the number of moves to move a marble from one coordinate to another,
         * i.e. the Manhattan distance.
         *
         * The solution for the Manhattan distance in a hexagonal grid was taken from
         * this article by redblobgames.com:
         * [Hexagonal Grids](https://www.redblobgames.com/grids/hexagons/#distances-axial)
         *
         * @param c1 coordinate 1.
         * @param c2 coordinate 2.
         * @return the number of moves from c1 to c2.
         */
        fun dist(c1: Coordinate, c2: Coordinate): Int {
            val r1 = -c1.letter.ordinal
            val q1 = c1.number.ordinal
            val r2 = -c2.letter.ordinal
            val q2 = c2.number.ordinal

            return (abs(q1 - q2) + abs(q1 + r1 - q2 - r2) + abs(r1 - r2)) / 2
        }

        /**
         * Computes the overall adjacency advantage for Black. This is done by counting
         * the number of same-colour neighbours for Black and White, then subtracting
         * Black's count from White's.
         *
         * @param board the game board being examined.
         * @return
         */
        fun adjacency(board: BoardState): Int {
            var blackAdjacent = 0
            var whiteAdjacent = 0
            for (marble in board.cells.keys) {
                val piece = board.cells[marble]
                if (piece == Piece.Black || piece == Piece.White) {
                    val directionsToCheck = listOf(
                        marble.move(MoveDirection.NegZ),
                        marble.move(MoveDirection.NegY),
                        marble.move(MoveDirection.PosX),
                    )
                    for (direction in directionsToCheck) {
                        if (board.cells[direction] == piece) {
                            if (piece == Piece.Black) {
                                blackAdjacent += 2
                            } else {
                                whiteAdjacent += 2
                            }
                        }
                    }

                }
            }
            return blackAdjacent - whiteAdjacent
        }
    }
}
