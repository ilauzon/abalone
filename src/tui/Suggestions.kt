package tui

import abalone.model.Action
import abalone.model.Coordinate
import abalone.model.MoveDirection
import abalone.model.StateRepresentation
import abalone.model.StateSpaceGenerator
import abalone.model.LetterCoordinate as L
import abalone.model.NumberCoordinate as N

private fun Action.line(): SelectionAxis? {
    if (coordinates.isEmpty()) return null
    val coordList = coordinates.toList()

    val letter = coordList[0].letter
    val number = coordList[0].number
    var lettersAreTheSame = true
    var numbersAreTheSame = true

    for (coord in coordList) {
        if (coord.letter != letter) lettersAreTheSame = false
        if (coord.number != number) numbersAreTheSame = false
    }
    if (lettersAreTheSame) {
        return SelectionAxis.X
    }

    if (numbersAreTheSame) {
        return SelectionAxis.Y
    }

    return SelectionAxis.Z
}

fun suggestions(state: StateRepresentation, str: String): MoveSuggestionSet {
    val suggestions = MoveSuggestionSet()
    val lexed = Lexer.move(str) ?: return suggestions
    val actions = StateSpaceGenerator.actions(state)
    val board = state.board.cells
    val parsedCoordinates = Parser.parseCoordinates(lexed) ?: return MoveSuggestionSet()

    suggestions.userInput.coordinates.addAll(parsedCoordinates)

    if (lexed.first != null) {
        suggestions.userInput.axisIndexer = lexed.first
        if (lexed.altMode) {
            suggestions.userInput.axis = SelectionAxis.Z
        } else if (Lexer.isLetter((lexed.first))) {
            suggestions.userInput.axis = SelectionAxis.X
        } else if (Lexer.isNumber(lexed.first)) {
            suggestions.userInput.axis = SelectionAxis.Y
            suggestions.userInput.axisIndexer = lexed.first
        }
    }

    outer@ for (action in actions) {
        val line = action.line()
        val actionCoords = action.coordinates
            .filter { board[it] == state.currentPlayer }
        if (!actionCoords.containsAll(parsedCoordinates)) continue

        if (lexed.first == null) {
            suggestions.suggestAltMode = true

            if (lexed.altMode) {
                suggestions.suggestAltMode = false
                for (pCoord in actionCoords) {
                    when (pCoord.side()) {
                        BoardSide.MIDDLE -> suggestions.suggestAltModeAxis = true
                        BoardSide.LEFT -> suggestions.letters.add(pCoord.alt().letter)
                        BoardSide.RIGHT -> suggestions.numbers.add(pCoord.alt().number)
                    }
                }
                continue
            }

            if (actionCoords.size <= 1) {
                suggestions.letters.addAll(actionCoords.map { it.letter })
                suggestions.numbers.addAll(actionCoords.map { it.number })
                continue
            }

            if (line == SelectionAxis.X) {
                suggestions.letters.addAll(actionCoords.map { it.letter })
            }

            if (line == SelectionAxis.Y) {
                suggestions.numbers.addAll(actionCoords.map { it.number })
            }

            continue
        }

        if (lexed.direction != null) {
            if (!parsedCoordinates.containsAll(actionCoords)) continue

            // if the user puts in left or right, suggest actions with the corresponding diagonal directions
            if (lexed.direction == MoveDirection.NegX && action.direction == MoveDirection.PosY || action.direction == MoveDirection.NegZ) {
                suggestions.directions.add(action.direction)
            }
            if (lexed.direction == MoveDirection.PosX && action.direction == MoveDirection.NegY || action.direction == MoveDirection.PosZ) {
                suggestions.directions.add(action.direction)
            }

            if (lexed.direction == action.direction) {
                suggestions.isCompleteMove = true
                suggestions.userInput.direction = action.direction
                return suggestions
            }
            continue
        }

        // i.e. the user entered up or down
        if (lexed.incompleteDirection != null) {
            if (!parsedCoordinates.containsAll(actionCoords)) continue
            if (lexed.incompleteDirection == Lexer.IncompleteDirection.Up) {
                if (action.direction == MoveDirection.PosY || action.direction == MoveDirection.PosZ) {
                    suggestions.directions.add(action.direction)
                }
            } else {
                if (action.direction == MoveDirection.NegY || action.direction == MoveDirection.NegZ) {
                    suggestions.directions.add(action.direction)
                }
            }
            continue
        }

        // suggest action directions when the user has not yet provided one
        if (parsedCoordinates.containsAll(actionCoords)) {
            suggestions.directions.add(action.direction)
            continue
        }

        /*
         * At this point, the user has not specified a direction, nor does the current action contain all the user's
         * given coordinates. They have provided at least one character.
         */

        if (lexed.altMode) {
            if (lexed.first == Characters.ALT_MODE_CHAR) {
                for (coord in actionCoords) {
                    // all the action's coordinates must be on the Z-axis if the first char is the alt mode char.
                    if (coord.side() != BoardSide.MIDDLE) {
                        continue@outer
                    }
                }
                suggestions.numbers.addAll(
                    actionCoords
                        .filter { !parsedCoordinates.contains(it) }
                        .map { it.number })
                suggestions.letters.addAll(
                    actionCoords
                        .filter { !parsedCoordinates.contains(it) }
                        .map { it.letter })
                continue
            }

            if (Lexer.isLetter(lexed.first)) {
                for (coord in actionCoords) {
                    if (coord.alt().letter != Lexer.toLetter(lexed.first)) {
                        continue@outer
                    }
                }
                suggestions.numbers.addAll(
                    actionCoords
                        .filter { !parsedCoordinates.contains(it) }
                        .map { it.alt().number })
                continue
            }

            if (Lexer.isNumber(lexed.first)) {
                for (coord in actionCoords) {
                    if (coord.alt().number != Lexer.toNumber(lexed.first)) {
                        continue@outer
                    }
                }
                suggestions.letters.addAll(
                    actionCoords
                        .filter { !parsedCoordinates.contains(it) }
                        .map { it.alt().letter })
                continue
            }

            continue
        }

        if (Lexer.isLetter(lexed.first) &&
            actionCoords.containsAll(parsedCoordinates) &&
            actionCoords.all { it.letter == Lexer.toLetter(lexed.first) }
        ) {
            suggestions.numbers.addAll(
                actionCoords
                    .filter { !parsedCoordinates.contains(it) }
                    .map { it.number })
            continue
        }

        if (Lexer.isNumber(lexed.first) &&
            actionCoords.containsAll(parsedCoordinates) &&
            actionCoords.all { it.number == Lexer.toNumber(lexed.first) }
        ) {
            suggestions.letters.addAll(
                actionCoords
                    .filter { !parsedCoordinates.contains(it) }
                    .map { it.letter })
            continue
        }
    }

    return suggestions
}

data class MoveSuggestionSet(
    val letters: MutableSet<L> = mutableSetOf(),
    val numbers: MutableSet<N> = mutableSetOf(),
    val directions: MutableSet<MoveDirection> = mutableSetOf(),
    val userInput: UserInput = UserInput(),
    var suggestAltMode: Boolean = false,
    var suggestAltModeAxis: Boolean = false,
    var isCompleteMove: Boolean = false,
) {
    data class UserInput(
        var axis: SelectionAxis? = null,
        var axisIndexer: Char? = null,
        val coordinates: MutableSet<Coordinate> = mutableSetOf(),
        var direction: MoveDirection? = null,
    )
    fun none(): Boolean {
        return letters.isEmpty() && numbers.isEmpty() && directions.isEmpty() && !isCompleteMove
    }
}
