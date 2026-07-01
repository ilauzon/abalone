package tui

import abalone.model.Coordinate
import abalone.model.MoveDirection
import abalone.model.LetterCoordinate as L
import abalone.model.NumberCoordinate as N

class Lexer {

    enum class IncompleteDirection {
        Up,
        Down,
    }

    companion object {

        val altModeLetters = setOf(
            L.D,
            L.C,
            L.B,
            L.A,
        )

        val altModeNumbers = setOf(
            N.ONE,
            N.TWO,
            N.THREE,
            N.FOUR,
        )

        fun isLetter(c: Char): Boolean {
            return L.convertLetter(c.toString()) != L.NULL
        }

        fun isNumber(c: Char): Boolean {
            return N.convertNumber(c.toString()) != N.NULL
        }

        fun toNumber(c: Char): N {
            return N.convertNumber(c.toString())
        }

        fun toLetter(c: Char): L {
            return L.convertLetter(c.toString())
        }

        /**
         * Parse a move string.
         *
         * Examples:
         * - A1↖: Move A1 along positive Y.
         * - 1B↖: Move B1 along positive Y.
         * - B123→: Move B1, B2, and B3 along positive X.
         * - C345↙: Move C3, C4, and C5 along negative Z.
         * - 3CDE↙: Move C3, D3, and E3 along negative Z.
         * - //123↖: Move A1, B2, and C3 along positive Y.
         * - /A123↖: Move B1, C2, and D3 along positive Y.
         */
        fun move(str: String): Lexed? {
            if (str.isEmpty()) return Lexed(null, "", null, null, false)
            val altMode = str.isNotEmpty() && str[0] == Characters.ALT_MODE_CHAR
            val s = if (altMode) str.drop(1) else str
            if (s.isEmpty()) return Lexed(null, "", null, null, altMode)

            val first = s[0]
            var rest = ""
            val directionChars = listOf(
                Characters.UP,
                Characters.DOWN,
                Characters.LEFT,
                Characters.RIGHT,
                Characters.UP_LEFT,
                Characters.UP_RIGHT,
                Characters.DOWN_LEFT,
                Characters.DOWN_RIGHT
            )

            // parse marble selection
            var counter = 1
            if (altMode && Characters.ALT_MODE_CHAR == first) {
                val restOrdinals = mutableSetOf<Int>()
                while (counter < s.length && !directionChars.contains(s[counter])) {
                    if (rest.length >= 3 || rest.contains(s[counter])) return null
                    val ord: Int
                    if (isNumber(s[counter])) {
                        ord = toNumber(s[counter]).ordinal
                    } else if (isLetter(s[counter])) {
                        ord = toLetter(s[counter]).ordinal
                    } else {
                        return null
                    }
                    if (restOrdinals.contains(ord)) return null
                    restOrdinals.add(ord)
                    rest += s[counter]
                    counter++
                }
            } else if (isLetter(first)) {
                if (altMode && !altModeLetters.contains(toLetter(first))) {
                    return null
                }
                while (counter < s.length && !directionChars.contains(s[counter])) {
                    if (!isNumber(s[counter])) {
                        return null
                    }
                    if (rest.length >= 3 || rest.contains(s[counter])) return null
                    rest += s[counter]
                    counter++
                }
            } else if (isNumber(first)) {
                if (altMode && !altModeNumbers.contains(toNumber(first))) {
                    return null
                }
                while (counter < s.length && !directionChars.contains(s[counter])) {
                    if (!isLetter(s[counter])) {
                        return null
                    }
                    if (rest.length >= 3 || rest.contains(s[counter])) return null
                    rest += s[counter]
                    counter++
                }
            } else {
                return null
            }

            // parse direction
            if (counter >= s.length) {
                return Lexed(first, rest, null, null, altMode)
            } else if (counter < s.length - 1) {
                return null
            } else {
                var invalid = false
                var incompleteDirection: IncompleteDirection? = null
                val direction = when (s[counter]) {
                    Characters.LEFT -> MoveDirection.NegX
                    Characters.RIGHT -> MoveDirection.PosX
                    Characters.DOWN_RIGHT -> MoveDirection.NegY
                    Characters.UP_LEFT -> MoveDirection.PosY
                    Characters.DOWN_LEFT -> MoveDirection.NegZ
                    Characters.UP_RIGHT -> MoveDirection.PosZ
                    Characters.DOWN -> {
                        incompleteDirection = IncompleteDirection.Down
                        null
                    }

                    Characters.UP -> {
                        incompleteDirection = IncompleteDirection.Up
                        null
                    }

                    else -> {
                        invalid = true
                        null
                    }
                }

                if (invalid) {
                    return null
                }

                return Lexed(first, rest, direction, incompleteDirection, altMode)
            }
        }
    }
}

data class Lexed(
    val first: Char?,
    val rest: String,
    val direction: MoveDirection?,
    val incompleteDirection: Lexer.IncompleteDirection?,
    val altMode: Boolean,
)
