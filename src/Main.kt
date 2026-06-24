import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.foundation.anim.*
import com.varabyte.kotter.foundation.input.*
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.foundation.text.ColorLayer.*
import com.varabyte.kotter.foundation.timer.*
import com.varabyte.kotter.runtime.render.*
import com.varabyte.kotter.runtime.terminal.TerminalSize
import com.varabyte.kotter.terminal.system.*
import com.varabyte.kotter.terminal.virtual.*
import com.varabyte.kotterx.grid.*
import com.varabyte.kotterx.text.*
import kotlin.random.Random
import abalone.model.*
import abalone.model.search.*

private const val WIDTH = 60
private const val HEIGHT = 20

private const val UP = '↑'
private const val DOWN = '↓'
private const val LEFT = '←'
private const val RIGHT = '→'
private const val UP_LEFT = '↖'
private const val UP_RIGHT = '↗'
private const val DOWN_LEFT = '↙'
private const val DOWN_RIGHT = '↘'

private const val ALT_MODE_CHAR = 'Z'

private const val botGoesFirst = true
private val botPiece = if (botGoesFirst) Piece.Black else Piece.White
private val humanPiece = if (botGoesFirst) Piece.White else Piece.Black

fun RenderScope.blackMarble() = green { text('◯') }
fun RenderScope.whiteMarble() = green { text('◉') }
fun RenderScope.emptySpot() = green { text('∙') }

fun RenderScope.humanColour(scopedBlock: RenderScope.() -> Unit) = blue { scopedBlock() }
fun RenderScope.botColour(scopedBlock: RenderScope.() -> Unit) = green { scopedBlock() }
fun RenderScope.boardColour(scopedBlock: RenderScope.() -> Unit) = white { scopedBlock() }
fun RenderScope.selected(scopedBlock: RenderScope.() -> Unit) = red(layer = BG) { black { scopedBlock() } }
fun RenderScope.highlighted(scopedBlock: RenderScope.() -> Unit) = white(layer = BG) { black { scopedBlock() } }

fun isLetter(c: Char): Boolean {
    return LetterCoordinate.convertLetter(c.toString()) != LetterCoordinate.NULL
}

fun isNumber(c: Char): Boolean {
    return NumberCoordinate.convertNumber(c.toString()) != NumberCoordinate.NULL
}

fun isDirectionSign(c: Char): Boolean {
    return listOf('-', '+').contains(c)
}

fun isDirectionAxis(c: Char): Boolean {
    return listOf('X', 'Y', 'Z').contains(c)
}

fun toNumber(c: Char): NumberCoordinate {
    return NumberCoordinate.convertNumber(c.toString())
}

fun toLetter(c: Char): LetterCoordinate {
    return LetterCoordinate.convertLetter(c.toString())
}

fun toCoordinate(str: String): Coordinate? {
    val letter = toLetter(str[0])
    val number = toNumber(str[1])

    if (letter == LetterCoordinate.NULL || number == NumberCoordinate.NULL) {
        return null
    }

    val coordinate: Coordinate

    try {
        coordinate = Coordinate.get(letter, number)
    } catch (e: IllegalArgumentException) {
        return null
    }

    if (coordinate == Coordinate.offBoard) {
        return null
    }

    return coordinate
}

fun toMoveDirection(str: String): MoveDirection? {
    return MoveDirection.entries.find { it.toString() == str }
}

enum class IncompleteDirection {
    Up,
    Down,
}

data class ParseResult(
    val first: Char?,
    val rest: String,
    val direction: MoveDirection?,
    val incompleteDirection: IncompleteDirection?,
    val altMode: Boolean,
)

/**
 * Parses a move string. 
 * 
 * Examples:
 * - A1↖: Move A1 along positive Y.
 * - 1B↖: Move B1 along positive Y.
 * - B123→: Move B1, B2, and B3 along positive X.
 * - C345↙: Move C3, C4, and C5 along negative Z.
 * - 3CDE↙: Move C3, D3, and E3 along negative Z.
 * - ZZ123↖: Move A1, B2, and C3 along positive Y.
 * - ZA123↖: Move B1, C2, and D3 along positive Y.
 */
fun parseMove(str: String): ParseResult? {
    if (str.isEmpty()) return ParseResult(null, "", null, null, false)
    val altMode = str.isNotEmpty() && str[0] == ALT_MODE_CHAR
    val s = if (altMode) str.drop(1) else str
    if (s.isEmpty()) return ParseResult(null, "", null, null, altMode)
    if (s.length == 1) return ParseResult(s[0], "", null, null, false)

    val first = s[0]
    var rest = ""
    val directionChars = listOf(UP, DOWN, LEFT, RIGHT, UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT)
    val incompleteDirectionChars = listOf(UP, DOWN)

    val altModeLetters = setOf(
        LetterCoordinate.D,
        LetterCoordinate.C,
        LetterCoordinate.B,
        LetterCoordinate.A,
    )

    val altModeNumbers = setOf(
        NumberCoordinate.ONE,
        NumberCoordinate.TWO,
        NumberCoordinate.THREE,
        NumberCoordinate.FOUR,
    )

    var counter = 1
    if (altMode && ALT_MODE_CHAR == first) {
        while (counter < s.length && !directionChars.contains(s[counter])) {
            if (rest.length >= 3 || rest.contains(s[counter])) return null
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

    if (counter >= s.length) {
        return ParseResult(first, rest, null, null, altMode)
    } else if (counter < s.length - 1) {
        return null
    } else {
        var invalid = false
        var incompleteDirection: IncompleteDirection? = null
        val direction = when (s[counter]) {
            LEFT -> MoveDirection.NegX
            RIGHT -> MoveDirection.PosX
            DOWN_RIGHT -> MoveDirection.NegY
            UP_LEFT -> MoveDirection.PosY
            DOWN_LEFT -> MoveDirection.NegZ
            UP_RIGHT -> MoveDirection.PosZ
            DOWN -> {
                incompleteDirection = IncompleteDirection.Down
                null
            }

            UP -> {
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

        return ParseResult(first, rest, direction, incompleteDirection, altMode)
    }
}

fun MoveDirection.toArrow(): Char {
    return when (this) {
        MoveDirection.NegX -> LEFT
        MoveDirection.PosX -> RIGHT
        MoveDirection.NegY -> DOWN_RIGHT
        MoveDirection.PosY -> UP_LEFT
        MoveDirection.NegZ -> DOWN_LEFT
        MoveDirection.PosZ -> UP_RIGHT
    }

}

data class MoveSuggestions(
    val letters: MutableSet<LetterCoordinate> = mutableSetOf(),
    val numbers: MutableSet<NumberCoordinate> = mutableSetOf(),
    val directions: MutableSet<MoveDirection> = mutableSetOf(),
    var isCompleteMove: Boolean = false
) {

    fun none(): Boolean {
        return letters.isEmpty() && numbers.isEmpty() && directions.isEmpty() && !isCompleteMove
    }
}

fun getNextMoveSuggestions(state: StateRepresentation, str: String): MoveSuggestions {
    val suggestions = MoveSuggestions()
    val parsed = parseMove(str) ?: return suggestions
    val actions = StateSpaceGenerator.actions(state)
    val board = state.board.cells
    val parsedCoordinates = hashSetOf<Coordinate>()

    try {
        if (parsed.first != null) {
            if (isLetter(parsed.first)) {
                parsedCoordinates.addAll(parsed.rest.map { c -> Coordinate.get(toLetter(parsed.first), toNumber(c)) })
            } else if (isNumber(parsed.first)) {
                parsedCoordinates.addAll(parsed.rest.map { c -> Coordinate.get(toLetter(c), toNumber(parsed.first)) })
            }
        }
    } catch (e: IllegalArgumentException) { // for Coordinate.get
        return MoveSuggestions()
    }


    for (action in actions) {
        val playerCoords = action.coordinates.filter { board[it] == state.currentPlayer }
        if (!playerCoords.containsAll(parsedCoordinates)) continue

        if (parsed.first == null) {
            if (action.direction == MoveDirection.PosX || action.direction == MoveDirection.NegX) {
                suggestions.letters.addAll(playerCoords.map { it.letter })
            }
            if (action.direction == MoveDirection.PosY || action.direction == MoveDirection.NegY) {
                suggestions.numbers.addAll(playerCoords.map { it.number })
            }
        } else if (isLetter(parsed.first) || isNumber(parsed.first)) {
            if (parsed.direction != null) {
                if (!parsedCoordinates.containsAll(playerCoords)) continue
                if (parsed.direction == action.direction) {
                    suggestions.isCompleteMove = true
                    return suggestions
                }
                if (parsed.direction == MoveDirection.PosX || parsed.direction == MoveDirection.NegX) {
                    if (parsed.direction == MoveDirection.NegX) {
                        if (action.direction == MoveDirection.PosY || action.direction == MoveDirection.NegZ) {
                            suggestions.directions.add(action.direction)
                        }
                    } else {
                        if (action.direction == MoveDirection.NegY || action.direction == MoveDirection.PosZ) {
                            suggestions.directions.add(action.direction)
                        }
                    }
                }
            } else if (parsed.incompleteDirection != null) {
                if (!parsedCoordinates.containsAll(playerCoords)) continue
                if (parsed.incompleteDirection == IncompleteDirection.Up) {
                    if (action.direction == MoveDirection.PosY || action.direction == MoveDirection.PosZ) {
                        suggestions.directions.add(action.direction)
                    }
                } else {
                    if (action.direction == MoveDirection.NegY || action.direction == MoveDirection.NegZ) {
                        suggestions.directions.add(action.direction)
                    }
                }
            } else {
                if (parsedCoordinates.containsAll(playerCoords)) {
                    suggestions.directions.add(action.direction)
                } else {
                    if (isLetter(parsed.first) && playerCoords.all { it.letter == toLetter(parsed.first) }) {
                        suggestions.numbers.addAll(
                            playerCoords
                                .filter { !parsedCoordinates.contains(it) }
                                .map { it.number })
                    } else if (isNumber(parsed.first) && playerCoords.all { it.number == toNumber(parsed.first) }) {
                        suggestions.letters.addAll(
                            playerCoords
                                .filter { !parsedCoordinates.contains(it) }
                                .map { it.letter }
                        )
                    }
                }
            }
        }
    }

    return suggestions
}

fun getMove(state: StateRepresentation, str: String): Action? {
    val parsed = parseMove(str) ?: return null
    val board = state.board.cells

    val parsedCoordinates = hashSetOf<Coordinate>()
    if (parsed.first != null) {
        if (isLetter(parsed.first)) {
            parsedCoordinates.addAll(parsed.rest.map { c -> Coordinate.get(toLetter(parsed.first), toNumber(c)) })
        } else if (isNumber(parsed.first)) {
            parsedCoordinates.addAll(parsed.rest.map { c -> Coordinate.get(toLetter(c), toNumber(parsed.first)) })
        }
    }

    for (action in StateSpaceGenerator.actions(state)) {
        if (action.coordinates
                .filter { board[it] == state.currentPlayer }
                .toSet() == parsedCoordinates.toSet()
            && action.direction == parsed.direction
        ) {
            return action
        }
    }
    return null
}

fun Coordinate.isSelected(
    selectedLetters: Set<Char>,
    selectedNumbers: Set<Char>,
    altMode: Boolean = false
): Boolean {
    return selectedLetters.contains(letter.toString()[0]) && selectedNumbers.contains(number.toString()[0])
}

fun Coordinate.isHighlighted(
    axisChar: Char,
    altMode: Boolean = false
): Boolean {
    if (axisChar == ALT_MODE_CHAR) {
        return letter.ordinal == number.ordinal
    } else if (isLetter(axisChar)) {
        return letter == toLetter(axisChar)
    } else if (isNumber(axisChar)) {
        return number == toNumber(axisChar)
    }

    return false
}

fun RenderScope.drawBoard(
    game: StateRepresentation,
    selectedLetters: Set<Char>,
    selectedNumbers: Set<Char>,
    axisChar: Char?,
    altMode: Boolean = false,
) {

    fun RenderScope.letterRowToString(letter: LetterCoordinate) {
        val prefix = when (letter) {
            in LetterCoordinate.F..LetterCoordinate.I -> " "
            in LetterCoordinate.A..LetterCoordinate.D -> " "
            else -> " "
        }
        val postfix = when (letter) {
            in LetterCoordinate.F..LetterCoordinate.I -> " "
            in LetterCoordinate.A..LetterCoordinate.D -> " "
            else -> " "
        }

        fun Coordinate.render(c: Char) {
            if (isSelected(selectedLetters, selectedNumbers, altMode)) {
                selected {
                    if (axisChar != null && isHighlighted(axisChar, altMode)) {
                        if (toNumber(axisChar) == number) {
                            text(letter.toString())
                        } else {
                            text(number.toString())
                        }
                    } else {
                        text(c)
                    }
                }
            } else if (axisChar != null && isHighlighted(axisChar, altMode)) {
                highlighted {
                    if (toNumber(axisChar) == number) {
                        text(letter.toString())
                    } else {
                        text(number.toString())
                    }
                }
            } else {
                text(c)
            }
        }

        for (l in letter.min..letter.max) {
            val coord = Coordinate.get(letter, l)
            val piece = game.board.cells[coord]
            when (piece) {
                Piece.Empty -> {
                    text('∙')
                }

                Piece.Black -> {
                    if (botPiece == Piece.Black) {
                        botColour {
                            text('O')
                        }
                    } else {
                        humanColour {
                            coord.render('O')
                        }
                    }
                }

                Piece.White -> {
                    if (botPiece == Piece.White) {
                        botColour {
                            text('@')
                        }
                    } else {
                        humanColour {
                            coord.render('@')
                        }
                    }
                }

                Piece.OffBoard -> {
                    text(' ')
                }
            }
            text(' ')
        }
    }

    fun RenderScope.number(c: Char) {
        if (selectedNumbers.contains(c)) {
            highlighted { text(c) }
        } else {
            boardColour { text(c) }
        }
    }

    fun RenderScope.letter(c: Char) {
        if (selectedLetters.contains(c)) {
            highlighted { text(c) }
        } else {
            boardColour { text(c) }
        }
    }

    text("    "); letter('I'); text(' '); letterRowToString(LetterCoordinate.I); textLine()
    text("   "); letter('H'); text(' '); letterRowToString(LetterCoordinate.H); textLine()
    text("  "); letter('G'); text(' '); letterRowToString(LetterCoordinate.G); textLine()
    text(" "); letter('F'); text(' '); letterRowToString(LetterCoordinate.F); textLine()
    text(""); letter('E'); text(' '); letterRowToString(LetterCoordinate.E); textLine()
    text(" "); letter('D'); text(' '); letterRowToString(LetterCoordinate.D); number('9'); textLine()
    text("  "); letter('C'); text(' '); letterRowToString(LetterCoordinate.C); number('8'); textLine()
    text("   "); letter('B'); text(' '); letterRowToString(LetterCoordinate.B); number('7'); textLine()
    text("    "); letter('A'); text(' '); letterRowToString(LetterCoordinate.A); number('6'); textLine()
    text("     "); letter(ALT_MODE_CHAR); text(' '); number('1'); text(' '); number('2'); text(' '); number('3'); text(
        ' '
    ); number(
        '4'
    ); text(
        ' '
    ); number(
        '5'
    )
}

fun main() {
    session(
        terminal = listOf(
            { SystemTerminal() },
            { VirtualTerminal.create(terminalSize = TerminalSize(WIDTH, HEIGHT + 15)) }
        ).firstSuccess(),
        clearTerminal = true,
    ) {

        var game = StateRepresentation(
            BoardState(BoardState.Layout.BELGIAN_DAISY),
            movesRemaining = 300,
            currentPlayer = Piece.Black
        )
        val maxDepth = 5
        val bot = StateSearcher(IsaacHeuristic())
        var status = ""
        var botTurn = botGoesFirst
        var firstMove = true
        var gameOver = false
        var suggestions = MoveSuggestions()
        var timerKey = Any()
        var inputStr = ""
        var altMode = false
        var axisChar: Char? = null

        var selectedLetters = setOf<Char>()
        var selectedNumbers = setOf<Char>()

        section {
            grid(Cols { fit(); fit() }) {
                cell {
                    drawBoard(game, selectedLetters, selectedNumbers, axisChar, altMode)
                }
                cell {
                    botColour {
                        textLine("Bot score: ${game.players[botPiece]!!.score}")
                    }
                    humanColour {
                        textLine("Human score: ${game.players[humanPiece]!!.score}")
                    }
                }
                cell(row = 1, colSpan = 2) {
                    if (!botTurn) {
                        text("Your move: ")
                        textLine(inputStr)
                        humanColour {
                            textLine()
                            if (suggestions.isCompleteMove) {
                                textLine("[Enter] to make move")
                            }
                            if (suggestions.letters.isNotEmpty()) {
                                white {
                                    text("Letters:    ")
                                }
                                suggestions.letters.sorted().forEach {
                                    text("$it ")
                                }
                                textLine()
                            }
                            if (suggestions.numbers.isNotEmpty()) {
                                white {
                                    text("Numbers:    ")
                                }
                                suggestions.numbers.sorted().forEach {
                                    text("$it ")
                                }
                                textLine()
                            }
                            if (suggestions.directions.isNotEmpty()) {
                                white {
                                    text("Directions: ")
                                }
                                suggestions.directions.sorted().forEach {
                                    text("${it.toArrow()} ")
                                }
                                textLine()
                            }
                        }
                    } else {
                        botColour {
                            textLine("Bot moving...")
                        }
                    }
                    green {
                        textLine(status)
                    }
                }
            }
        }.runUntilSignal {
            var playerAction: Action? = null

            onKeyPressed {
                when (key) {
                    Keys.Enter -> {
                        val move = getMove(game, inputStr)
                        if (move != null) {
                            inputStr = ""
                            axisChar = null
                            altMode = false
                            selectedLetters = setOf()
                            selectedNumbers = setOf()
                            suggestions = MoveSuggestions()
                            playerAction = move
                        }
                    }

                    Keys.Q -> signal()

                    else -> {
                        val str: String
                        if (key == Keys.Backspace) {
                            inputStr = inputStr.dropLast(1)
                            str = inputStr
                        } else {
                            var k: String = when (key) {
                                Keys.Up -> UP.toString()
                                Keys.Down -> DOWN.toString()
                                Keys.Left -> LEFT.toString()
                                Keys.Right -> RIGHT.toString()
                                else -> key.toString()
                            }

                            fun mergeDirections(c1: Char, c2: Char) {
                                inputStr = inputStr.dropLast(1)
                                if (c1 == UP) {
                                    if (c2 == LEFT) k = UP_LEFT.toString()
                                    if (c2 == RIGHT) k = UP_RIGHT.toString()
                                } else if (c1 == DOWN) {
                                    if (c2 == LEFT) k = DOWN_LEFT.toString()
                                    if (c2 == RIGHT) k = DOWN_RIGHT.toString()
                                }
                            }

                            if (!inputStr.isEmpty()) {
                                when (key) {
                                    Keys.Up -> when (inputStr.last()) {
                                        LEFT -> mergeDirections(UP, LEFT)
                                        RIGHT -> mergeDirections(UP, RIGHT)
                                    }

                                    Keys.Down -> when (inputStr.last()) {
                                        LEFT -> mergeDirections(DOWN, LEFT)
                                        RIGHT -> mergeDirections(DOWN, RIGHT)
                                    }

                                    Keys.Left -> when (inputStr.last()) {
                                        UP -> mergeDirections(UP, LEFT)
                                        DOWN -> mergeDirections(DOWN, LEFT)
                                    }

                                    Keys.Right -> when (inputStr.last()) {
                                        UP -> mergeDirections(UP, RIGHT)
                                        DOWN -> mergeDirections(DOWN, RIGHT)
                                    }
                                }
                            }
                            str = inputStr + k.uppercase()
                        }
                        val s = getNextMoveSuggestions(game, str)
                        if (!s.none()) {
                            inputStr = str
                            suggestions = s
                            selectedNumbers = inputStr.toSet()
                            selectedLetters = inputStr.toSet()
                            if (inputStr.isNotEmpty()) {
                                altMode = inputStr.first() == ALT_MODE_CHAR
                                axisChar = inputStr.first()
                                if (altMode && inputStr.length > 1) {
                                    axisChar = inputStr[1]
                                }
                            } else {
                                altMode = false
                                axisChar = null
                            }
                        }
                    }
                }
                rerender()
            }

            addTimer(Anim.ONE_FRAME_60FPS, repeat = true, key = timerKey) {
                var newGameState: StateRepresentation? = null

                if (botTurn) {
                    rerender()
                    val botAction = bot.search(game, maxDepth, firstMove)
                    newGameState = StateSpaceGenerator.result(game, botAction)
                    game = newGameState
                    if (StateSearcher.terminalTest(game)) {
                        gameOver = true
                    }
                    firstMove = false
                    suggestions = getNextMoveSuggestions(game, "")
                    botTurn = !botTurn
                } else if (playerAction != null) { // the timer waits here in a hot loop until the user enters a move
                    newGameState = StateSpaceGenerator.result(game, playerAction!!)
                    game = newGameState
                    if (StateSearcher.terminalTest(game)) {
                        gameOver = true
                    }
                    playerAction = null
                    firstMove = false
                    botTurn = !botTurn
                }

                if (gameOver) {
                    repeat = false
                    val whiteScore = game.players[Piece.White]!!.score
                    val blackScore = game.players[Piece.Black]!!.score
                    status = if (whiteScore >= 6 || whiteScore > blackScore) "White wins."
                    else if (blackScore >= 6 || blackScore > whiteScore) "Black wins."
                    else "Tie game."
                }
                rerender()
            }
        }
    }
}

