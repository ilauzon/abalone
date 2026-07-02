import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.foundation.anim.*
import com.varabyte.kotter.foundation.input.*
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.foundation.timer.*
import com.varabyte.kotter.runtime.terminal.TerminalSize
import com.varabyte.kotter.terminal.virtual.*
import com.varabyte.kotter.terminal.system.SystemTerminal

import abalone.model.*
import abalone.model.search.*
import tui.*
import tui.Renderer.Screens.Companion.game as gameScreen
import tui.Renderer.Screens.Companion.help as helpScreen
import tui.Renderer.Screens.Companion.settings as settingsScreen

fun main() {
    session(
        terminal = listOf(
            { SystemTerminal() },
            { VirtualTerminal.create(terminalSize = TerminalSize(Settings.WIDTH, Settings.HEIGHT + 15)) }
        ).firstSuccess(),
        clearTerminal = true,
    ) {
        val settings = settingsScreen()

        var game = StateRepresentation(
            BoardState(settings.layout),
            movesRemaining = settings.maxMoves,
            currentPlayer = Piece.Black
        )
        val bot = StateSearcher(IsaacHeuristic())
        var botTurn = settings.botGoesFirst
        var firstMove = true
        var gameOver = false
        var suggestions = MoveSuggestionSet()
        var timerKey = Any()
        var inputStr = ""
        var lastAction: Action? = null

        // caret blinking
        val BLINK_LEN = 500
        var lastBlink = System.currentTimeMillis()
        var blinkOn by liveVarOf(false)

        var helpMenuShowing by liveVarOf(false)

        section {
            if (helpMenuShowing) {
                helpScreen()
            } else {
                gameScreen(game, suggestions, settings, botTurn, inputStr, blinkOn, lastAction)
            }
        }.runUntilSignal {

            var playerAction: Action? = null

            onKeyPressed {
                when (key) {
                    Keys.Enter -> {
                        if (helpMenuShowing) return@onKeyPressed
                        val lexed = Lexer.move(inputStr)
                        if (lexed != null) {
                            val action = Parser.action(game, lexed)
                            if (action != null) {
                                inputStr = ""
                                suggestions = MoveSuggestionSet()
                                playerAction = action
                            }
                        }
                    }

                    Keys.Escape -> helpMenuShowing = !helpMenuShowing

                    Keys.Q -> signal()

                    else -> {
                        if (helpMenuShowing) return@onKeyPressed
                        val str: String
                        if (key == Keys.Backspace) {
                            inputStr = inputStr.dropLast(1)
                            str = inputStr
                        } else {
                            var k: String = when (key) {
                                Keys.Up -> Characters.UP.toString()
                                Keys.Down -> Characters.DOWN.toString()
                                Keys.Left -> Characters.LEFT.toString()
                                Keys.Right -> Characters.RIGHT.toString()
                                else -> key.toString()
                            }

                            fun mergeDirections(c1: Char, c2: Char) {
                                inputStr = inputStr.dropLast(1)
                                if (c1 == Characters.UP) {
                                    if (c2 == Characters.LEFT) k = Characters.UP_LEFT.toString()
                                    if (c2 == Characters.RIGHT) k = Characters.UP_RIGHT.toString()
                                } else if (c1 == Characters.DOWN) {
                                    if (c2 == Characters.LEFT) k = Characters.DOWN_LEFT.toString()
                                    if (c2 == Characters.RIGHT) k = Characters.DOWN_RIGHT.toString()
                                }
                            }

                            if (!inputStr.isEmpty()) {
                                when (key) {
                                    Keys.Up -> when (inputStr.last()) {
                                        Characters.LEFT -> mergeDirections(Characters.UP, Characters.LEFT)
                                        Characters.RIGHT -> mergeDirections(Characters.UP, Characters.RIGHT)
                                    }

                                    Keys.Down -> when (inputStr.last()) {
                                        Characters.LEFT -> mergeDirections(Characters.DOWN, Characters.LEFT)
                                        Characters.RIGHT -> mergeDirections(Characters.DOWN, Characters.RIGHT)
                                    }

                                    Keys.Left -> when (inputStr.last()) {
                                        Characters.UP -> mergeDirections(Characters.UP, Characters.LEFT)
                                        Characters.DOWN -> mergeDirections(Characters.DOWN, Characters.LEFT)
                                    }

                                    Keys.Right -> when (inputStr.last()) {
                                        Characters.UP -> mergeDirections(Characters.UP, Characters.RIGHT)
                                        Characters.DOWN -> mergeDirections(Characters.DOWN, Characters.RIGHT)
                                    }
                                }
                            }
                            str = inputStr + k.uppercase()
                        }
                        val s = suggestions(game, str)
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
                }
                var newGameState: StateRepresentation? = null

                if (botTurn) {
                    rerender()
                    val botAction = bot.search(game, settings.MaxSearchDepth, firstMove)
                    lastAction = botAction
                    newGameState = StateSpaceGenerator.result(game, botAction)
                    game = newGameState
                    if (StateSearcher.terminalTest(game)) {
                        gameOver = true
                        rerender()
                        return@addTimer
                    }
                    firstMove = false
                    suggestions = suggestions(game, "")
                    botTurn = !botTurn
                    rerender()
                } else if (playerAction != null) { // the control flow bumps into this 60 times a second until the player has moved
                    lastAction = playerAction
                    newGameState = StateSpaceGenerator.result(game, playerAction!!)
                    game = newGameState
                    if (StateSearcher.terminalTest(game)) {
                        gameOver = true
                        rerender()
                        return@addTimer
                    }
                    playerAction = null
                    firstMove = false
                    botTurn = !botTurn
                    rerender()
                }

                if (gameOver) {
                    repeat = false
                }
            }
        }
    }
}

