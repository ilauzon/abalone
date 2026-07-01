package tui

import abalone.model.*
import abalone.model.search.StateSearcher
import abalone.model.LetterCoordinate as L
import abalone.model.NumberCoordinate as N
import com.varabyte.kotter.foundation.text.ColorLayer.BG
import com.varabyte.kotter.foundation.text.black
import com.varabyte.kotter.foundation.text.blue
import com.varabyte.kotter.foundation.text.green
import com.varabyte.kotter.foundation.text.invert
import com.varabyte.kotter.foundation.text.red
import com.varabyte.kotter.foundation.text.rgb
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.text.underline
import com.varabyte.kotter.foundation.text.white
import com.varabyte.kotter.runtime.render.RenderScope
import com.varabyte.kotterx.grid.Cols
import com.varabyte.kotterx.grid.GridCharacters
import com.varabyte.kotterx.grid.grid

class Renderer {
    class Screens {
        companion object {
            fun RenderScope.game(
                game: StateRepresentation,
                suggestions: MoveSuggestionSet,
                botTurn: Boolean,
                inputStr: String,
                blinkOn: Boolean,
            ) {
                val humanScore = game.players[Settings.humanPiece]!!.score
                val botScore = game.players[Settings.botPiece]!!.score
                val status = if (humanScore >= 6 || humanScore > botScore) "You win!"
                else if (botScore >= 6 || botScore > humanScore) "Bot wins."
                else if (StateSearcher.terminalTest(game)) "Tie game."
                else ""
                grid(cols = Cols { fit(); fit() }, characters = GridCharacters.Invisible) {
                    cell(col = 0) {
                        board(game, suggestions)
                        // give the board some breathing room on the right
                        text("              ")
                    }
                    cell(col = 1) {
                        grid(cols = Cols { fit() }) {
                            cell {
                                text(" ")
                                underline { text("SCORE") }; textLine(" ")
                                humanColour {
                                    textLine(" You: ${game.players[Settings.humanPiece]!!.score} ")
                                }
                                botColour {
                                    textLine(" Bot: ${game.players[Settings.botPiece]!!.score} ")
                                }
                            }
                        }
                    }
                }
                if (!botTurn) {
                    grid(cols = Cols(40)) {
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
                            text("[Enter] to make move")
                        } else if (suggestions.directions.isNotEmpty()) {
                            white {
                                text("Directions: ")
                            }
                            suggestions.directions.sorted().forEach {
                                text("${arrow(it)} ")
                            }
                        }
                    }
                } else {
                    grid(cols = Cols(Settings.UI_WIDTH), characters = GridCharacters.Invisible) {
                        cell {
                            botColour {
                                textLine("Bot moving...")
                            }
                        }
                    }
                }
                textLine(status)
                green { text("[Esc]") }; textLine(" to see help menu")
                green { text("[Q]") }; textLine(" to quit")
            }

            fun RenderScope.help() {
                grid(Cols(Settings.UI_WIDTH - 10 - 1, 10), characters = GridCharacters.Invisible) {
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
                        green { textLine("[A,B,C,D,E,F,G,H,I]") }; textLine("  Select line of marbles along the X-axis")
                        green { textLine("[1,2,3,4,5,6,7,8,9]") }; textLine("  Select line of marbles along the Y-axis")
                        green { textLine("[${Characters.ALT_MODE_CHAR}]") }; textLine("  Toggle selecting along the Z-axis");
                        green { textLine("[D,C,B,A,${Characters.ALT_MODE_CHAR},1,2,3,4]") }; textLine("  Select line of marbles along the Z-axis")
                        green { textLine("[Arrow keys]") }; textLine(
                        "  Select direction to move the line of marbles. Combine them to\n" +
                                "  move along Y and Z axes ([${Characters.DOWN}] plus [${Characters.LEFT}] equals [${Characters.DOWN_LEFT}])\n])"
                    );
                        green { textLine("[Enter]") }; textLine("  Move");
                        green { textLine("[Q]") }; textLine("  Quit");
                        green { textLine("[Esc]") }; textLine("  Toggle this menu");
                        textLine()
                        textLine("HINT: The labels on the edge of the board will tell you what \n      moves are possible.")
                    }
                }
            }
        }
    }

    companion object {
        private val RenderScope.humanColour: RenderScope.(RenderScope.() -> Unit) -> Unit
            get() = { scopedBlock -> blue { scopedBlock() } }
        private val RenderScope.botColour: RenderScope.(RenderScope.() -> Unit) -> Unit
            get() = { scopedBlock -> red { scopedBlock() } }
        private val RenderScope.disabledLabel: RenderScope.(RenderScope.() -> Unit) -> Unit
            get() = { scopedBlock -> rgb(0x333333) { scopedBlock() } }
        private val RenderScope.label: RenderScope.(RenderScope.() -> Unit) -> Unit
            get() = { scopedBlock -> green { scopedBlock() } }
        private val RenderScope.lineHighlight: RenderScope.(Char) -> Unit
            get() = { c -> rgb(0x333333, layer = BG) { text(c) } }
        private val RenderScope.cellHighlight: RenderScope.(Char) -> Unit
            get() = { c -> white(layer = BG) { black { text(c) } } }

        fun arrow(direction: MoveDirection): Char {
            return when (direction) {
                MoveDirection.NegX -> Characters.LEFT
                MoveDirection.PosX -> Characters.RIGHT
                MoveDirection.NegY -> Characters.DOWN_RIGHT
                MoveDirection.PosY -> Characters.UP_LEFT
                MoveDirection.NegZ -> Characters.DOWN_LEFT
                MoveDirection.PosZ -> Characters.UP_RIGHT
            }
        }

        private fun RenderScope.board(game: StateRepresentation, suggestions: MoveSuggestionSet) {
            fun Coordinate.render() {
                val piece = game.board.cells[this]
                var pixel: RenderScope.(c: Char) -> Unit = { c -> text(c) }

                if (lineIsHighlighted(suggestions)) {
                    pixel = lineHighlight
                }

                if (suggestions.userInput.coordinates.contains(this)) {
                    pixel = cellHighlight
                }

                when (piece) {
                    Piece.Empty -> {
                        pixel('.')
                    }

                    Piece.Black -> {
                        if (Settings.botPiece == Piece.Black) {
                            botColour {
                                pixel(Characters.BLACK_PIECE)
                            }
                        } else {
                            humanColour {
                                pixel(display(Characters.BLACK_PIECE, suggestions))
                            }
                        }
                    }

                    Piece.White -> {
                        if (Settings.botPiece == Piece.White) {
                            botColour {
                                pixel(Characters.WHITE_PIECE)
                            }
                        } else {
                            humanColour {
                                pixel(display(Characters.WHITE_PIECE, suggestions))
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
                    label { text(n.toString()) }
                } else {
                    disabledLabel { text(n.toString()) }
                }
            }

            fun RenderScope.letter(l: L) {
                if (suggestions.letters.contains(l)) {
                    label { text(l.toString()) }
                } else {
                    disabledLabel { text(l.toString()) }
                }
            }

            fun RenderScope.altChar() {
                if (suggestions.suggestAltMode || suggestions.suggestAltModeAxis) {
                    label { text(Characters.ALT_MODE_CHAR) }
                } else {
                    disabledLabel { text(Characters.ALT_MODE_CHAR) }
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
        }


        private fun Coordinate.lineIsHighlighted(
            suggestions: MoveSuggestionSet
        ): Boolean {
            if (suggestions.userInput.axis == SelectionAxis.X && letter.toString()[0] == suggestions.userInput.axisIndexer) {
                return true
            }

            if (suggestions.userInput.axis == SelectionAxis.Y && number.toString()[0] == suggestions.userInput.axisIndexer) {
                return true
            }

            if (suggestions.userInput.axis == SelectionAxis.Z) {
                val simple = SimpleCoordinate(letter, number)
                val alt = simple.alt()
                val side = simple.side()
                if (side == BoardSide.MIDDLE && suggestions.userInput.axisIndexer == Characters.ALT_MODE_CHAR) {
                    return true
                }
                if (side == BoardSide.LEFT && suggestions.userInput.axisIndexer == alt.letter.toString()[0]) {
                    return true
                }

                if (side == BoardSide.RIGHT && suggestions.userInput.axisIndexer == alt.number.toString()[0]) {
                    return true
                }
                return false
            }

            return false
        }

        private fun Coordinate.display(c: Char, suggestions: MoveSuggestionSet): Char {
            val letterChar = letter.toString()[0]
            val numberChar = number.toString()[0]
            val simple = SimpleCoordinate(letter, number)
            if (suggestions.userInput.axis == SelectionAxis.X &&
                suggestions.numbers.contains(number) &&
                suggestions.userInput.axisIndexer == letterChar
            ) {
                return numberChar
            }

            if (suggestions.userInput.axis == SelectionAxis.Y &&
                suggestions.letters.contains(letter) &&
                suggestions.userInput.axisIndexer == numberChar
            ) {
                return letterChar
            }

            if (suggestions.userInput.axis == SelectionAxis.Z) {
                val side = simple.side()
                val alt = simple.alt()
                val altLetterChar = alt.letter.toString()[0]
                val altNumberChar = alt.number.toString()[0]
                if (side == BoardSide.MIDDLE &&
                    suggestions.userInput.axisIndexer == Characters.ALT_MODE_CHAR &&
                    suggestions.numbers.contains(number)
                ) {
                    return numberChar
                }
                if (suggestions.userInput.axisIndexer == altLetterChar &&
                    suggestions.numbers.contains(number)
                ) {
                    return altNumberChar
                }
                if (suggestions.userInput.axisIndexer == altNumberChar &&
                    suggestions.letters.contains(letter)
                ) {
                    return altLetterChar
                }
            }

            return c
        }

    }
}