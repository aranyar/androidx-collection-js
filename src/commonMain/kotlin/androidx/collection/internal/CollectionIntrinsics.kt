@file:Suppress("FunctionName", "unused")

package androidx.collection.internal

/**
 * JS-only intrinsics for the androidx.collection hot paths. On JS these are implemented
 * by C functions registered by zipline's IntSetBuiltins.cpp; on other platforms they
 * will fail at runtime (the hot paths in IntObjectMap/IntSet should not be hit there
 * because those classes are mostly used from JS live UI).
 */

/**
 * ScatterSet probe loop intrinsic. Same metadata layout as IntSet but the elements
 * array holds Any? (JS object values) instead of primitives. We compare with JS
 * strict equality (===), which matches Kotlin's `==` on Any?.
 */
internal expect fun _scatterSetFind(
    metadataFlat: IntArray,
    elements: Array<Any?>,
    capacity: Int,
    element: Any?,
    hash: Int,
    hash2: Int,
): Int

internal expect fun _scatterSetFindSlot(
    metadataFlat: IntArray,
    elements: Array<Any?>,
    capacity: Int,
    element: Any?,
    hash: Int,
    hash2: Int,
    emptySlot: IntArray,
): Int

internal expect fun _scatterSetRemove(
    metadataFlat: IntArray,
    elements: Array<Any?>,
    capacity: Int,
    element: Any?,
    hash: Int,
    hash2: Int,
): Int

internal expect fun _scatterMapFindSlot(
    metadataFlat: IntArray,
    keys: Array<Any?>,
    capacity: Int,
    key: Any?,
    hash: Int,
    hash2: Int,
    emptySlot: IntArray,
): Int

internal expect fun _scatterMapFind(
    metadataFlat: IntArray,
    keys: Array<Any?>,
    capacity: Int,
    key: Any?,
    hash: Int,
    hash2: Int,
): Int

internal expect fun _scatterMapRemove(
    metadataFlat: IntArray,
    keys: Array<Any?>,
    values: Array<Any?>,
    capacity: Int,
    key: Any?,
    hash: Int,
    hash2: Int,
): Int

internal expect fun _intsetFind(
    metadataFlat: IntArray,
    elements: IntArray,
    capacity: Int,
    element: Int,
    hash: Int,
    hash2: Int,
): Int

internal expect fun _intsetFindSlot(
    metadataFlat: IntArray,
    elements: IntArray,
    capacity: Int,
    element: Int,
    hash: Int,
    hash2: Int,
    emptySlot: IntArray,
): Int

internal expect fun _intsetRemove(
    metadataFlat: IntArray,
    elements: IntArray,
    capacity: Int,
    element: Int,
    hash: Int,
    hash2: Int,
): Int

internal expect fun _intObjectMapFind(
    metadataFlat: IntArray,
    keys: IntArray,
    capacity: Int,
    key: Int,
    hash: Int,
    hash2: Int,
): Int

internal expect fun _intObjectMapPut(
    metadataFlat: IntArray,
    keys: IntArray,
    capacity: Int,
    key: Int,
    hash: Int,
    hash2: Int,
    outCreated: IntArray,
    outSizeDelta: IntArray,
): Int

internal expect fun _intObjectMapRemove(
    metadataFlat: IntArray,
    keys: IntArray,
    capacity: Int,
    key: Int,
    hash: Int,
    hash2: Int,
): Int

internal expect fun _intObjectMapFindAvailableSlot(
    metadataFlat: IntArray,
    capacity: Int,
    hash1: Int,
): Int
