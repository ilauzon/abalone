package abalone.model.search

import abalone.model.StateRepresentation

/**
 * Interface containing a heuristic function, which can be used in an agent's evaluation function.
 */
interface Heuristic {

    /**
     * The heuristic function. Determines a value representing closeness to a goal state.
     *
     * @param state the state that is being evaluated by the heuristic.
     * @return a number representing closeness to a goal state.
     */
    fun heuristic(state: StateRepresentation): Double
}
