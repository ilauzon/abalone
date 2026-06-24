package abalone.model.search

import abalone.model.Piece
import abalone.model.Action
import abalone.model.StateRepresentation
import abalone.model.StateSpaceGenerator.Companion.actions
import abalone.model.StateSpaceGenerator.Companion.expand
import abalone.model.StateSpaceGenerator.Companion.result
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min


/**
 * Responsible for performing search on a game tree. The game tree is generated dynamically.
 */
class StateSearcher(private val heuristic: Heuristic) {

    private data class Context(
        var actionChosen: Action? = null,
        val timeStarted: Long = System.currentTimeMillis(),
        var iterativeDepth: Int = STARTING_DEPTH,
        var ranOutOfTime: Boolean = false,
    )

    companion object {
        /** The maximum time per move, in milliseconds. */
        private const val MAX_MOVE_TIME = 5000

        /** The iterative deepening increased depth per iteration. */
        private const val DEPTH_STEP = 1

        /** The starting depth of search. */
        private const val STARTING_DEPTH = 1

        /**
         * Determines if the given state is a terminal state for minimax search.
         *
         * @param state the state being examined.
         * @return true if the state is terminal, false if otherwise.
         */
        fun terminalTest(state: StateRepresentation): Boolean {
            return state.players[state.currentPlayer]!!.score >= 6
                    || state.players[state.currentPlayer.opposite()]!!.score >= 6
                    || state.movesRemaining <= 0
        }
    }

    val cache = TranspositionTable(4_000_000)
    private val statesBeingSearched: ConcurrentHashMap<StateRepresentation, Int> = ConcurrentHashMap()
    val depths: MutableList<Int> = mutableListOf()

    /** DEBUG DATA */
    var cacheHits = 0
    var cacheMisses = 0

    /**
     * Performs minimax search with alpha-beta pruning.
     *
     * Returns the best action to take in the current state, which is dependent on the search
     * strategy.
     *
     * @param state the current state, which the returned action is acting on.
     * @param depth the maximum depth to search to.
     * @param firstMove if this is the first move of the agent.
     * @return the "best" action to take from that state.
     */
    fun search(state: StateRepresentation, depth: Int, firstMove: Boolean = false, threadCount: Int = 4): Action {

        if (depth < STARTING_DEPTH) {
            throw IllegalArgumentException("depth must be $STARTING_DEPTH or more for search to occur.")
        } else if (terminalTest(state)) {
            throw IllegalArgumentException("the given state is a terminal state. No action can be chosen.")
        }

        if (toMove(state) == Piece.Black && firstMove) {
            val states = expand(state)
            println("RANDOM FIRST MOVE")
            return states.random().first
        }

        for ((action, possibleWin) in expand(state)) {
            if (winFor(possibleWin) == state.currentPlayer) {
                println("Early return, immediate win found")
                return action
            }
        }

        val threads: MutableList<Thread> = mutableListOf()
        val actions: MutableList<Pair<Int?, Action?>> = mutableListOf()
        for (i in 0..<threadCount) {
            threads.add(thread(start = false) {
                actions.add(threadSearch(state, depth))
            })
        }
        for (thread in threads) {
            thread.start()
        }
        for (thread in threads) {
            thread.join()
        }
        var bestAction: Action? = null
        var bestDepth = 0
        for ((actionDepth, action) in actions) {
            if (actionDepth!! > bestDepth) {
                println(actionDepth)
                bestAction = action
                bestDepth = actionDepth
            }
        }
        println("Final action chosen at depth $bestDepth")
        return bestAction!!
    }

    /**
     * A search task for a single thread. The threading solution is based on the [Simplified ABDADA
    algorithm by Tom Kerrigan](http://www.tckerrigan.com/Chess/Parallel_Search/Simplified_ABDADA/).
     */
    private fun threadSearch(state: StateRepresentation, depth: Int): Pair<Int?, Action?> {
        val currentPlayer = toMove(state)
        var bestDepthAction: Pair<Int?, Action?> = null to null
        val context = Context(
            actionChosen = null,
            timeStarted = System.currentTimeMillis(),
            iterativeDepth = STARTING_DEPTH,
            ranOutOfTime = false,
        )

        // Black is Max because they move first.
        while (context.iterativeDepth <= depth && !context.ranOutOfTime) {
            if (currentPlayer == Piece.Black) {
                value(true, state, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, context.iterativeDepth, context)
            } else {
                value(false, state, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, context.iterativeDepth, context)
            }
            if (!context.ranOutOfTime) {
                println("Finished depth ${context.iterativeDepth}")
                bestDepthAction = context.iterativeDepth to context.actionChosen!!
                // if the last cycle took more than a quarter the time to crunch, don't bother trying another level.
                if (System.currentTimeMillis() - context.timeStarted > MAX_MOVE_TIME / 4) {
                    break
                }
            }
            context.iterativeDepth += DEPTH_STEP
        }

        println("Action depth: ${bestDepthAction.first}")
        depths.add(bestDepthAction.first!!)
        return bestDepthAction
    }

    /**
     * Combined Min-Value and Max-Value functions, controlled by the isMax flag.
     *
     *
     * This function writes to the actionChosen instance variable upon completing its recursive calls. The alternative
     * to this would have been passing around a reference to an action to every recursive call of value().
     *
     * @param isMax true if the calculation is Max-Value().
     * @param state the state being examined.
     * @param a alpha.
     * @param b beta.
     * @param depth the depth to search to.
     * @return the estimated utility of the given state.
     */
    private fun value(
        isMax: Boolean,
        state: StateRepresentation,
        a: Double,
        b: Double,
        depth: Int,
        context: Context
    ): Double {
        if (depth <= 0 || terminalTest(state)) {
            return eval(state)
        }

        val cachedValue = getCachedState(state, depth)
        if (cachedValue != null) {
            context.actionChosen = cachedValue.action
            return cachedValue.value
        }

        var v = if (isMax) Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY
        var bestAction: Action? = null
        var alpha = a
        var beta = b

        // ordering nodes, if isMax is true, sort in descending order, else sort in ascending order
        val sortedStates = actions(state)
            .map { action ->
                val newState = result(state, action)
                Triple(newState, eval(newState), action)
            }
            .sortedByDescending { if (isMax) it.second else -it.second }
            .toMutableList()

        val cachedEntry = getCachedState(state, 0)
        if (cachedEntry != null) {
            val indexOfBest = sortedStates.indexOfFirst {
                cachedEntry.action == it.third
            }
            if (indexOfBest != -1) {
                val element = sortedStates.removeAt(indexOfBest)
                sortedStates.add(0, element)
            }
        }

        if (sortedStates.isEmpty()) throw IllegalArgumentException(
            "Zero actions were generated from the non-terminal state."
        )

        var i = 0
        while (i < sortedStates.size) {
            val triple = sortedStates[i]
            val result = triple.first
            val action = triple.third
            if (i != 0) {
                if (statesBeingSearched[result] == null) {
                    statesBeingSearched[result] = depth
                } else {
                    sortedStates.remove(triple)
                    sortedStates.add(triple)
                    continue
                }
            }
            val newV = value(!isMax, result, alpha, beta, depth - 1, context)
            if (isMax && newV > v || !isMax && newV < v) {
                v = newV
                bestAction = action
            }
            if (
                isMax && v > beta
                || !isMax && v < alpha
                || outOfTime(context)
            ) {
                if (outOfTime(context)) context.ranOutOfTime = true
                cacheState(state, bestAction!!, v, depth)
                statesBeingSearched.remove(result)
                context.actionChosen = bestAction
                return v
            }
            if (isMax) {
                alpha = max(alpha, v)
            } else {
                beta = min(beta, v)
            }
            statesBeingSearched.remove(result)
            i++
        }

        cacheState(state, bestAction!!, v, depth)
        context.actionChosen = bestAction
        return v
    }

    private fun outOfTime(context: Context): Boolean {
        return System.currentTimeMillis() - context.timeStarted > (MAX_MOVE_TIME - 500)
    }

    private fun winFor(state: StateRepresentation): Piece? {
        val blackScore = state.players[Piece.Black]!!.score
        val whiteScore = state.players[Piece.White]!!.score
        if (blackScore >= 6) {
            return Piece.Black
        } else if (whiteScore >= 6) {
            return Piece.White
        } else if (state.movesRemaining == 0) {
            if (blackScore > whiteScore) return Piece.Black
            if (whiteScore > blackScore) return Piece.White
        }
        return null
    }

    /**
     * Estimates a state's utility value.
     *
     */
    private fun eval(state: StateRepresentation): Double {
        return heuristic.heuristic(state)
    }

    private fun cacheState(state: StateRepresentation, action: Action, value: Double, depth: Int) {
        cache[TranspositionTable.Key(
            state.board.cells,
            state.currentPlayer
        )] = TranspositionTable.Entry(value, action, depth)
    }

    private fun getCachedState(state: StateRepresentation, depth: Int): TranspositionTable.Entry? {
        val key = TranspositionTable.Key(state.board.cells, state.currentPlayer)
        val cachedValue = cache[key]
        if (cachedValue == null || cachedValue.depth < depth) {
            cacheMisses++
            return null
        } else {
            cacheHits++
            return cachedValue
        }
    }

    /**
     * Returns the colour whose move it currently is.
     *
     * @param state the state being examined.
     * @return Piece.Black or Piece.White.
     */
    private fun toMove(state: StateRepresentation): Piece {
        val currentPlayer = state.currentPlayer
        if (currentPlayer == Piece.Empty || currentPlayer == Piece.OffBoard) {
            throw IllegalArgumentException(
                "$currentPlayer is an invalid current player, " +
                        "the next move must be either Black or White."
            )
        }
        return currentPlayer
    }
}
