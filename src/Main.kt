import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.foundation.anim.*
import com.varabyte.kotter.foundation.input.*
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.foundation.text.ColorLayer.*
import com.varabyte.kotter.foundation.timer.*
import com.varabyte.kotter.runtime.render.*
import com.varabyte.kotter.runtime.terminal.TerminalSize
import com.varabyte.kotter.terminal.virtual.*
import com.varabyte.kotterx.grid.*
import abalone.model.*
import abalone.model.search.*
import kotlin.time.Duration.Companion.milliseconds
import abalone.model.LetterCoordinate as L
import abalone.model.NumberCoordinate as N

private const val WIDTH = 80
private const val HEIGHT = 40

private const val UP = '↑'
private const val DOWN = '↓'
private const val LEFT = '←'
private const val RIGHT = '→'
private const val UP_LEFT = '↖'
private const val UP_RIGHT = '↗'
private const val DOWN_LEFT = '↙'
private const val DOWN_RIGHT = '↘'

private const val BLACK_PIECE = 'O'
private const val WHITE_PIECE = '@'

private const val ALT_MODE_CHAR = '/'

private const val botGoesFirst = true
private val botPiece = if (botGoesFirst) Piece.Black else Piece.White
private val humanPiece = if (botGoesFirst) Piece.White else Piece.Black

fun RenderScope.humanColour(scopedBlock: RenderScope.() -> Unit) = blue { scopedBlock() }
fun RenderScope.botColour(scopedBlock: RenderScope.() -> Unit) = red { scopedBlock() }
fun RenderScope.boardColour(scopedBlock: RenderScope.() -> Unit) = white { scopedBlock() }
val RenderScope.lineHighlight: RenderScope.(Char) -> Unit
    get() = { c -> rgb(0x222222, layer = BG) { text(c) } }
val RenderScope.cellHighlight: RenderScope.(Char) -> Unit
    get() = { c -> white(layer = BG) { black { text(c) } } }

fun isLetter(c: Char): Boolean {
    return L.convertLetter(c.toString()) != L.NULL
}

fun isNumber(c: Char): Boolean {
    return N.convertNumber(c.toString()) != N.NULL
}

fun isDirectionSign(c: Char): Boolean {
    return listOf('-', '+').contains(c)
}

fun isDirectionAxis(c: Char): Boolean {
    return listOf('X', 'Y', 'Z').contains(c)
}

fun toNumber(c: Char): N {
    return N.convertNumber(c.toString())
}

fun toLetter(c: Char): L {
    return L.convertLetter(c.toString())
}

fun toCoordinate(str: String): Coordinate? {
    val letter = toLetter(str[0])
    val number = toNumber(str[1])

    if (letter == L.NULL || number == N.NULL) {
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

enum class BoardSide {
    LEFT,
    MIDDLE,
    RIGHT,
}

data class SimpleCoordinate(val letter: L, val number: N)

/**
 * Transform a coordinate described w.r.t. the Z-axis of the board to a coordinate in regular space.
 * @param side the side of the board that the coordinate is on.
 *             This must be specified since the side that Z-axis coordinates are on can be ambiguous.
 * @return the transformed coordinate.
 */
fun SimpleCoordinate.reg(side: BoardSide): Coordinate {
    if (side == BoardSide.LEFT) {
        val altLetter = letter + number.ordinal
        return Coordinate.get(altLetter, number)
    } else if (side == BoardSide.MIDDLE) {
        return Coordinate.get(letter, number)
    } else {
        val altNumber = number + letter.ordinal
        return Coordinate.get(letter, altNumber)
    }
}

/**
 * Transform a coordinate to a coordinate described w.r.t. the Z-axis of the board.
 *
 * @return the transformed coordinate.
 */
fun SimpleCoordinate.alt(): SimpleCoordinate {
    val side = side()
    if (side == BoardSide.LEFT) {
        val altLetter = letter - number.ordinal
        return SimpleCoordinate(altLetter, number)
    } else if (side == BoardSide.MIDDLE) {
        return SimpleCoordinate(letter, number)
    } else {
        val altNumber = number - letter.ordinal
        return SimpleCoordinate(letter, altNumber)
    }
}

fun SimpleCoordinate.side(): BoardSide {
    val difference = letter.ordinal - number.ordinal
    return when {
        difference > 0 -> BoardSide.LEFT
        difference < 0 -> BoardSide.RIGHT
        else -> BoardSide.MIDDLE
    }
}

fun Coordinate.alt(): SimpleCoordinate {
    return SimpleCoordinate(letter, number).alt()
}

fun Coordinate.side(): BoardSide {
    return SimpleCoordinate(letter, number).side()
}

fun Coordinate.simple(): SimpleCoordinate {
    return SimpleCoordinate(letter, number)
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
fun parseMove(str: String): ParseResult? {
    if (str.isEmpty()) return ParseResult(null, "", null, null, false)
    val altMode = str.isNotEmpty() && str[0] == ALT_MODE_CHAR
    val s = if (altMode) str.drop(1) else str
    if (s.isEmpty()) return ParseResult(null, "", null, null, altMode)

    val first = s[0]
    var rest = ""
    val directionChars = listOf(UP, DOWN, LEFT, RIGHT, UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT)
    val incompleteDirectionChars = listOf(UP, DOWN)

    // parse marble selection
    var counter = 1
    if (altMode && ALT_MODE_CHAR == first) {
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

enum class SelectionAxis {
    X, Y, Z
}

open class ParsedInput(
    var axis: SelectionAxis? = null,
    var axisIndexer: Char? = null,
    val coordinates: MutableSet<Coordinate> = mutableSetOf(),
    var direction: MoveDirection? = null,
)

data class MoveSuggestionSet(
    val letters: MutableSet<L> = mutableSetOf(),
    val numbers: MutableSet<N> = mutableSetOf(),
    val directions: MutableSet<MoveDirection> = mutableSetOf(),
    val parsed: ParsedInput = ParsedInput(),
    var suggestAltMode: Boolean = false,
    var suggestAltModeAxis: Boolean = false,
    var isCompleteMove: Boolean = false,
) {

    fun none(): Boolean {
        return letters.isEmpty() && numbers.isEmpty() && directions.isEmpty() && !isCompleteMove
    }
}

/**
 * Parse the coordinates from a ParseResult object.
 *
 * @return the parsed coordinates in regular space, or null if there was a problem parsing.
 */
fun parseCoordinates(parsed: ParseResult): Set<Coordinate>? {
    if (parsed.first == null) {
        return hashSetOf()
    }

    val parsedCoordinates = hashSetOf<Coordinate>()
    if (parsed.altMode) {
        if (ALT_MODE_CHAR == parsed.first) {
            parsedCoordinates.addAll(
                parsed.rest.map { c ->
                    val ord: Int
                    if (isLetter(c)) {
                        ord = toLetter(c).ordinal
                    } else if (isNumber(c)) {
                        ord = toNumber(c).ordinal
                    } else {
                        return null
                    }

                    Coordinate.get(
                        L.entries[ord],
                        N.entries[ord],
                    )
                }
            )
        } else if (isLetter(parsed.first)) {
            try {
                parsedCoordinates.addAll(
                    parsed.rest.map { c ->
                        SimpleCoordinate(toLetter(parsed.first), toNumber(c)).reg(BoardSide.LEFT)
                    }
                )
            } catch (e: IllegalArgumentException) {
                return null
            }
        } else if (isNumber(parsed.first)) {
            if (!altModeNumbers.contains(toNumber(parsed.first))) return null
            try {
                parsedCoordinates.addAll(
                    parsed.rest.map { c ->
                        SimpleCoordinate(toLetter(c), toNumber(parsed.first)).reg(BoardSide.RIGHT)
                    }
                )
            } catch (e: IllegalArgumentException) {
                return null
            }
        }
    } else {
        if (isLetter(parsed.first)) {
            parsedCoordinates.addAll(parsed.rest.map { c ->
                Coordinate.get(
                    toLetter(parsed.first),
                    toNumber(c)
                )
            })
        } else if (isNumber(parsed.first)) {
            parsedCoordinates.addAll(parsed.rest.map { c ->
                Coordinate.get(
                    toLetter(c),
                    toNumber(parsed.first)
                )
            })
        }
    }
    return parsedCoordinates
}

fun getNextMoveSuggestions(state: StateRepresentation, str: String): MoveSuggestionSet {
    val suggestions = MoveSuggestionSet()
    val parsed = parseMove(str) ?: return suggestions
    val actions = StateSpaceGenerator.actions(state)
    val board = state.board.cells
    val parsedCoordinates = parseCoordinates(parsed) ?: return MoveSuggestionSet()

    suggestions.parsed.coordinates.addAll(parsedCoordinates)

    if (parsed.first != null) {
        suggestions.parsed.axisIndexer = parsed.first
        if (parsed.altMode) {
            suggestions.parsed.axis = SelectionAxis.Z
        } else if (isLetter((parsed.first))) {
            suggestions.parsed.axis = SelectionAxis.X
        } else if (isNumber(parsed.first)) {
            suggestions.parsed.axis = SelectionAxis.Y
            suggestions.parsed.axisIndexer = parsed.first
        }
    }

    outer@ for (action in actions) {
        val actionCoords = action.coordinates
            .filter { board[it] == state.currentPlayer }
        if (!actionCoords.containsAll(parsedCoordinates)) continue

        if (parsed.first == null) {
            suggestions.suggestAltMode = true

            if (parsed.altMode) {
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

            suggestions.letters.addAll(actionCoords.map { it.letter })
            suggestions.numbers.addAll(actionCoords.map { it.number })

            continue
        }

        if (parsed.direction != null) {
            if (!parsedCoordinates.containsAll(actionCoords)) continue

            // if the user puts in left or right, suggest actions with the corresponding diagonal directions
            if (parsed.direction == MoveDirection.NegX && action.direction == MoveDirection.PosY || action.direction == MoveDirection.NegZ) {
                suggestions.directions.add(action.direction)
            }
            if (parsed.direction == MoveDirection.PosX && action.direction == MoveDirection.NegY || action.direction == MoveDirection.PosZ) {
                suggestions.directions.add(action.direction)
            }

            if (parsed.direction == action.direction) {
                suggestions.isCompleteMove = true
                suggestions.parsed.direction = action.direction
                return suggestions
            }
            continue
        }

        // i.e. the user entered up or down
        if (parsed.incompleteDirection != null) {
            if (!parsedCoordinates.containsAll(actionCoords)) continue
            if (parsed.incompleteDirection == IncompleteDirection.Up) {
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

        if (parsed.altMode) {
            if (parsed.first == ALT_MODE_CHAR) {
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

            if (isLetter(parsed.first)) {
                for (coord in actionCoords) {
                    if (coord.alt().letter != toLetter(parsed.first)) {
                        continue@outer
                    }
                }
                suggestions.numbers.addAll(
                    actionCoords
                        .filter { !parsedCoordinates.contains(it) }
                        .map { it.alt().number })
                continue
            }

            if (isNumber(parsed.first)) {
                for (coord in actionCoords) {
                    if (coord.alt().number != toNumber(parsed.first)) {
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

        if (isLetter(parsed.first) && actionCoords.all { it.letter == toLetter(parsed.first) }) {
            suggestions.numbers.addAll(
                actionCoords
                    .filter { !parsedCoordinates.contains(it) }
                    .map { it.number })
            continue
        }

        if (isNumber(parsed.first) && actionCoords.all { it.number == toNumber(parsed.first) }) {
            suggestions.letters.addAll(
                actionCoords
                    .filter { !parsedCoordinates.contains(it) }
                    .map { it.letter })
            continue
        }
    }

    return suggestions
}

fun getMove(state: StateRepresentation, str: String): Action? {
    val parsed = parseMove(str) ?: return null
    val board = state.board.cells

    val parsedCoordinates = parseCoordinates(parsed) ?: return null

    for (action in StateSpaceGenerator.actions(state)) {
        if (action.coordinates
                .filter { board[it] == state.currentPlayer }
                .toSet() == parsedCoordinates.map { c -> Coordinate.get(c.letter, c.number) }.toSet()
            && action.direction == parsed.direction
        ) {
            return action
        }
    }
    return null
}

fun Coordinate.lineIsHighlighted(
    suggestions: MoveSuggestionSet
): Boolean {
    if (suggestions.parsed.axis == SelectionAxis.X && letter.toString()[0] == suggestions.parsed.axisIndexer) {
        return true
    }

    if (suggestions.parsed.axis == SelectionAxis.Y && number.toString()[0] == suggestions.parsed.axisIndexer) {
        return true
    }

    if (suggestions.parsed.axis == SelectionAxis.Z) {
        val simple = SimpleCoordinate(letter, number)
        val alt = simple.alt()
        val side = simple.side()
        if (side == BoardSide.MIDDLE && suggestions.parsed.axisIndexer == ALT_MODE_CHAR) {
            return true
        }
        if (side == BoardSide.LEFT && suggestions.parsed.axisIndexer == alt.letter.toString()[0]) {
            return true
        }

        if (side == BoardSide.RIGHT && suggestions.parsed.axisIndexer == alt.number.toString()[0]) {
            return true
        }
        return false
    }

    return false
}

fun Coordinate.display(c: Char, suggestions: MoveSuggestionSet): Char {
    val letterChar = letter.toString()[0]
    val numberChar = number.toString()[0]
    val simple = SimpleCoordinate(letter, number)
    if (suggestions.parsed.axis == SelectionAxis.X && suggestions.parsed.axisIndexer == letterChar) {
        return numberChar
    }

    if (suggestions.parsed.axis == SelectionAxis.Y && suggestions.parsed.axisIndexer == numberChar) {
        return letterChar
    }

    if (suggestions.parsed.axis == SelectionAxis.Z) {
        val side = simple.side()
        val alt = simple.alt()
        val altLetterChar = alt.letter.toString()[0]
        val altNumberChar = alt.number.toString()[0]
        if (side == BoardSide.MIDDLE && suggestions.parsed.axisIndexer == ALT_MODE_CHAR) {
            return numberChar
        }
        if (suggestions.parsed.axisIndexer == altLetterChar) {
            return altNumberChar
        }
        if (suggestions.parsed.axisIndexer == altNumberChar) {
            return altLetterChar
        }
    }

    return c
}

fun RenderScope.drawBoard(
    game: StateRepresentation,
    suggestions: MoveSuggestionSet,
) {
    fun Coordinate.render() {
        val piece = game.board.cells[this]
        var pixel: RenderScope.(c: Char) -> Unit = { c -> text(c) }

        if (lineIsHighlighted(suggestions)) {
            pixel = lineHighlight
        }

        if (suggestions.parsed.coordinates.contains(this)) {
            pixel = cellHighlight
        }

        when (piece) {
            Piece.Empty -> {
                pixel('.')
            }

            Piece.Black -> {
                if (botPiece == Piece.Black) {
                    botColour {
                        pixel(BLACK_PIECE)
                    }
                } else {
                    humanColour {
                        pixel(display(BLACK_PIECE, suggestions))
                    }
                }
            }

            Piece.White -> {
                if (botPiece == Piece.White) {
                    botColour {
                        pixel(WHITE_PIECE)
                    }
                } else {
                    humanColour {
                        pixel(display(WHITE_PIECE, suggestions))
                    }
                }
            }

            Piece.OffBoard -> {
                text(' ')
            }
        }
        text(' ')
    }

    fun letterRowToString(letter: L) {
        for (l in letter.min..letter.max) {
            val coord = Coordinate.get(letter, l)
            coord.render()
        }
    }

    fun RenderScope.number(n: N) {
        if (suggestions.numbers.contains(n)) {
            boardColour { text(n.toString()) }
        } else {
            text(' ')
        }
    }

    fun RenderScope.letter(l: L) {
        if (suggestions.letters.contains(l)) {
            boardColour { text(l.toString()) }
        } else {
            text(' ')
        }
    }

    fun RenderScope.altChar() {
        if (suggestions.suggestAltMode || suggestions.suggestAltModeAxis) {
            boardColour { text(ALT_MODE_CHAR) }
        } else {
            text(' ')
        }
    }

    text("    "); letter(L.I); text(' '); letterRowToString(L.I); textLine()
    text("   "); letter(L.H); text(' '); letterRowToString(L.H); textLine()
    text("  "); letter(L.G); text(' '); letterRowToString(L.G); textLine()
    text(" "); letter(L.F); text(' '); letterRowToString(L.F); textLine()
    text(""); letter(L.E); text(' '); letterRowToString(L.E); textLine()
    text(" "); letter(L.D); text(' '); letterRowToString(L.D); number(N.NINE); textLine()
    text("  "); letter(L.C); text(' '); letterRowToString(L.C); number(N.EIGHT); textLine()
    text("   "); letter(L.B); text(' '); letterRowToString(L.B); number(N.SEVEN); textLine()
    text("    "); letter(L.A); text(' '); letterRowToString(L.A); number(N.SIX); textLine()
    text("     "); altChar()
    text(' '); number(N.ONE)
    text(' '); number(N.TWO)
    text(' '); number(N.THREE)
    text(' '); number(N.FOUR)
    text(' '); number(N.FIVE)
    textLine()
}

fun main() {
    session(
        terminal = listOf(
//            { SystemTerminal() },
            { VirtualTerminal.create(terminalSize = TerminalSize(WIDTH, HEIGHT + 15)) }
        ).firstSuccess(),
        clearTerminal = true,
    ) {

        var game = StateRepresentation(
            BoardState(BoardState.Layout.STANDARD),
            movesRemaining = 300,
            currentPlayer = Piece.Black
        )
        val maxDepth = 5
        val bot = StateSearcher(IsaacHeuristic())
        var status = ""
        var botTurn = botGoesFirst
        var firstMove = true
        var gameOver = false
        var suggestions = MoveSuggestionSet()
        var timerKey = Any()
        var inputStr = ""

        // caret blinking
        val BLINK_LEN = 500
        var lastBlink = System.currentTimeMillis()
        var blinkOn by liveVarOf(false)

        section {
            val gridWidth = WIDTH - 2
            underline { textLine("ABALONE v1.0.0") }
            textLine()
            drawBoard(game, suggestions)
            textLine()
            humanColour {
                textLine("Your score: ${game.players[humanPiece]!!.score}")
            }
            botColour {
                textLine("Bot score:  ${game.players[botPiece]!!.score}")
            }
            textLine()
            if (!botTurn) {
                grid(cols = Cols(gridWidth)) {
                    cell {
                        text(" Your move: ")
                        text(inputStr)
                        if (blinkOn) {
                            invert { text(' ') }
                        }
                        textLine()
                    }
                }
                green {
                    if (suggestions.isCompleteMove) {
                        textLine("[Enter] to make move")
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
            grid(Cols(gridWidth - 10 - 1, 10), characters = GridCharacters.Invisible) {
                cell(row = 0, col = 0, colSpan = 2) {
                    underline { textLine("CONTROLS") }
                    textLine()
                }
                cell(row = 1, col = 1) {
                    textLine(
                        """
                                +Y    +Z
                                  \   /
                                   \ /
                                -X--.--+X
                                   / \    
                                  /   \
                                -Z    -Y
                            """.trimIndent()
                    )
                }
                cell(row = 1, col = 0) {
                    green { textLine("[A-I]") }; textLine("  Select line of marbles along the X-axis")
                    green { textLine("[1-9]") }; textLine("  Select line of marbles along the Y-axis")
                    green { textLine("[/]") }; textLine("  Select line of marbles along the Z-axis");
                    green { textLine("[Arrow Keys]") }; textLine(
                    "  Select direction to move the line of marbles. Combine them to\n" +
                            "  move along Y and Z axes ([↓] plus [←] equals [↙])"
                );
                    green { textLine("[Enter]") }; textLine("  Move");
                    green { textLine("[Q]") }; textLine("  Quit");
                    textLine()
                    textLine("HINT: The labels on the edge of the board will tell you what \n      inputs are possible.")
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
                            suggestions = MoveSuggestionSet()
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
                        }
                    }
                }
                rerender()
            }

            addTimer(Anim.ONE_FRAME_60FPS, repeat = true, key = timerKey) {
                if (System.currentTimeMillis() - lastBlink > BLINK_LEN) {
                    blinkOn = !blinkOn
                    lastBlink = System.currentTimeMillis()
                    rerender()
                }
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
                } else if (playerAction != null) { // the timer tests this 60 times a second until the player has moved
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

