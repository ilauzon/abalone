package tui

import abalone.model.LetterCoordinate
import abalone.model.NumberCoordinate

/** A coordinate with no restrictions on letter-number combinations. */
data class SimpleCoordinate(val letter: LetterCoordinate, val number: NumberCoordinate)

