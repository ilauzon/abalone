/**
 * Houses the Coordinate, VerticalCoordinate, and HorizontalCoordinate classes, which all
 * manage the coordinate system of marbles.
 */
package abalone.model

import java.lang.IllegalArgumentException
import kotlin.math.*

/**
 * Represents an immutable letter-number pair, signifying a position on the Abalone game board.
 *
 * Implemented using a private constructor. Instances of this class are accessed via the .get
 * method, which retrieves an instance from a map generated in a static constructor. This is to
 * reduce unnecessary instantiation, given that there is a very small and defined number of possible
 * instances of this class.
 *
 * @property letter the letter coordinate.
 * @property number the number coordinate.
 */
class Coordinate private constructor(val letter: LetterCoordinate, val number: NumberCoordinate) {

    companion object {
        /**
         * The coordinate representing a location not on the board. Has a letter and number of NULL
         * (the enum value, not the null type).
         */
        val offBoard = Coordinate(LetterCoordinate.NULL, NumberCoordinate.NULL)

        /**
         * The map of coordinates that is looked up whenever an instance is retrieved.
         */
        private val coordinates: Array<Coordinate> = Array(
            LetterCoordinate.I.ordinal * 16 + NumberCoordinate.NINE.ordinal + 1,
        ) { offBoard }

        /**
         * A list of all adjacent coordinates to each coordinate, to be generated at the start of the program.
         */
//        private val adjacentMap: HashMap<Coordinate, Map<MoveDirection, Coordinate>> = hashMapOf()
        private val adjacentArray: Array<Coordinate> = Array(coordinates.size * 6) { offBoard }

        /**
         * Initializes the coordinates map and adjacency map.
         */
        init {
            for (l: LetterCoordinate in LetterCoordinate.entries) {
                for (n: NumberCoordinate in l.min .. l.max) {
                    val coord = Coordinate(l, n)
                    val pos = hash(l, n)
                    coordinates[pos] = coord
                    for (direction in MoveDirection.entries) {
                        adjacentArray[pos * 6 + direction.ordinal] = coord.initMove(direction)
                    }
//                    val adjacent = hashMapOf(
//                        MoveDirection.PosX to coord.initMove(MoveDirection.PosX),
//                        MoveDirection.NegX to coord.initMove(MoveDirection.NegX),
//                        MoveDirection.PosY to coord.initMove(MoveDirection.PosY),
//                        MoveDirection.NegY to coord.initMove(MoveDirection.NegY),
//                        MoveDirection.PosZ to coord.initMove(MoveDirection.PosZ),
//                        MoveDirection.NegZ to coord.initMove(MoveDirection.NegZ),
//                    )
//                    adjacentMap[coord] = adjacent
                }
            }
        }


        /**
         * Returns the Coordinate instance with the given letter and number.
         *
         * @param letter the letter coordinate.
         * @param number the number coordinate.
         * @return an existing Coordinate instance.
         * @throws IllegalArgumentException if a number is passed in that is outside the min and max
         * number of the letter passed in. If only one of letter or number is NULL, this will also
         * throw.
         */
        fun get(letter: LetterCoordinate, number: NumberCoordinate): Coordinate {
            if (number < letter.min || number > letter.max) {
                throw IllegalArgumentException("Coordinate $letter$number does not exist on the board")
            }
            return coordinates[hash(letter, number)]
        }

        /**
         * Returns a hash value of a letter and number.
         *
         * @param letter the letter coordinate.
         * @param number the number coordinate.
         * @return the hash value.
         */
        private fun hash(letter: LetterCoordinate, number: NumberCoordinate): Int {
            return letter.ordinal * 16 + number.ordinal
        }
    }

    /**
     * Provides the adjacent coordinates, i.e. the immediate neighbours of the coordinate.
     *
     * @return adjacent coordinates.
     */
    fun adjacentCoordinates(): Array<Pair<Coordinate, MoveDirection>> {
        val adjacent = listOf(
            move(MoveDirection.PosX) to MoveDirection.PosX,
            move(MoveDirection.NegX) to MoveDirection.NegX,
            move(MoveDirection.PosY) to MoveDirection.PosY,
            move(MoveDirection.NegY) to MoveDirection.NegY,
            move(MoveDirection.PosZ) to MoveDirection.PosZ,
            move(MoveDirection.NegZ) to MoveDirection.NegZ,
        )
        val onBoard = adjacent.filter { it.first != offBoard }
        return onBoard.toTypedArray()
    }

    /**
     * Returns the coordinate that results after the translation by 1 space in the given direction.
     *
     * @param direction the direction to move in.
     * @return the new coordinate. Does not mutate the original coordinate.
     */
    fun move(direction: MoveDirection): Coordinate {
        val newCoordinate = adjacentArray[this.hashCode() * 6 + direction.ordinal]
//        val newCoordinate = adjacentMap[this]!![direction]!!
        return newCoordinate
    }

    private fun initMove(direction: MoveDirection): Coordinate {
        val newCoordinate = when(direction) {
            MoveDirection.PosX -> moveX(true)
            MoveDirection.NegX -> moveX(false)
            MoveDirection.PosY -> moveY(true)
            MoveDirection.NegY -> moveY(false)
            MoveDirection.PosZ -> moveZ(true)
            MoveDirection.NegZ -> moveZ(false)
        }
        return newCoordinate
    }

    private fun moveX(forward: Boolean): Coordinate {
        val newCoordinate = Coordinate(letter, number + if (forward) 1 else -1)
        if (
            newCoordinate.number == NumberCoordinate.NULL
            || newCoordinate.letter < newCoordinate.number.min
            || newCoordinate.letter > newCoordinate.number.max
        ) {
            return offBoard
        }
        return newCoordinate
    }

    private fun moveY(forward: Boolean): Coordinate {
        val newCoordinate = Coordinate(letter + if (forward) 1 else -1, number)
        if (
            newCoordinate.letter == LetterCoordinate.NULL
            || newCoordinate.number < newCoordinate.letter.min
            || newCoordinate.number > newCoordinate.letter.max
        ) {
            return offBoard
        }
        return newCoordinate
    }

    private fun moveZ(forward: Boolean): Coordinate {
        val amt = if (forward) 1 else -1
        val newCoordinate = Coordinate(letter + amt, number + amt)
        if (
            newCoordinate.letter == LetterCoordinate.NULL
            || newCoordinate.number == NumberCoordinate.NULL
            || newCoordinate.letter < newCoordinate.number.min
            || newCoordinate.number < newCoordinate.letter.min
            || newCoordinate.letter > newCoordinate.number.max
            || newCoordinate.number > newCoordinate.letter.max
        ) {
            return offBoard
        }
        return newCoordinate
    }

    fun findDistanceFrom(letter: LetterCoordinate, number: NumberCoordinate): Int {
        // Calculate the distance between two coordinates
        val numberDiff = this.number.ordinal - number.ordinal
        val letterDiff = this.letter.ordinal - letter.ordinal
        return maxOf(abs(numberDiff), abs(letterDiff)) // Use Chebyshev distance for hex grids
    }

    override fun toString(): String = "$letter$number"

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is Coordinate) return false
        return other.letter == letter && other.number == number
    }

    override fun hashCode(): Int {
        return hash(letter, number)
    }
}

/**
 * The different values possible along the X axis of the game board, i.e. the axis along a
 * single letter-line.
 */
enum class NumberCoordinate {
    NULL, ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE;

    /** The minimum letter value of this line. */
    val min: LetterCoordinate get() = minMaxMapping[this]!!.first
    /** The maximum letter value of this line. */
    val max: LetterCoordinate get() = minMaxMapping[this]!!.second
    companion object {
        private val minMaxMapping = hashMapOf(
            NULL to (LetterCoordinate.NULL to LetterCoordinate.NULL),
            ONE to (LetterCoordinate.A to LetterCoordinate.E),
            TWO to (LetterCoordinate.A to LetterCoordinate.F),
            THREE to (LetterCoordinate.A to LetterCoordinate.G),
            FOUR to (LetterCoordinate.A to LetterCoordinate.H),
            FIVE to (LetterCoordinate.A to LetterCoordinate.I),
            SIX to (LetterCoordinate.B to LetterCoordinate.I),
            SEVEN to (LetterCoordinate.C to LetterCoordinate.I),
            EIGHT to (LetterCoordinate.D to LetterCoordinate.I),
            NINE to (LetterCoordinate.E to LetterCoordinate.I),
        )

        fun convertNumber(letter: String): NumberCoordinate {
            val numberCoordinate = when (letter) {
                "1" -> ONE
                "2" -> TWO
                "3" -> THREE
                "4" -> FOUR
                "5" -> FIVE
                "6" -> SIX
                "7" -> SEVEN
                "8" -> EIGHT
                "9" -> NINE
                else -> NULL
            }
            return numberCoordinate
        }
    }

    operator fun plus(amount: Int): NumberCoordinate {
        val sum = ordinal + amount
        if (sum < ONE.ordinal || sum > NINE.ordinal) return NULL
        return entries[sum]
    }

    operator fun minus(amount: Int): NumberCoordinate {
        return plus(-amount)
    }

    operator fun rangeTo(that: NumberCoordinate): Iterable<NumberCoordinate> {
        val newRange = (this.ordinal .. that.ordinal).map { enumValues<NumberCoordinate>()[it] }
        return newRange.asIterable()
    }

    infix fun downTo(that: NumberCoordinate): Iterable<NumberCoordinate> {
        val newRange = (this.ordinal downTo  that.ordinal).map { enumValues<NumberCoordinate>()[it] }
        return newRange.asIterable()
    }

    override fun toString(): String = ordinal.toString()
}

/**
 * The different values possible along the Y axis of the game board, i.e. the axis along a
 * single number-line.
 */
enum class LetterCoordinate {
    NULL, A, B, C, D, E, F, G, H, I;

    /** The minimum number value of this line. */
    val min: NumberCoordinate get() = minMaxMapping[this]!!.first
    /** The minimum number value of this line. */
    val max: NumberCoordinate get() = minMaxMapping[this]!!.second
    companion object {
        private val minMaxMapping = hashMapOf(
            NULL to (NumberCoordinate.NULL to NumberCoordinate.NULL),
            A to (NumberCoordinate.ONE to NumberCoordinate.FIVE),
            B to (NumberCoordinate.ONE to NumberCoordinate.SIX),
            C to (NumberCoordinate.ONE to NumberCoordinate.SEVEN),
            D to (NumberCoordinate.ONE to NumberCoordinate.EIGHT),
            E to (NumberCoordinate.ONE to NumberCoordinate.NINE),
            F to (NumberCoordinate.TWO to NumberCoordinate.NINE),
            G to (NumberCoordinate.THREE to NumberCoordinate.NINE),
            H to (NumberCoordinate.FOUR to NumberCoordinate.NINE),
            I to (NumberCoordinate.FIVE to NumberCoordinate.NINE),
        )
        fun convertLetter(letter: String): LetterCoordinate {
            val letterCoordinate = when (letter) {
                "A" -> A
                "B" -> B
                "C" -> C
                "D" -> D
                "E" -> E
                "F" -> F
                "G" -> G
                "H" -> H
                "I" -> I
                else -> NULL
            }
            return letterCoordinate
        }
    }

    operator fun plus(amount: Int): LetterCoordinate {
        val sum = ordinal + amount
        if (sum < A.ordinal || sum > I.ordinal) return NULL
        return entries[sum]
    }

    operator fun minus(amount: Int): LetterCoordinate {
        return plus(-amount)
    }

    operator fun rangeTo(that: LetterCoordinate): Iterable<LetterCoordinate> {
        val newRange = (this.ordinal .. that.ordinal).map { enumValues<LetterCoordinate>()[it] }
        return newRange.asIterable()
    }

    infix fun downTo(that: LetterCoordinate): Iterable<LetterCoordinate> {
        val newRange = (this.ordinal downTo  that.ordinal).map { enumValues<LetterCoordinate>()[it] }
        return newRange.asIterable()
    }

    override fun toString(): String = name
}

