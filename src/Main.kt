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
import kotlin.random.Random
import abalone.model.*

private const val WIDTH = 60
private const val HEIGHT = 20
fun RenderScope.blackMarble() = green { text('◯') }
fun RenderScope.whiteMarble() = green { text('◉') }
fun RenderScope.emptySpot() = green { text('∙') }


/**
 * Renders the row of the given letter as a string.
 *
 * @param letter the row designator.
 * @param boardMap the current state of cells on the board to render..
 */
fun letterRowToString(letter: LetterCoordinate, boardMap: BoardMap): String {
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
    return (letter.min..letter.max).joinToString(
        separator = " ",
        prefix = prefix,
        postfix = postfix,
        transform = {
            val piece = boardMap[Coordinate.get(letter, it)]
            when (piece) {
                Piece.Empty -> "∙"
                Piece.Black -> "@"
                Piece.White -> "O"
                Piece.OffBoard -> " "
            }
        }
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
        val board = BoardState(BoardState.Layout.STANDARD)

        section {
            textLine("        I ${letterRowToString(LetterCoordinate.I, board.cells)} ")
            textLine("       H ${letterRowToString(LetterCoordinate.H, board.cells)} ")
            textLine("      G ${letterRowToString(LetterCoordinate.G, board.cells)} ")
            textLine("     F ${letterRowToString(LetterCoordinate.F, board.cells)} ")
            textLine("    E ${letterRowToString(LetterCoordinate.E, board.cells)} ")
            textLine("     D ${letterRowToString(LetterCoordinate.D, board.cells)}9")
            textLine("      C ${letterRowToString(LetterCoordinate.C, board.cells)}8")
            textLine("       B ${letterRowToString(LetterCoordinate.B, board.cells)}7")
            textLine("        A ${letterRowToString(LetterCoordinate.A, board.cells)}6")
            textLine("            1 2 3 4 5")
        }.run()
    }
}
