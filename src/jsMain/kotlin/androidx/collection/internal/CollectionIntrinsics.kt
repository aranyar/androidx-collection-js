@file:Suppress("FunctionName", "unused")

package androidx.collection.internal

/**
 * C intrinsic implementations registered by zipline. The functions live on the JS
 * global object so they can be called without additional JS interop setup.
 */
@JsName("_intObjectMapFind")
internal actual external fun _intObjectMapFind(
    metadataFlat: IntArray,
    keys: IntArray,
    capacity: Int,
    key: Int,
    hash: Int,
    hash2: Int,
): Int

@JsName("_intObjectMapPut")
internal actual external fun _intObjectMapPut(
    metadataFlat: IntArray,
    keys: IntArray,
    capacity: Int,
    key: Int,
    hash: Int,
    hash2: Int,
    outCreated: IntArray,
    outSizeDelta: IntArray,
): Int

@JsName("_intObjectMapRemove")
internal actual external fun _intObjectMapRemove(
    metadataFlat: IntArray,
    keys: IntArray,
    capacity: Int,
    key: Int,
    hash: Int,
    hash2: Int,
): Int

@JsName("_intObjectMapFindAvailableSlot")
internal actual external fun _intObjectMapFindAvailableSlot(
    metadataFlat: IntArray,
    capacity: Int,
    hash1: Int,
): Int

@JsName("_intsetFind")
internal actual external fun _intsetFind(
    metadataFlat: IntArray,
    elements: IntArray,
    capacity: Int,
    element: Int,
    hash: Int,
    hash2: Int,
): Int

@JsName("_intsetAdd")
internal actual external fun _intsetAdd(
    metadataFlat: IntArray,
    elements: IntArray,
    capacity: Int,
    element: Int,
    hash: Int,
    hash2: Int,
    outCreated: IntArray,
    outSizeDelta: IntArray,
): Int

@JsName("_intsetRemove")
internal actual external fun _intsetRemove(
    metadataFlat: IntArray,
    elements: IntArray,
    capacity: Int,
    element: Int,
    hash: Int,
    hash2: Int,
): Int

@JsName("_scatterSetFind")
internal actual external fun _scatterSetFind(
    metadataFlat: IntArray,
    elements: Array<Any?>,
    capacity: Int,
    element: Any?,
    hash: Int,
    hash2: Int,
): Int

@JsName("_scatterSetAdd")
internal actual external fun _scatterSetAdd(
    metadataFlat: IntArray,
    elements: Array<Any?>,
    capacity: Int,
    element: Any?,
    hash: Int,
    hash2: Int,
    outCreated: IntArray,
    outSizeDelta: IntArray,
): Int

@JsName("_scatterSetRemove")
internal actual external fun _scatterSetRemove(
    metadataFlat: IntArray,
    elements: Array<Any?>,
    capacity: Int,
    element: Any?,
    hash: Int,
    hash2: Int,
): Int
