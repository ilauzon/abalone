package tui

import abalone.model.Coordinate

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

