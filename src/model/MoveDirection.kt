package abalone.model

/**
 * Represents all possible directions of movement.
 *
 */
enum class MoveDirection(private val stringRepresentation: String) {
    PosX("+X"),
    NegX("-X"),
    PosY("+Y"),
    NegY("-Y"),
    PosZ("+Z"),
    NegZ("-Z");

    fun opposite(): MoveDirection {
        val ord = ordinal
        val oppositeOrd = if (ordinal % 2 == 0) ord + 1 else ord - 1
        return MoveDirection.entries[oppositeOrd]
    }

    override fun toString(): String {
        return stringRepresentation
    }
}
