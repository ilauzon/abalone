package tui

import abalone.model.*
import abalone.model.LetterCoordinate as L
import abalone.model.NumberCoordinate as N

class Parser {
    companion object {

        /**
         * Parse coordinates.
         *
         * @return the parsed coordinates in regular space, or null if there was a problem parsing.
         */
        fun parseCoordinates(lexed: Lexed): Set<Coordinate>? {
            if (lexed.first == null) {
                return hashSetOf()
            }

            val parsedCoordinates = hashSetOf<Coordinate>()
            if (lexed.altMode) {
                if (Characters.ALT_MODE_CHAR == lexed.first) {
                    parsedCoordinates.addAll(
                        lexed.rest.map { c ->
                            val ord: Int
                            if (Lexer.isLetter(c)) {
                                ord = Lexer.toLetter(c).ordinal
                            } else if (Lexer.isNumber(c)) {
                                ord = Lexer.toNumber(c).ordinal
                            } else {
                                return null
                            }

                            Coordinate.get(
                                L.entries[ord],
                                N.entries[ord],
                            )
                        }
                    )
                } else if (Lexer.isLetter(lexed.first)) {
                    try {
                        parsedCoordinates.addAll(
                            lexed.rest.map { c ->
                                SimpleCoordinate(Lexer.toLetter(lexed.first), Lexer.toNumber(c)).reg(BoardSide.LEFT)
                            }
                        )
                    } catch (e: IllegalArgumentException) {
                        return null
                    }
                } else if (Lexer.isNumber(lexed.first)) {
                    if (!Lexer.altModeNumbers.contains(Lexer.toNumber(lexed.first))) return null
                    try {
                        parsedCoordinates.addAll(
                            lexed.rest.map { c ->
                                SimpleCoordinate(Lexer.toLetter(c), Lexer.toNumber(lexed.first)).reg(BoardSide.RIGHT)
                            }
                        )
                    } catch (e: IllegalArgumentException) {
                        return null
                    }
                }
            } else {
                if (Lexer.isLetter(lexed.first)) {
                    try {
                        parsedCoordinates.addAll(lexed.rest.map { c ->
                            Coordinate.get(
                                Lexer.toLetter(lexed.first),
                                Lexer.toNumber(c)
                            )
                        })
                    } catch (e: IllegalArgumentException) {
                        return null
                    }
                } else if (Lexer.isNumber(lexed.first)) {
                    try {
                        parsedCoordinates.addAll(lexed.rest.map { c ->
                            Coordinate.get(
                                Lexer.toLetter(c),
                                Lexer.toNumber(lexed.first)
                            )
                        })
                    } catch (e: IllegalArgumentException) {
                        return null
                    }
                }
            }
            return parsedCoordinates
        }

        fun action(state: StateRepresentation, lexed: Lexed): Action? {
            val board = state.board.cells

            val parsedCoordinates = parseCoordinates(lexed) ?: return null

            for (action in StateSpaceGenerator.actions(state)) {
                if (action.coordinates
                        .filter { board[it] == state.currentPlayer }
                        .toSet() == parsedCoordinates.map { c -> Coordinate.get(c.letter, c.number) }.toSet()
                    && action.direction == lexed.direction
                ) {
                    return action
                }
            }
            return null
        }
    }
}
