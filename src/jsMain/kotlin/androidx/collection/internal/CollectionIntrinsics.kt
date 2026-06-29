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

@JsName("_scatterSetAdd")
internal actual external fun _scatterSetAdd(
    metadata: IntArray,
    elements: Array<Any?>,
    capacity: Int,
    element: Any?,
    hash: Int,
    hash2: Int,
    outCreated: IntArray,
    outSizeDelta: IntArray,
    outWasEmpty: IntArray
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
