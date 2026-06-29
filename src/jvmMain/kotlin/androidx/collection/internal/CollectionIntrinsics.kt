@file:Suppress("FunctionName", "unused")

package androidx.collection.internal

/**
 * JVM implementations of the JS-only intrinsics. These mirror the C functions in
 * IntSetBuiltins.cpp so that the existing test suite (which runs on JVM) can exercise
 * the full code paths that the JS hot paths use.
 *
 * The data layout is identical to the C side: `metadataFlat` is a parallel Int32Array
 * whose low 8 bits per element hold the metadata byte for that slot
 * (0x80 = Empty, 0xFE = Deleted, 0xFF = Sentinel, 0x00..0x7F = hash2 of the element at that slot).
 *
 * `probeMask` is `capacity` (not `capacity - 1`): we mask to capacity-1 internally, then
 * mask again at the end. (The original 1.5.0 source uses `& _capacity` so we follow that.)
 */
private const val META_EMPTY: Int = 0x80
private const val META_DELETED: Int = 0xFE
private const val META_SENTINEL: Int = 0xFF

private fun readMetaByte(flat: IntArray, offset: Int): Int = flat[offset] and 0xFF

private fun writeMetaByte(flat: IntArray, offset: Int, byte: Int) {
    flat[offset] = byte and 0xFF
}

// Build the 8-byte group as a Long. Byte 0 of the group = flat[offset], byte 1 = flat[offset+1], etc.
// capacity must be a power of 2 (>= 8).
private fun loadGroup(flat: IntArray, offset: Int, capacity: Int): Long {
    val mask = capacity - 1
    var g: Long = 0L
    for (i in 0 until 8) {
        val o = (offset + i) and mask
        g = g or (readMetaByte(flat, o).toLong() shl (i * 8))
    }
    return g
}

// Match the 8-byte group against hash2 (0..127). Each set bit 8k+7 indicates
// that byte k matches hash2. Mirrors the C SIMD bit trick:
//   m = (x - 0x01010101) & ~x & 0x80808080
// where x = g XOR (hash2 * 0x01010101...)
private fun matchHash2(g: Long, hash2: Int): Long {
    val xored = g xor (hash2.toLong() * 0x0101010101010101L)
    return (xored - 0x0101010101010101L) and (xored.inv()) and -0x7f7f7f7f7f7f7f80L
}

private fun anyEmptyOrDeleted(g: Long): Boolean {
    val emptyMask = matchHash2(g, META_EMPTY)
    val deletedMask = matchHash2(g, META_DELETED)
    return (emptyMask or deletedMask) != 0L
}

private fun firstEmptyOrDeleted(g: Long, probeOffset: Int, capacity: Int): Int {
    val emptyMask = matchHash2(g, META_EMPTY)
    val deletedMask = matchHash2(g, META_DELETED)
    val mask = emptyMask or deletedMask
    if (mask == 0L) return -1
    val bitIndex = java.lang.Long.numberOfTrailingZeros(mask)
    val byteInGroup = bitIndex shr 3
    val maskIdx = capacity - 1
    return (probeOffset + byteInGroup) and maskIdx
}

// ============================================================================
// IntObjectMap / IntObjectMapPut / etc.
// ============================================================================

internal actual fun _intObjectMapFind(
    metadataFlat: IntArray,
    keys: IntArray,
    capacity: Int,
    key: Int,
    hash: Int,
    hash2: Int,
): Int {
    if (metadataFlat.isEmpty()) return -1
    val probeMask = capacity  // matches Kotlin source: `& _capacity`
    var probeOffset = (hash ushr 7) and probeMask
    var probeIndex = 0
    while (true) {
        val g = loadGroup(metadataFlat, probeOffset, capacity)
        val m = matchHash2(g, hash2)
        var bitIdx = m
        while (bitIdx != 0L) {
            val lowBit = java.lang.Long.numberOfTrailingZeros(bitIdx)
            val byteInGroup = lowBit shr 3
            val index = (probeOffset + byteInGroup) and (capacity - 1)
            if (keys[index] == key) return index
            bitIdx = bitIdx and (bitIdx - 1L)
        }
        if (anyEmptyOrDeleted(g)) return -1
        probeIndex += 8
        probeOffset = (probeOffset + probeIndex) and probeMask
    }
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
    if (metadataFlat.isEmpty()) {
        outCreated[0] = 0
        outSizeDelta[0] = 0
        return -1
    }
    val probeMask = capacity
    var probeOffset = (hash ushr 7) and probeMask
    var probeIndex = 0
    var insertSlot = -1
    while (true) {
        val g = loadGroup(metadataFlat, probeOffset, capacity)
        val m = matchHash2(g, hash2)
        var bitIdx = m
        while (bitIdx != 0L) {
            val lowBit = java.lang.Long.numberOfTrailingZeros(bitIdx)
            val byteInGroup = lowBit shr 3
            val index = (probeOffset + byteInGroup) and (capacity - 1)
            if (keys[index] == key) {
                outCreated[0] = 0
                outSizeDelta[0] = 0
                return index
            }
            bitIdx = bitIdx and (bitIdx - 1L)
        }
        val slot = firstEmptyOrDeleted(g, probeOffset, capacity)
        if (slot >= 0) { insertSlot = slot; break }
        probeIndex += 8
        probeOffset = (probeOffset + probeIndex) and probeMask
    }
    writeMetaByte(metadataFlat, insertSlot, hash2)
    keys[insertSlot] = key
    outCreated[0] = 1
    outSizeDelta[0] = 1
    return insertSlot
}

internal actual fun _intObjectMapRemove(
    metadataFlat: IntArray,
    keys: IntArray,
    capacity: Int,
    key: Int,
    hash: Int,
    hash2: Int,
): Int {
    if (metadataFlat.isEmpty()) return -1
    val probeMask = capacity
    var probeOffset = (hash ushr 7) and probeMask
    var probeIndex = 0
    while (true) {
        val g = loadGroup(metadataFlat, probeOffset, capacity)
        val m = matchHash2(g, hash2)
        var bitIdx = m
        while (bitIdx != 0L) {
            val lowBit = java.lang.Long.numberOfTrailingZeros(bitIdx)
            val byteInGroup = lowBit shr 3
            val index = (probeOffset + byteInGroup) and (capacity - 1)
            if (keys[index] == key) {
                writeMetaByte(metadataFlat, index, META_DELETED)
                return index
            }
            bitIdx = bitIdx and (bitIdx - 1L)
        }
        // No match in this group. Check if any Empty (probe terminator).
        val emptyMask = matchHash2(g, META_EMPTY)
        if (emptyMask != 0L) return -1
        probeIndex += 8
        probeOffset = (probeOffset + probeIndex) and probeMask
    }
}

internal actual fun _intObjectMapFindAvailableSlot(
    metadataFlat: IntArray,
    capacity: Int,
    hash1: Int,
): Int {
    if (metadataFlat.isEmpty()) return -1
    val probeMask = capacity
    var probeOffset = hash1 and probeMask
    var probeIndex = 0
    while (true) {
        val g = loadGroup(metadataFlat, probeOffset, capacity)
        val slot = firstEmptyOrDeleted(g, probeOffset, capacity)
        if (slot >= 0) return slot
        probeIndex += 8
        probeOffset = (probeOffset + probeIndex) and probeMask
    }
}

// ============================================================================
// IntSet (uses IntArray for elements)
// ============================================================================

internal actual fun _intsetFind(
    metadataFlat: IntArray,
    elements: IntArray,
    capacity: Int,
    element: Int,
    hash: Int,
    hash2: Int,
): Int {
    if (metadataFlat.isEmpty()) return -1
    val probeMask = capacity
    var probeOffset = (hash ushr 7) and probeMask
    var probeIndex = 0
    while (true) {
        val g = loadGroup(metadataFlat, probeOffset, capacity)
        val m = matchHash2(g, hash2)
        var bitIdx = m
        while (bitIdx != 0L) {
            val lowBit = java.lang.Long.numberOfTrailingZeros(bitIdx)
            val byteInGroup = lowBit shr 3
            val index = (probeOffset + byteInGroup) and (capacity - 1)
            if (elements[index] == element) return index
            bitIdx = bitIdx and (bitIdx - 1L)
        }
        if (anyEmptyOrDeleted(g)) return -1
        probeIndex += 8
        probeOffset = (probeOffset + probeIndex) and probeMask
    }
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
    if (metadataFlat.isEmpty()) {
        outCreated[0] = 0
        outSizeDelta[0] = 0
        return -1
    }
    val probeMask = capacity
    var probeOffset = (hash ushr 7) and probeMask
    var probeIndex = 0
    var insertSlot = -1
    while (true) {
        val g = loadGroup(metadataFlat, probeOffset, capacity)
        val m = matchHash2(g, hash2)
        var bitIdx = m
        while (bitIdx != 0L) {
            val lowBit = java.lang.Long.numberOfTrailingZeros(bitIdx)
            val byteInGroup = lowBit shr 3
            val index = (probeOffset + byteInGroup) and (capacity - 1)
            if (elements[index] == element) {
                outCreated[0] = 0
                outSizeDelta[0] = 0
                return index
            }
            bitIdx = bitIdx and (bitIdx - 1L)
        }
        val slot = firstEmptyOrDeleted(g, probeOffset, capacity)
        if (slot >= 0) { insertSlot = slot; break }
        probeIndex += 8
        probeOffset = (probeOffset + probeIndex) and probeMask
    }
    writeMetaByte(metadataFlat, insertSlot, hash2)
    elements[insertSlot] = element
    outCreated[0] = 1
    outSizeDelta[0] = 1
    return insertSlot
}

internal actual fun _intsetRemove(
    metadataFlat: IntArray,
    elements: IntArray,
    capacity: Int,
    element: Int,
    hash: Int,
    hash2: Int,
): Int {
    if (metadataFlat.isEmpty()) return -1
    val probeMask = capacity
    var probeOffset = (hash ushr 7) and probeMask
    var probeIndex = 0
    while (true) {
        val g = loadGroup(metadataFlat, probeOffset, capacity)
        val m = matchHash2(g, hash2)
        var bitIdx = m
        while (bitIdx != 0L) {
            val lowBit = java.lang.Long.numberOfTrailingZeros(bitIdx)
            val byteInGroup = lowBit shr 3
            val index = (probeOffset + byteInGroup) and (capacity - 1)
            if (elements[index] == element) {
                writeMetaByte(metadataFlat, index, META_DELETED)
                return index
            }
            bitIdx = bitIdx and (bitIdx - 1L)
        }
        val emptyMask = matchHash2(g, META_EMPTY)
        if (emptyMask != 0L) return -1
        probeIndex += 8
        probeOffset = (probeOffset + probeIndex) and probeMask
    }
}

// ============================================================================
// ScatterSet: elements is Array<Any?>, compare with === (JS strict equality).
// JVM uses Object identity, which matches === for non-overloaded Any? references.
// ============================================================================

internal actual fun _scatterSetFind(
    metadataFlat: IntArray,
    elements: Array<Any?>,
    capacity: Int,
    element: Any?,
    hash: Int,
    hash2: Int,
): Int {
    if (metadataFlat.isEmpty()) return -1
    val probeMask = capacity
    var probeOffset = (hash ushr 7) and probeMask
    var probeIndex = 0
    while (true) {
        val g = loadGroup(metadataFlat, probeOffset, capacity)
        val m = matchHash2(g, hash2)
        var bitIdx = m
        while (bitIdx != 0L) {
            val lowBit = java.lang.Long.numberOfTrailingZeros(bitIdx)
            val byteInGroup = lowBit shr 3
            val index = (probeOffset + byteInGroup) and (capacity - 1)
            if (elements[index] === element) return index
            bitIdx = bitIdx and (bitIdx - 1L)
        }
        if (anyEmptyOrDeleted(g)) return -1
        probeIndex += 8
        probeOffset = (probeOffset + probeIndex) and probeMask
    }
}