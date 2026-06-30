@file:Suppress("FunctionName", "unused")

package androidx.collection.internal

private const val GROUP_WIDTH = 8
private const val EMPTY = 0x80L
private const val DELETED = 0xFEL

private val EMPTY_BYTE_MASK = 0x8080808080808080UL.toLong()
private val DELETED_BYTE_MASK = 0xFEFEFEFEFEFEFEFEUL.toLong()
private val HIGH_BIT_MASK = 0x8080808080808080UL.toLong()
private val REPEATED_ONE = 0x0101010101010101L

internal actual fun _scatterSetFind(
    metadataFlat: IntArray,
    elements: Array<Any?>,
    capacity: Int,
    element: Any?,
    hash: Int,
    hash2: Int,
): Int {
    val mask = capacity
    var probeOffset = h1(hash) and mask
    var probeIndex = 0
    while (true) {
        val g = loadGroup(metadataFlat, probeOffset)
        var m = match(g, hash2)
        while (m != 0L) {
            val byteInGroup = m.countTrailingZeroBits() shr 3
            val index = (probeOffset + byteInGroup) and mask
            if (elements[index] == element) {
                return index
            }
            m = m and (m - 1)
        }
        if (hasEmpty(g)) break
        probeIndex += GROUP_WIDTH
        probeOffset = (probeOffset + probeIndex) and mask
    }
    return -1
}

internal actual fun _scatterSetFindSlot(
    metadataFlat: IntArray,
    elements: Array<Any?>,
    capacity: Int,
    element: Any?,
    hash: Int,
    hash2: Int,
    emptySlot: IntArray,
): Int {
    val probeMask = capacity
    var probeOffset = h1(hash) and probeMask
    var probeIndex = 0

    while (true) {
        val g = loadGroup(metadataFlat, probeOffset)
        var m = match(g, hash2)
        while (m.hasNext()) {
            val index = (probeOffset + m.get()) and probeMask
            if (elements[index] == element) {
                return index
            }
            m = m.next()
        }

        if (maskEmpty(g) != 0L) {
            break
        }

        probeIndex += GROUP_WIDTH
        probeOffset = (probeOffset + probeIndex) and probeMask
    }
    emptySlot[0] = findFirstAvailableSlot(metadataFlat, capacity, h1(hash))
    return -1
}

internal actual fun _scatterSetRemove(
    metadataFlat: IntArray,
    elements: Array<Any?>,
    capacity: Int,
    element: Any?,
    hash: Int,
    hash2: Int,
): Int {
    val mask = capacity
    var probeOffset = h1(hash) and mask
    var probeIndex = 0

    while (true) {
        val g = loadGroup(metadataFlat, probeOffset)
        var m = match(g, hash2)
        while (m != 0L) {
            val byteInGroup = m.countTrailingZeroBits() shr 3
            val index = (probeOffset + byteInGroup) and mask
            if (elements[index] == element) {
                writeByte(metadataFlat, index, DELETED)
                elements[index] = null
                return index
            }
            m = m and (m - 1)
        }
        if (hasEmpty(g)) break
        probeIndex += GROUP_WIDTH
        probeOffset = (probeOffset + probeIndex) and mask
    }
    return -1
}

internal actual fun _scatterMapFindSlot(
    metadataFlat: IntArray,
    keys: Array<Any?>,
    capacity: Int,
    key: Any?,
    hash: Int,
    hash2: Int,
    emptySlot: IntArray,
): Int {
    val probeMask = capacity
    var probeOffset = h1(hash) and probeMask
    var probeIndex = 0

    while (true) {
        val g = loadGroup(metadataFlat, probeOffset)
        var m = match(g, hash2)
        while (m.hasNext()) {
            val index = (probeOffset + m.get()) and probeMask
            if (keys[index] == key) {
                return index
            }
            m = m.next()
        }

        if (maskEmpty(g) != 0L) {
            break
        }

        probeIndex += GROUP_WIDTH
        probeOffset = (probeOffset + probeIndex) and probeMask
    }
    emptySlot[0] = findFirstAvailableSlot(metadataFlat, capacity, h1(hash))
    return -1
}

internal actual fun _scatterMapFind(
    metadataFlat: IntArray,
    keys: Array<Any?>,
    capacity: Int,
    key: Any?,
    hash: Int,
    hash2: Int,
): Int {
    val probeMask = capacity
    var probeOffset = h1(hash) and probeMask
    var probeIndex = 0

    while (true) {
        val g = loadGroup(metadataFlat, probeOffset)
        var m = match(g, hash2)
        while (m.hasNext()) {
            val index = (probeOffset + m.get()) and probeMask
            if (keys[index] == key) {
                return index
            }
            m = m.next()
        }

        if (maskEmpty(g) != 0L) {
            break
        }

        probeIndex += GROUP_WIDTH
        probeOffset = (probeOffset + probeIndex) and probeMask
    }
    return -1
}

internal actual fun _scatterMapRemove(
    metadataFlat: IntArray,
    keys: Array<Any?>,
    values: Array<Any?>,
    capacity: Int,
    key: Any?,
    hash: Int,
    hash2: Int,
): Int {
    val probeMask = capacity
    var probeOffset = h1(hash) and probeMask
    var probeIndex = 0

    while (true) {
        val g = loadGroup(metadataFlat, probeOffset)
        var m = match(g, hash2)
        while (m.hasNext()) {
            val index = (probeOffset + m.get()) and probeMask
            if (keys[index] == key) {
                writeByte(metadataFlat, index, DELETED)
                keys[index] = null
                values[index] = null
                return index
            }
            m = m.next()
        }

        if (maskEmpty(g) != 0L) {
            break
        }

        probeIndex += GROUP_WIDTH
        probeOffset = (probeOffset + probeIndex) and probeMask
    }
    return -1
}

internal actual fun _intsetFind(
    metadataFlat: IntArray,
    elements: IntArray,
    capacity: Int,
    element: Int,
    hash: Int,
    hash2: Int,
): Int {
    val probeMask = capacity
    var probeOffset = h1(hash) and probeMask
    var probeIndex = 0

    while (true) {
        val g = loadGroup(metadataFlat, probeOffset)
        var m = match(g, hash2)
        while (m.hasNext()) {
            val index = (probeOffset + m.get()) and probeMask
            if (elements[index] == element) {
                return index
            }
            m = m.next()
        }

        if (maskEmpty(g) != 0L) {
            break
        }

        probeIndex += GROUP_WIDTH
        probeOffset = (probeOffset + probeIndex) and probeMask
    }
    return -1
}

internal actual fun _intsetAdd(
    metadataFlat: IntArray,
    elements: IntArray,
    capacity: Int,
    element: Int,
    hash: Int,
    hash2: Int,
    outCreated: IntArray,
    outSizeDelta: IntArray,
): Int {
    val probeMask = capacity
    var probeOffset = h1(hash) and probeMask
    var probeIndex = 0

    while (true) {
        val g = loadGroup(metadataFlat, probeOffset)
        var m = match(g, hash2)
        while (m.hasNext()) {
            val index = (probeOffset + m.get()) and probeMask
            if (elements[index] == element) {
                outCreated[0] = 0
                outSizeDelta[0] = 0
                return index
            }
            m = m.next()
        }

        if (maskEmpty(g) != 0L) {
            val emptyIndex = (probeOffset + m.get()) and probeMask
            writeByte(metadataFlat, emptyIndex, hash2.toLong())
            elements[emptyIndex] = element
            outCreated[0] = 1
            outSizeDelta[0] = 1
            return emptyIndex
        }

        probeIndex += GROUP_WIDTH
        probeOffset = (probeOffset + probeIndex) and probeMask
    }
}

internal actual fun _intsetRemove(
    metadataFlat: IntArray,
    elements: IntArray,
    capacity: Int,
    element: Int,
    hash: Int,
    hash2: Int,
): Int {
    val probeMask = capacity
    var probeOffset = h1(hash) and probeMask
    var probeIndex = 0

    while (true) {
        val g = loadGroup(metadataFlat, probeOffset)
        var m = match(g, hash2)
        while (m.hasNext()) {
            val index = (probeOffset + m.get()) and probeMask
            if (elements[index] == element) {
                writeByte(metadataFlat, index, DELETED)
                elements[index] = 0
                return index
            }
            m = m.next()
        }

        if (maskEmpty(g) != 0L) {
            break
        }

        probeIndex += GROUP_WIDTH
        probeOffset = (probeOffset + probeIndex) and probeMask
    }
    return -1
}

internal actual fun _intObjectMapFind(
    metadataFlat: IntArray,
    keys: IntArray,
    capacity: Int,
    key: Int,
    hash: Int,
    hash2: Int,
): Int {
    val probeMask = capacity
    var probeOffset = h1(hash) and probeMask
    var probeIndex = 0

    while (true) {
        val g = loadGroup(metadataFlat, probeOffset)
        var m = match(g, hash2)
        while (m.hasNext()) {
            val index = (probeOffset + m.get()) and probeMask
            if (keys[index] == key) {
                return index
            }
            m = m.next()
        }

        if (maskEmpty(g) != 0L) {
            break
        }

        probeIndex += GROUP_WIDTH
        probeOffset = (probeOffset + probeIndex) and probeMask
    }
    return -1
}

internal actual fun _intObjectMapPut(
    metadataFlat: IntArray,
    keys: IntArray,
    capacity: Int,
    key: Int,
    hash: Int,
    hash2: Int,
    outCreated: IntArray,
    outSizeDelta: IntArray,
): Int {
    val probeMask = capacity
    var probeOffset = h1(hash) and probeMask
    var probeIndex = 0

    while (true) {
        val g = loadGroup(metadataFlat, probeOffset)
        var m = match(g, hash2)
        while (m.hasNext()) {
            val index = (probeOffset + m.get()) and probeMask
            if (keys[index] == key) {
                outCreated[0] = 0
                outSizeDelta[0] = 0
                return index
            }
            m = m.next()
        }

        if (maskEmpty(g) != 0L) {
            val emptyIndex = (probeOffset + m.get()) and probeMask
            writeByte(metadataFlat, emptyIndex, hash2.toLong())
            keys[emptyIndex] = key
            outCreated[0] = 1
            outSizeDelta[0] = 1
            return emptyIndex
        }

        probeIndex += GROUP_WIDTH
        probeOffset = (probeOffset + probeIndex) and probeMask
    }
}

internal actual fun _intObjectMapRemove(
    metadataFlat: IntArray,
    keys: IntArray,
    capacity: Int,
    key: Int,
    hash: Int,
    hash2: Int,
): Int {
    val probeMask = capacity
    var probeOffset = h1(hash) and probeMask
    var probeIndex = 0

    while (true) {
        val g = loadGroup(metadataFlat, probeOffset)
        var m = match(g, hash2)
        while (m.hasNext()) {
            val index = (probeOffset + m.get()) and probeMask
            if (keys[index] == key) {
                writeByte(metadataFlat, index, DELETED)
                keys[index] = 0
                return index
            }
            m = m.next()
        }

        if (maskEmpty(g) != 0L) {
            break
        }

        probeIndex += GROUP_WIDTH
        probeOffset = (probeOffset + probeIndex) and probeMask
    }
    return -1
}

internal actual fun _intObjectMapFindAvailableSlot(
    metadataFlat: IntArray,
    capacity: Int,
    hash1: Int,
): Int {
    return findFirstAvailableSlot(metadataFlat, capacity, hash1)
}

private fun findFirstAvailableSlot(metadataFlat: IntArray, capacity: Int, hash1: Int): Int {
    val probeMask = capacity
    var probeOffset = hash1 and probeMask
    var probeIndex = 0
    while (true) {
        val g = loadGroup(metadataFlat, probeOffset)
        val m = maskEmptyOrDeleted(g)
        if (m != 0L) {
            return (probeOffset + m.lowestBitSet()) and probeMask
        }
        probeIndex += GROUP_WIDTH
        probeOffset = (probeOffset + probeIndex) and probeMask
    }
}

private fun loadGroup(metadata: IntArray, offset: Int): Long {
    val i = offset shr 3
    val b = (offset and 0x7) shl 3
    val asLong = IntAsLongArray(metadata)
    val result = (asLong[i] ushr b) or (asLong[i + 1] shl (64 - b) and (-(b.toLong()) shr 63))
    return result
}

private fun match(g: Long, hash2: Int): Long {
    val x = g xor (hash2.toLong() * REPEATED_ONE)
    return (x - REPEATED_ONE) and x.inv() and HIGH_BIT_MASK
}

private fun hasEmpty(g: Long): Boolean = maskEmpty(g) != 0L

private fun maskEmpty(g: Long): Long {
    val x = g xor EMPTY_BYTE_MASK
    return (x - REPEATED_ONE) and x.inv() and HIGH_BIT_MASK
}

private fun maskEmptyOrDeleted(g: Long): Long {
    val x = g xor EMPTY_BYTE_MASK
    val y = g xor DELETED_BYTE_MASK
    return ((x - REPEATED_ONE) and x.inv() and HIGH_BIT_MASK) or
           ((y - REPEATED_ONE) and y.inv() and HIGH_BIT_MASK)
}

private fun readByte(metadata: IntArray, slot: Int): Long {
    val intIdx = slot shr 2
    val byteShift = (slot and 0x3) shl 3
    return ((metadata[intIdx] ushr byteShift) and 0xFF).toLong()
}

private fun writeByte(metadata: IntArray, slot: Int, value: Long) {
    val intIdx = slot shr 2
    val byteShift = (slot and 0x3) shl 3
    val byteMask = 0xFF shl byteShift

    val old = metadata[intIdx]
    val new = (old and byteMask.inv()) or ((value.toInt() and 0xFF) shl byteShift)

    metadata[intIdx] = new
}

private fun Long.hasNext(): Boolean = this != 0L
private fun Long.get(): Int = this.countTrailingZeroBits() shr 3
private fun Long.next(): Long = this and (this - 1)
private fun Long.lowestBitSet(): Int = this.countTrailingZeroBits()

private inline fun h1(hash: Int) = hash ushr 7
