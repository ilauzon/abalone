package abalone

import abalone.model.*
import abalone.model.search.*

fun main(args: Array<String>) {
    val heuristic: Heuristic = NicoleHeuristic()
    val startAs = Piece.Black

    val state = StateRepresentation(
        board = BoardState(BoardState.Layout.STANDARD),
        movesRemaining = 100,
        currentPlayer = Piece.Black,
    )

    ConsoleOpponent().play(heuristic, state, startAs)
}

/**
 * A class designed for testing play against the agent.
 *
 */
private class ConsoleOpponent {

    fun play(heuristic: Heuristic, startState: StateRepresentation, humanColour: Piece = Piece.Black) {
        val searcher = StateSearcher(heuristic)
        var currentState = startState

        do {
            println(currentState.toStringPretty())
            if (currentState.currentPlayer == humanColour) {
                val move = getHumanMove(currentState)
                currentState = StateSpaceGenerator.result(currentState, move)
            } else {
                val move = getAgentMove(currentState, searcher)
                println()
                println("The agent has chosen a move: $move")
                println()
                currentState = StateSpaceGenerator.result(currentState, move)
            }
        } while (!searcher.terminalTest(currentState))

        println("Terminal state reached. \nEND")
    }

    private fun getHumanMove(state: StateRepresentation): Action {
        val actions = StateSpaceGenerator.actions(state)
        var validActionChosen = false
        var chosenAction: Action? = null
        while (!validActionChosen) {
            println("Actions Available:")

            var currentLine = ""
            var lineEntryCount = 0
            actions.forEach {
                currentLine += " ".repeat(lineEntryCount * 25 - currentLine.length)
                lineEntryCount++
                currentLine += it
                if (currentLine.length > 80) {
                    println(currentLine)
                    currentLine = ""
                    lineEntryCount = 0
                }
            }

            println()
            print("Your turn as ${state.currentPlayer}. copy/paste an action (press Enter for a random move): ")
            val input = readln()
            println()
            println()
            if (input == "") {
                chosenAction = actions.random()
            } else {
                chosenAction = actions.find { it.toString() == input }
            }
            if (chosenAction == null) {
                println("Not a valid action. Try again.")
            } else {
                validActionChosen = true
            }
        }

        return chosenAction!!
    }

    private fun getAgentMove(state: StateRepresentation, searcher: StateSearcher): Action {
        return searcher.search(state, 3)
    }
}
