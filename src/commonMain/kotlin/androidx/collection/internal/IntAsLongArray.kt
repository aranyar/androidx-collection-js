package androidx.collection.internal

/**
 * A drop‑in replacement for [LongArray] that stores each 64‑bit value as two 32‑bit integers
 * in an internal [IntArray] of length `2 * size`. All operations are O(1) and inline‑friendly.
 */
class IntAsLongArray(val data: IntArray) {

    /** Creates a new array of the specified [size], filled with zeros. */
    constructor(size: Int) : this(IntArray(size * 2))

    /** Returns the number of elements in this array. */
    val size: Int get() = data.size / 2

    /** Returns the index of the last element, or -1 if empty. */
    val lastIndex: Int get() = size - 1

    /**
     * Reads the [Long] value at the given [index].
     * @throws IndexOutOfBoundsException if [index] is out of range.
     */
    operator fun get(index: Int): Long {
        val i = index * 2
        return (data[i + 1].toLong() shl 32) or (data[i].toLong() and 0xFFFFFFFFL)
    }

    /**
     * Writes the [Long] [value] at the given [index].
     * @throws IndexOutOfBoundsException if [index] is out of range.
     */
    operator fun set(index: Int, value: Long) {
        val i = index * 2
        data[i] = (value and 0xFFFFFFFFL).toInt()
        data[i + 1] = ((value shr 32) and 0xFFFFFFFFL).toInt()
    }

    /** Fills the entire array with the given [value]. */
    fun fill(value: Long) {
        val low = (value and 0xFFFFFFFFL).toInt()
        val high = ((value shr 32) and 0xFFFFFFFFL).toInt()
        var i = 0
        while (i < data.size) {
            data[i] = low
            data[i + 1] = high
            i += 2
        }
    }

    /** Returns a copy of this array. */
    fun copyOf(): IntAsLongArray = IntAsLongArray(data.copyOf())

    /**
     * Returns a new array with the specified [newSize], either truncating or padding with zeros.
     */
    fun copyOf(newSize: Int): IntAsLongArray =
        IntAsLongArray(data.copyOf(newSize * 2))

    /** Returns the underlying [IntArray] (use with caution – changes affect this instance). */
    fun toIntArray(): IntArray = data

    override fun equals(other: Any?): Boolean =
        other is IntAsLongArray && data.contentEquals(other.data)

    override fun hashCode(): Int = data.contentHashCode()

    override fun toString(): String {
        if (size == 0) return "[]"
        return buildString {
            append('[')
            for (i in 0 until size) {
                if (i > 0) append(", ")
                append(get(i))
            }
            append(']')
        }
    }

    companion object {
        /** An empty [IntAsLongArray]. */
        val EMPTY = IntAsLongArray(0)
    }
}
