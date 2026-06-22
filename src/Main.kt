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

private const val botGoesFirst = true
private val botPiece = if (botGoesFirst) Piece.Black else Piece.White
private val humanPiece = if (botGoesFirst) Piece.White else Piece.Black

fun RenderScope.blackMarble() = green { text('◯') }
fun RenderScope.whiteMarble() = green { text('◉') }
fun RenderScope.emptySpot() = green { text('∙') }

fun RenderScope.humanColour(scopedBlock: RenderScope.() -> Unit) = blue { scopedBlock() }
fun RenderScope.botColour(scopedBlock: RenderScope.() -> Unit) = green { scopedBlock() }
fun RenderScope.boardColour(scopedBlock: RenderScope.() -> Unit) = white { scopedBlock() }
fun RenderScope.highlightedColour(scopedBlock: RenderScope.() -> Unit) = blue(layer = BG) { scopedBlock() }

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

data class ParseResult(val coordinates: List<Coordinate>, val moveDirection: MoveDirection?, val fragment: Char?)

fun parseMove(str: String): ParseResult? {
    val coordinates = mutableListOf<Coordinate>()
    val charPairs = str.chunked(2)
    var fragment: Char? = null
    var moveDirection: MoveDirection? = null
    for ((counter, pair) in charPairs.withIndex()) {
        if (pair.length == 1) {
            fragment = pair[0]
            break
        }

        if (counter < 3) {
            val coord = toCoordinate(pair)
            if (coord == null) {
                moveDirection = toMoveDirection(pair) ?: return null
            } else {
                coordinates.add(coord)
            }
        } else if (counter == 3) {
            moveDirection = toMoveDirection(pair) ?: return null
        } else {
            return null
        }

    }
    return ParseResult(coordinates, moveDirection, fragment)
}

fun getMatchingSuggestions(
    state: StateRepresentation,
    parsed: ParseResult
): List<String> {
    val suggestions = mutableSetOf<String>()
    val actions = StateSpaceGenerator.actions(state)
    val board = state.board.cells
    for (action in actions) {
        val playerCoords = action.coordinates.filter { board[it] == state.currentPlayer }
        if (parsed.fragment == null) {
            if (playerCoords.containsAll(parsed.coordinates)) {
                if (parsed.coordinates.size < 3) {
                    suggestions.addAll(
                        playerCoords
                            .filter { !parsed.coordinates.contains(it) }
                            .map { it.toString() }
                    )
                }
                if (parsed.coordinates.containsAll(playerCoords)) {
                    if (parsed.moveDirection == action.direction) {
                        suggestions.add("[Enter] to make move")
                        break
                    } else {
                        suggestions.add(action.direction.toString())
                    }
                }
            }
        } else if (isLetter(parsed.fragment)) {
            val letter = toLetter(parsed.fragment)
            if (playerCoords.containsAll(parsed.coordinates) && parsed.coordinates.size < 3) {
                suggestions.addAll(
                    playerCoords
                        .filter { !parsed.coordinates.contains(it) && it.letter == letter }
                        .map { it.toString() }
                )
            }
        } else if (isDirectionSign(parsed.fragment) && parsed.coordinates.isNotEmpty()) {
            if (playerCoords.toSet() == parsed.coordinates.toSet()) {
                if (action.direction.toString()[0] == parsed.fragment) {
                    suggestions.add(action.direction.toString())
                }
            }
        }
    }
    return suggestions.toList()
}

fun getSuggestions(state: StateRepresentation, str: String): List<String> {
    val parsed = parseMove(str) ?: return listOf()
    return getMatchingSuggestions(state, parsed)
}

fun getMove(state: StateRepresentation, str: String): Action? {
    val parsed = parseMove(str) ?: return null
    val board = state.board.cells
    for (action in StateSpaceGenerator.actions(state)) {
        if (action.coordinates
                .filter { board[it] == state.currentPlayer }
                .toSet() == parsed.coordinates.toSet()
            && action.direction == parsed.moveDirection
            && parsed.fragment == null
        ) {
            return action
        }
    }
    return null
}

fun RenderScope.drawBoard(
    game: StateRepresentation,
    selectedLetters: Set<Char> = setOf(),
    selectedNumbers: Set<Char> = setOf()
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
        for (l in letter.min..letter.max) {
            val piece = game.board.cells[Coordinate.get(letter, l)]
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
                            text('O')
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
                            text('@')
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
            highlightedColour { text(c) }
        } else {
            boardColour { text(c) }
        }
    }

    fun RenderScope.letter(c: Char) {
        if (selectedLetters.contains(c)) {
            highlightedColour { text(c) }
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
    text("     "); text("  "); number('1'); text(' '); number('2'); text(' '); number('3'); text(' '); number('4'); text(
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
        var suggestions = listOf<String>()
        var timerKey = Any()
        var inputStr = ""

        section {
            grid(Cols { fit(); fit() }) {
                cell {
                    drawBoard(game)
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
                            suggestions.forEach {
                                text("    $it")
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
                            var k = key
                            if (key == Keys.Equals) k = Keys.Plus
                            str = inputStr + k.toString().uppercase()
                        }
                        val retrievedSuggestions = getSuggestions(game, str)
                        if (!retrievedSuggestions.isEmpty()) {
                            suggestions = retrievedSuggestions
                            inputStr = str
                        }
                    }
                }
                rerender()
            }

            addTimer(Anim.ONE_FRAME_60FPS, repeat = true, key = timerKey) {
                var newGameState: StateRepresentation? = null

                if (botTurn) {
                    rerender()
                    try {
                        val botAction = bot.search(game, maxDepth, firstMove)
                        newGameState = StateSpaceGenerator.result(game, botAction)
                        game = newGameState
                    } catch (e: IllegalArgumentException) {
                        gameOver = true
                    }
                    firstMove = false
                    botTurn = !botTurn
                    inputStr = ""
                    suggestions = getSuggestions(game, inputStr)
                } else if (playerAction != null) { // the timer waits here in a hot loop until the user enters a move
                    try {
                        newGameState = StateSpaceGenerator.result(game, playerAction!!)
                        game = newGameState
                    } catch (e: IllegalArgumentException) {
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

