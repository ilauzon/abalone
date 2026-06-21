/**
 * Houses the state space generator.
 */

package abalone.model

class StateSpaceGenerator {

    companion object {
        var debug = false

        /**
         * The __Actions__ available to the agent from a given state.
         *
         * Defined formally in the text as
         * `Actions(s)` returning a finite set of actions that can be executed in state `s`.
         *
         * Implementation:
         *  1. put all current-player nodes that have at least one empty/opponent space as an immediate
         *  neighbour in a list P.
         *  2. for each node n in P, add all actions leading to moving n to an empty space to
         *  a list of actions A.
         *  3. for each node n in P, look at the current-player node neighbours, but only the ones that
         *  have a smaller hash value than n's. Add all empty-node moves to A. Look at the immediate
         *  opponent-node neighbours, and see if the next node in the line formed by those three nodes
         *  is empty. If so, add to A.
         *  4. for each node n in P, extend each two-node line that was blocked by a line of two
         *  opponent nodes, if there is another current-player node in that extension. If there is
         *  only a line of two opponent nodes there, add that action to A.
         *  5. return A.
         *
         * @param state the state representation.
         * @return a set of __valid__ actions to take from the given state.
         */
        fun actions(state: StateRepresentation): Set<Action> {
            val board = state.board.cells
            val actions = mutableSetOf<Action>()

            // filter player nodes
            val playerCoordinates = board.keys.filter { board[it] == state.currentPlayer }

            // add unary actions
            val unaryActions = mutableSetOf<Action>()
            for (coord in playerCoordinates) {
                for ((adjCoord, direction) in coord.adjacentCoordinates()) {
                    if (board[adjCoord] == Piece.Empty) { // if that cell is empty
                        unaryActions.add(Action(setOf(coord), direction))
                    }
                }
            }
            actions.addAll(unaryActions)

            // add non-sumito binary and ternary actions
            val inlineBinaryActions = mutableSetOf<Action>()
            val crossBinaryActions = mutableSetOf<Action>()
            val inlineTernaryActions = mutableSetOf<Action>()
            val crossTernaryActions = mutableSetOf<Action>()
            for (action in unaryActions) {
                val actionCoordinate = action.coordinates.first()
                val inlineCoordinate = actionCoordinate.move(action.direction.opposite())
                if (board[inlineCoordinate] == state.currentPlayer) {
                    inlineBinaryActions.add(
                        Action(
                            setOf(actionCoordinate, inlineCoordinate),
                            action.direction
                        )
                    )

                    // for ternary actions
                    val backInlineCoordinate = inlineCoordinate.move(action.direction.opposite())
                    if (board[backInlineCoordinate] == state.currentPlayer) {
                        inlineTernaryActions.add(
                            Action(
                                setOf(actionCoordinate, inlineCoordinate, backInlineCoordinate),
                                action.direction
                            )
                        )
                    }
                }
                val crossDirections = MoveDirection.entries.filter {
                    it != action.direction && it != action.direction.opposite()
                }
                for (crossDirection in crossDirections) {
                    val crossCoordinate = actionCoordinate.move(crossDirection)

                    /*
                        the hashcode is checked to prevent duplicate sets caused by checking cross-
                        neighbours from different coordinates in the same set. This works because
                        hash1 > hash2 and hash2 < hash1 occur in all the same conditions, and that
                        all hashes of coordinates should be unique.
                     */
                    if (
                        board[crossCoordinate] == state.currentPlayer
                        && actionCoordinate.hashCode() > crossCoordinate.hashCode()
                        && board[crossCoordinate.move(action.direction)] == Piece.Empty
                    ) {
                        crossBinaryActions.add(
                            Action(
                                setOf(actionCoordinate, crossCoordinate),
                                action.direction
                            )
                        )

                        // for ternary actions
                        val thirdCrossCoordinate = crossCoordinate.move(crossDirection)
                        if (
                            board[thirdCrossCoordinate] == state.currentPlayer
                            && board[thirdCrossCoordinate.move(action.direction)] == Piece.Empty
                        ) {
                            crossTernaryActions.add(
                                Action(
                                    setOf(actionCoordinate, crossCoordinate, thirdCrossCoordinate),
                                    action.direction
                                )
                            )
                        }
                    }
                }
            }
            actions.addAll(inlineBinaryActions)
            actions.addAll(crossBinaryActions)
            actions.addAll(inlineTernaryActions)
            actions.addAll(crossTernaryActions)

            // add sumito binary actions
            val sumitoBinaryActions = mutableSetOf<Action>()
            val sumitoTernaryActions = mutableSetOf<Action>()
            for (coord in playerCoordinates) {
                for ((coordAhead, direction) in coord.adjacentCoordinates()) {
                    val coordBehind = coord.move(direction.opposite())
                    // moving into the coordinate is a valid binary sumito if:
                    if (
                    // the adjacent piece is an opponent piece
                        board[coordAhead] == state.currentPlayer.opposite()
                        // the current piece has a piece of the current player's colour behind it
                        && board[coordBehind] == state.currentPlayer
                    ) {
                        val secondCoordAhead = coordAhead.move(direction)
                        val secondPieceAhead = board[secondCoordAhead]
                        val thirdCoordBehind = coordBehind.move(direction.opposite())
                        val thirdPieceBehind = board[thirdCoordBehind]
                        if (
                            secondPieceAhead == Piece.Empty
                            || secondPieceAhead == Piece.OffBoard
                        ) {
                            sumitoBinaryActions.add(
                                Action(
                                    setOf(coordBehind, coord, coordAhead),
                                    direction
                                )
                            )
                            if (thirdPieceBehind == state.currentPlayer) {
                                sumitoTernaryActions.add(
                                    Action(
                                        setOf(thirdCoordBehind, coordBehind, coord, coordAhead),
                                        direction
                                    )
                                )
                            }
                        }

                        // ternary sumitos
                        if (
                            secondPieceAhead == state.currentPlayer.opposite()
                            && thirdPieceBehind == state.currentPlayer
                        ) {
                            val thirdCoordAhead = secondCoordAhead.move(direction)
                            val thirdPieceAhead = board[thirdCoordAhead]
                            if (
                                thirdPieceAhead == Piece.Empty
                                || thirdPieceAhead == Piece.OffBoard
                            ) {
                                sumitoTernaryActions.add(
                                    Action(
                                        setOf(thirdCoordBehind, coordBehind, coord, coordAhead, secondCoordAhead),
                                        direction
                                    )
                                )
                            }
                        }
                    }
                }
            }
            actions.addAll(sumitoBinaryActions)
            if (debug) println(sumitoBinaryActions)
            actions.addAll(sumitoTernaryActions)

            return actions
        }

        /**
         * The __Transition Model__.
         *
         * Defined formally in the text as
         * `Result(s,a)` returning the state that results from doing action `a` in state `s`.
         *
         * @param state the state being transitioned from.
         * @param action the action being taken upon the state.
         * @return the state that was transitioned to.
         */
        fun result(state: StateRepresentation, action: Action): StateRepresentation {

            // clone the board
            val oldBoard = state.board.cells
            val newBoard = oldBoard.clone()
            var scoreAdded = 0

            val mappedCoordinates: List<Pair<Coordinate, Coordinate>> = action.coordinates.map {
                it to it.move(action.direction)
            }

            val oldCoordinates = mappedCoordinates.map { it.first }
            val newCoordinates = mappedCoordinates.map { it.second }

            for (coordinate in oldCoordinates) {
                // check if the score should be changed
                val fromCell = oldBoard[coordinate]
                val toCoord = coordinate.move(action.direction)
                if (fromCell == state.currentPlayer.opposite() && toCoord == Coordinate.offBoard) {
                    scoreAdded++
                }
                // make the move after checking if the score should be changed
                if (coordinate !in newCoordinates) newBoard[coordinate] = Piece.Empty
                val newCoordinate = coordinate.move(action.direction)
                newBoard[newCoordinate] = oldBoard[coordinate]
            }
            newBoard[Coordinate.offBoard] = Piece.OffBoard

            if (scoreAdded > 1) {
                throw IllegalArgumentException(
                    "The given action and state result in more than one" +
                            "opposition piece being pushed off the board, which is an invalid move."
                )
            }

            // set up the new state

            val oldCurrentPlayer = state.players[state.currentPlayer]!!
            val newCurrentPlayer = oldCurrentPlayer.copy(score = oldCurrentPlayer.score + scoreAdded)
            val newPlayers = hashMapOf(
                state.currentPlayer to newCurrentPlayer,
                state.currentPlayer.opposite() to state.players[state.currentPlayer.opposite()]!!.copy(),
            )
            val newState = StateRepresentation(
                BoardState(newBoard),
                newPlayers,
                state.movesRemaining - 1,
                if (state.currentPlayer == Piece.White) Piece.Black else Piece.White
            )

            return newState
        }

        fun expand(state: StateRepresentation, depth: Int = 1): List<Pair<Action, StateRepresentation>> {
            if (depth == 1) {
                return expandOnce(state)
            }
            val actionStates = expand(state, depth - 1)
            val newActionStates = mutableListOf<Pair<Action, StateRepresentation>>()
            for ((_, newState) in actionStates) {
                newActionStates.addAll(expandOnce(newState))
            }
            return newActionStates
        }

        private fun expandOnce(state: StateRepresentation): List<Pair<Action, StateRepresentation>> {
            val actions = actions(state)
            val actionStates = mutableListOf<Pair<Action, StateRepresentation>>()
            actions.forEach {
                val newState = result(state, it)
                actionStates.add(it to newState)
            }
            return actionStates
        }

        /**
         * The __Goal Test__.
         *
         * @param state the state being tested.
         * @return true if the state is a goal state, false otherwise.
         */
        fun goal(state: StateRepresentation): Boolean {
            TODO("define the goal test function")
        }
    }
}

/**
 * Represents an action available to the agent.
 *
 * @property coordinates the coordinates of the marbles to move.
 * @property direction the direction to move the marbles in.
 */
data class Action(val coordinates: Set<Coordinate>, val direction: MoveDirection) {
    override fun toString(): String {
        return "[${coordinates.joinToString(",")}]$direction"
    }
}
