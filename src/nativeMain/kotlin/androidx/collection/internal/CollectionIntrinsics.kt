@file:Suppress("FunctionName", "unused")

package androidx.collection.internal

/**
 * Stubs for non-JS targets. The hot paths that use these intrinsics (IntSet/IntObjectMap
 * find/add/remove) are only invoked from JS compose-live UI. On native these would throw
 * at runtime if reached, but they aren't called.
 */
internal actual fun _scatterSetFind(
    metadataFlat: LongArray,
    elements: Array<Any?>,
    capacity: Int,
    element: Any?,
    hash: Int,
    hash2: Int,
): Int = error("JS-only intrinsic")

internal actual fun _scatterSetFindSlot(
    metadataFlat: LongArray,
    elements: Array<Any?>,
    capacity: Int,
    element: Any?,
    hash: Int,
    hash2: Int,
    emptySlot: IntArray,
): Int = error("JS-only intrinsic")

internal actual fun _scatterSetRemove(
    metadataFlat: LongArray,
    elements: Array<Any?>,
    capacity: Int,
    element: Any?,
    hash: Int,
    hash2: Int,
): Int = error("JS-only intrinsic")

internal actual fun _scatterMapFindSlot(
    metadataFlat: LongArray,
    keys: Array<Any?>,
    capacity: Int,
    key: Any?,
    hash: Int,
    hash2: Int,
    emptySlot: IntArray,
): Int = error("JS-only intrinsic")

internal actual fun _scatterMapFind(
    metadataFlat: LongArray,
    keys: Array<Any?>,
    capacity: Int,
    key: Any?,
    hash: Int,
    hash2: Int,
): Int = error("JS-only intrinsic")

internal actual fun _scatterMapRemove(
    metadataFlat: LongArray,
    keys: Array<Any?>,
    values: Array<Any?>,
    capacity: Int,
    key: Any?,
    hash: Int,
    hash2: Int,
): Int = error("JS-only intrinsic")

internal actual fun _intsetFind(
    metadataFlat: LongArray,
    elements: IntArray,
    capacity: Int,
    element: Int,
    hash: Int,
    hash2: Int,
): Int = error("JS-only intrinsic")

internal actual fun _intsetAdd(
    metadataFlat: LongArray,
    elements: IntArray,
    capacity: Int,
    element: Int,
    hash: Int,
    hash2: Int,
    outCreated: IntArray,
    outSizeDelta: IntArray,
): Int = error("JS-only intrinsic")

internal actual fun _intsetRemove(
    metadataFlat: LongArray,
    elements: IntArray,
    capacity: Int,
    element: Int,
    hash: Int,
    hash2: Int,
): Int = error("JS-only intrinsic")

internal actual fun _intObjectMapFind(
    metadataFlat: LongArray,
    keys: IntArray,
    capacity: Int,
    key: Int,
    hash: Int,
    hash2: Int,
): Int = error("JS-only intrinsic")

internal actual fun _intObjectMapPut(
    metadataFlat: LongArray,
    keys: IntArray,
    capacity: Int,
    key: Int,
    hash: Int,
    hash2: Int,
    outCreated: IntArray,
    outSizeDelta: IntArray,
): Int = error("JS-only intrinsic")

internal actual fun _intObjectMapRemove(
    metadataFlat: LongArray,
    keys: IntArray,
    capacity: Int,
    key: Int,
    hash: Int,
    hash2: Int,
): Int = error("JS-only intrinsic")

internal actual fun _intObjectMapFindAvailableSlot(
    metadataFlat: LongArray,
    capacity: Int,
    hash1: Int,
): Int = error("JS-only intrinsic")
