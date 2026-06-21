package abalone.model.search

import abalone.model.Piece
import abalone.model.Action
import abalone.model.BoardMap
import java.util.LinkedHashMap
import kotlin.random.Random

/**
 * A least-recently-used cache to be used as a transposition table.
 *
 * Source: https://medium.com/@fasilt/understanding-lru-least-recently-used-cache-in-kotlin-b54c7060e752
 */
class TranspositionTable(
    private val capacity: Int
) : LinkedHashMap<TranspositionTable.Key, TranspositionTable.Entry>(
    capacity,
    0.75f,
    true
) {
    private var insertedCount = 0

    override fun put(key: Key, value: Entry): Entry? {
        insertedCount++
        val current = this[key]
        if (current != null && value.depth < current.depth) {
            return null
        }
        try {
            return super.put(key, value)
        } catch (e: ClassCastException) {
            println("ERROR: $e")
            return null
        }
    }

    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, Entry>?): Boolean {
        return size > capacity
    }

    fun collisions(): Int {
        val map = HashMap<Int, Int>()
        for (value in this.values) {
            if (map[value.hashCode()] == null)
                map[value.hashCode()] = 0
            map[value.hashCode()] = map[value.hashCode()]!! + 1
        }
        var sum = 0
        for (value in map.values) {
            if (value > 1) sum += value - 1
        }
        return sum
    }

    fun totalInserted(): Int {
        return insertedCount
    }

    data class Key(
        val board: BoardMap,
        val move: Piece,
    ) {
        companion object {
            private val turnHash = IntArray(4)

            init {
                val random = Random(1)
                turnHash[0] = random.nextInt()
                turnHash[1] = random.nextInt()
                turnHash[2] = random.nextInt()
                turnHash[3] = random.nextInt()
            }
        }

        override fun hashCode(): Int {
            val hash = board.hashCode() xor turnHash[move.ordinal]
            return hash
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Key) return false
            return board == other.board && move == other.move
        }
    }

    data class Entry(
        val value: Double,
        val action: Action,
        val depth: Int,
    )
}
