package abalone.model

/**
 * Can be thrown when a part of the state space generation determines that a move is invalid.
 */
class InvalidMoveException(message: String):  Exception(message)
