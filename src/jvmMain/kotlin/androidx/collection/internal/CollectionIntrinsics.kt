package androidx.collection.internal

import androidx.collection.h1
import androidx.collection.h2

private const val GroupWidth = 8
private const val Empty = 0x80L
private const val Deleted = 0xFEL

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
    hash2: Int
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
        probeIndex += GroupWidth
        probeOffset = (probeOffset + probeIndex) and mask
    }
    return -1
}

internal actual fun _scatterSetAdd(
    metadata: IntArray,
    elements: Array<Any?>,
    capacity: Int,
    element: Any?,
    hash: Int,
    hash2: Int,
    outCreated: IntArray,
    outSizeDelta: IntArray,
    outWasEmpty: IntArray
): Int {
    val mask = capacity - 1
    var probeOffset = h1(hash) and mask
    var probeIndex = 0
    var insertSlot = -1

    while (true) {
        val g = loadGroup(metadata, probeOffset)
        var m = match(g, hash2)
        while (m != 0L) {
            val byteInGroup = m.countTrailingZeroBits() shr 3
            val index = (probeOffset + byteInGroup) and mask
            if (elements[index] == element) {
                outCreated[0] = 0
                outSizeDelta[0] = 0
                outWasEmpty[0] = 0
                return index
            }
            m = m and (m - 1)
        }
        val slot = firstEmptyOrDeleted(g, probeOffset, capacity)
        if (slot >= 0) {
            insertSlot = slot
            break
        }
        probeIndex += GroupWidth
        probeOffset = (probeOffset + probeIndex) and mask
    }

    val oldByte = readByte(metadata, insertSlot)
    val isEmpty = oldByte == Empty

    writeByte(metadata, insertSlot, hash2.toLong())
    elements[insertSlot] = element

    outCreated[0] = 1
    outSizeDelta[0] = 1
    outWasEmpty[0] = if (isEmpty) 1 else 0
    return insertSlot
}

internal actual fun _scatterSetRemove(
    metadataFlat: IntArray,
    elements: Array<Any?>,
    capacity: Int,
    element: Any?,
    hash: Int,
    hash2: Int
): Int {
    val mask = capacity - 1
    var probeOffset = h1(hash) and mask
    var probeIndex = 0

    while (true) {
        val g = loadGroup(metadataFlat, probeOffset)
        var m = match(g, hash2)
        while (m != 0L) {
            val byteInGroup = m.countTrailingZeroBits() shr 3
            val index = (probeOffset + byteInGroup) and mask
            if (elements[index] == element) {
                writeByte(metadataFlat, index, Deleted)
                elements[index] = null
                return index
            }
            m = m and (m - 1)
        }
        if (hasEmpty(g)) break
        probeIndex += GroupWidth
        probeOffset = (probeOffset + probeIndex) and mask
    }
    return -1
}

private fun loadGroup(metadata: IntArray, offset: Int): Long {
    val i = offset shr 3
    val b = (offset and 0x7) shl 3
    val asLong = IntAsLongArray(metadata)
    return (asLong[i] ushr b) or (asLong[i + 1] shl (64 - b) and (-(b.toLong()) shr 63))
}

private fun match(g: Long, hash2: Int): Long {
    val x = g xor (hash2.toLong() * REPEATED_ONE)
    return (x - REPEATED_ONE) and x.inv() and HIGH_BIT_MASK
}

private fun hasEmpty(g: Long): Boolean = maskEmpty(g) != 0L

private fun firstEmptyOrDeleted(g: Long, probeOffset: Int, capacity: Int): Int {
    val mask = maskEmpty(g) or maskDeleted(g)
    if (mask == 0L) return -1
    val bitIndex = mask.countTrailingZeroBits()
    val byteInGroup = bitIndex shr 3
    return (probeOffset + byteInGroup) and (capacity - 1)
}

private fun maskEmpty(g: Long): Long {
    val x = g xor EMPTY_BYTE_MASK
    return (x - REPEATED_ONE) and x.inv() and HIGH_BIT_MASK
}

private fun maskDeleted(g: Long): Long {
    val x = g xor DELETED_BYTE_MASK
    return (x - REPEATED_ONE) and x.inv() and HIGH_BIT_MASK
}

private fun readByte(metadata: IntArray, slot: Int): Long {
    val longIdx = slot * 2
    val low = metadata[longIdx].toLong() and 0xFFFFFFFFL
    val high = metadata[longIdx + 1].toLong() and 0xFFFFFFFFL
    val combined = (high shl 32) or low
    val byteShift = (slot and 0x7) shl 3
    return (combined ushr byteShift) and 0xFFL
}

private fun writeByte(metadata: IntArray, slot: Int, value: Long) {
    val longIdx = slot * 2
    val byteShift = (slot and 0x7) shl 3
    val byteMask = 0xFFL shl byteShift

    val low = metadata[longIdx].toLong() and 0xFFFFFFFFL
    val high = (metadata[longIdx + 1].toLong() and 0xFFFFFFFFL) shl 32
    var combined = (high shl 32) or low

    combined = (combined and byteMask.inv()) or ((value and 0xFFL) shl byteShift)

    metadata[longIdx] = (combined and 0xFFFFFFFFL).toInt()
    metadata[longIdx + 1] = ((combined shr 32) and 0xFFFFFFFFL).toInt()
}
