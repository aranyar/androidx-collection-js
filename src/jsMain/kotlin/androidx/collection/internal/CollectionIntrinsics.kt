@file:Suppress("FunctionName", "unused")

package androidx.collection.internal

/**
 * C intrinsic implementations registered by zipline. The functions live on the JS
 * global object so they can be called without additional JS interop setup.
 */

@JsName("_scatterSetFind")
internal actual external fun _scatterSetFind(
    metadataFlat: IntArray,
    elements: Array<Any?>,
    capacity: Int,
    element: Any?,
    hash: Int,
    hash2: Int,
): Int

@JsName("_scatterSetFindSlot")
internal actual external fun _scatterSetFindSlot(
    metadataFlat: IntArray,
    elements: Array<Any?>,
    capacity: Int,
    element: Any?,
    hash: Int,
    hash2: Int,
    emptySlot: IntArray,
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

@JsName("_scatterMapFindSlot")
internal actual external fun _scatterMapFindSlot(
    metadataFlat: IntArray,
    keys: Array<Any?>,
    capacity: Int,
    key: Any?,
    hash: Int,
    hash2: Int,
    emptySlot: IntArray,
): Int

@JsName("_scatterMapFind")
internal actual external fun _scatterMapFind(
    metadataFlat: IntArray,
    keys: Array<Any?>,
    capacity: Int,
    key: Any?,
    hash: Int,
    hash2: Int,
): Int

@JsName("_scatterMapRemove")
internal actual external fun _scatterMapRemove(
    metadataFlat: IntArray,
    keys: Array<Any?>,
    values: Array<Any?>,
    capacity: Int,
    key: Any?,
    hash: Int,
    hash2: Int,
): Int
