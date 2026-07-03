package ca.isaaclauzon.abalone.tui

import ca.isaaclauzon.abalone.model.LetterCoordinate
import ca.isaaclauzon.abalone.model.NumberCoordinate

/** A coordinate with no restrictions on letter-number combinations. */
data class SimpleCoordinate(val letter: LetterCoordinate, val number: NumberCoordinate)

