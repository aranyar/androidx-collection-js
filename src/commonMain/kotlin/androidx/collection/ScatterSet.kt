/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress(
    "RedundantVisibilityModifier",
    "KotlinRedundantDiagnosticSuppress",
    "KotlinConstantConditions",
    "PropertyName",
    "ConstPropertyName",
    "PrivatePropertyName",
    "NOTHING_TO_INLINE"
)

package androidx.collection

import androidx.annotation.IntRange
import androidx.collection.internal.EMPTY_OBJECTS
import androidx.collection.internal._intObjectMapFindAvailableSlot
import androidx.collection.internal._scatterSetFind
import androidx.collection.internal.requirePrecondition
import androidx.collection.internal.throwNoSuchElementExceptionForInline
import kotlin.contracts.contract
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads

// This is a copy of ScatterMap, but without values

// Default empty set to avoid allocations
private val EmptyScatterSet = MutableScatterSet<Any?>(0)

/** Returns an empty, read-only [ScatterSet]. */
@Suppress("UNCHECKED_CAST")
public fun <E> emptyScatterSet(): ScatterSet<E> = EmptyScatterSet as ScatterSet<E>

/** Returns an empty, read-only [ScatterSet]. */
@Suppress("UNCHECKED_CAST")
public fun <E> scatterSetOf(): ScatterSet<E> = EmptyScatterSet as ScatterSet<E>

/** Returns a new read-only [ScatterSet] with only [element1] in it. */
@Suppress("UNCHECKED_CAST")
public fun <E> scatterSetOf(element1: E): ScatterSet<E> = mutableScatterSetOf(element1)

/** Returns a new read-only [ScatterSet] with only [element1] and [element2] in it. */
@Suppress("UNCHECKED_CAST")
public fun <E> scatterSetOf(element1: E, element2: E): ScatterSet<E> =
    mutableScatterSetOf(element1, element2)

/** Returns a new read-only [ScatterSet] with only [element1], [element2], and [element3] in it. */
@Suppress("UNCHECKED_CAST")
public fun <E> scatterSetOf(element1: E, element2: E, element3: E): ScatterSet<E> =
    mutableScatterSetOf(element1, element2, element3)

/** Returns a new read-only [ScatterSet] with only [elements] in it. */
@Suppress("UNCHECKED_CAST")
public fun <E> scatterSetOf(vararg elements: E): ScatterSet<E> =
    MutableScatterSet<E>(elements.size).apply { plusAssign(elements) }

/** Returns a new [MutableScatterSet]. */
public fun <E> mutableScatterSetOf(): MutableScatterSet<E> = MutableScatterSet()

/** Returns a new [MutableScatterSet] with only [element1] in it. */
public fun <E> mutableScatterSetOf(element1: E): MutableScatterSet<E> =
    MutableScatterSet<E>(1).apply { plusAssign(element1) }

/** Returns a new [MutableScatterSet] with only [element1] and [element2] in it. */
public fun <E> mutableScatterSetOf(element1: E, element2: E): MutableScatterSet<E> =
    MutableScatterSet<E>(2).apply {
        plusAssign(element1)
        plusAssign(element2)
    }

/** Returns a new [MutableScatterSet] with only [element1], [element2], and [element3] in it. */
public fun <E> mutableScatterSetOf(element1: E, element2: E, element3: E): MutableScatterSet<E> =
    MutableScatterSet<E>(3).apply {
        plusAssign(element1)
        plusAssign(element2)
        plusAssign(element3)
    }

/** Returns a new [MutableScatterSet] with the specified contents. */
public fun <E> mutableScatterSetOf(vararg elements: E): MutableScatterSet<E> =
    MutableScatterSet<E>(elements.size).apply { plusAssign(elements) }

/**
 * [ScatterSet] is a container with a [Set]-like interface based on a flat hash table
 * implementation. The underlying implementation is designed to avoid all allocations on insertion,
 * removal, retrieval, and iteration. Allocations may still happen on insertion when the underlying
 * storage needs to grow to accommodate newly added elements to the set.
 *
 * This implementation makes no guarantee as to the order of the elements, nor does it make
 * guarantees that the order remains constant over time. If the order of the elements must be
 * preserved, please refer to [OrderedScatterSet].
 *
 * Though [ScatterSet] offers a read-only interface, it is always backed by a [MutableScatterSet].
 * Read operations alone are thread-safe. However, any mutations done through the backing
 * [MutableScatterSet] while reading on another thread are not safe and the developer must protect
 * the set from such changes during read operations.
 *
 * **Note**: when a [Set] is absolutely necessary, you can use the method [asSet] to create a thin
 * wrapper around a [ScatterSet]. Please refer to [asSet] for more details and caveats.
 *
 * @see [MutableScatterSet]
 */
public sealed class ScatterSet<E> {
    // NOTE: Our arrays are marked internal to implement inlined forEach{}
    // The backing array for the metadata bytes contains
    // `capacity + 1 + ClonedMetadataCount` elements, including when
    // the set is empty (see [EmptyGroup]).
    // We use ONLY metadataFlat (IntArray) for all operations - it's the flat byte array
    // that C intrinsics expect.
    @PublishedApi @JvmField internal var metadataFlat: IntArray = EmptyIntArray

    @PublishedApi @JvmField internal var elements: Array<Any?> = EMPTY_OBJECTS

    // We use a backing field for capacity to avoid invokevirtual calls
    // every time we need to look at the capacity
    @JvmField internal var _capacity: Int = 0

    /**
     * Returns the number of elements that can be stored in this set without requiring internal
     * storage reallocation.
     */
    @get:IntRange(from = 0)
    public val capacity: Int
        get() = _capacity

    // We use a backing field for capacity to avoid invokevirtual calls
    // every time we need to look at the size
    @JvmField internal var _size: Int = 0

    /** Returns the number of elements in this set. */
    @get:IntRange(from = 0)
    public val size: Int
        get() = _size

    /** Returns `true` if this set has at least one element. */
    public fun any(): Boolean = _size != 0

    /** Returns `true` if this set has no elements. */
    public fun none(): Boolean = _size == 0

    /** Indicates whether this set is empty. */
    public fun isEmpty(): Boolean = _size == 0

    /** Returns `true` if this set is not empty. */
    public fun isNotEmpty(): Boolean = _size != 0

    /**
     * Returns the first element in the collection.
     *
     * @throws NoSuchElementException if the collection is empty
     */
    public fun first(): E {
        forEach {
            return it
        }
        throwNoSuchElementExceptionForInline("The ScatterSet is empty")
    }

    /**
     * Returns the first element in the collection for which [predicate] returns `true`
     *
     * @param predicate called with each element until it returns `true`.
     * @return The element for which [predicate] returns `true`.
     * @throws NoSuchElementException if [predicate] returns `false` for all elements or the
     *   collection is empty.
     */
    public inline fun first(predicate: (element: E) -> Boolean): E {
        contract { callsInPlace(predicate) }
        forEach { if (predicate(it)) return it }
        throwNoSuchElementExceptionForInline("Could not find a match")
    }

    /**
     * Returns the first element in the collection for which [predicate] returns `true` or `null` if
     * there are no elements that match [predicate].
     *
     * @param predicate called with each element until it returns `true`.
     * @return The element for which [predicate] returns `true` or `null` if there are no elements
     *   in the set or [predicate] returned `false` for every element in the set.
     */
    public inline fun firstOrNull(predicate: (element: E) -> Boolean): E? {
        contract { callsInPlace(predicate) }
        forEach { if (predicate(it)) return it }
        return null
    }

    /** Iterates over every element stored in this set by invoking the specified [block] lambda. */
    @PublishedApi
    internal inline fun forEachIndex(block: (index: Int) -> Unit) {
        contract { callsInPlace(block) }
        val flat = metadataFlat
        val groupsCount = (metadataFlat.size + 7) shr 3
        val lastGroupIndex = groupsCount - 2

        for (i in 0..lastGroupIndex) {
            var g = groupFromFlat(flat, i shl 3)
            if (g.maskEmptyOrDeleted() != BitmaskMsb) {
                val bitCount = 8 - ((i - lastGroupIndex).inv() ushr 31)
                for (j in 0 until bitCount) {
                    if (isFull(g and 0xFFL)) {
                        block((i shl 3) + j)
                    }
                    g = g shr 8
                }
                if (bitCount != 8) return
            }
        }
    }

    /**
     * Iterates over every element stored in this set by invoking the specified [block] lambda. It
     * is safe to remove the element passed to [block] during iteration.
     *
     * @param block called with each element in the set
     */
    public inline fun forEach(block: (element: E) -> Unit) {
        contract { callsInPlace(block) }
        val elements = elements
        forEachIndex { index -> @Suppress("UNCHECKED_CAST") block(elements[index] as E) }
    }

    /**
     * Returns true if all elements match the given [predicate]. If there are no elements in the
     * set, `true` is returned.
     *
     * @param predicate called for elements in the set to determine if it returns return `true` for
     *   all elements.
     */
    public inline fun all(predicate: (element: E) -> Boolean): Boolean {
        contract { callsInPlace(predicate) }
        forEach { element -> if (!predicate(element)) return false }
        return true
    }

    /**
     * Returns true if at least one element matches the given [predicate].
     *
     * @param predicate called for elements in the set to determine if it returns `true` for any
     *   elements.
     */
    public inline fun any(predicate: (element: E) -> Boolean): Boolean {
        contract { callsInPlace(predicate) }
        forEach { element -> if (predicate(element)) return true }
        return false
    }

    /** Returns the number of elements in this set. */
    @IntRange(from = 0) public fun count(): Int = size

    /**
     * Returns the number of elements matching the given [predicate].
     *
     * @param predicate Called for all elements in the set to count the number for which it returns
     *   `true`.
     */
    @IntRange(from = 0)
    public inline fun count(predicate: (element: E) -> Boolean): Int {
        contract { callsInPlace(predicate) }
        var count = 0
        forEach { element -> if (predicate(element)) count++ }
        return count
    }

    /**
     * Returns true if the specified [element] is present in this hash set, false otherwise.
     *
     * @param element The element to look for in this set
     */
    public operator fun contains(element: E): Boolean = findElementIndex(element) >= 0

    /**
     * Creates a String from the elements separated by [separator] and using [prefix] before and
     * [postfix] after, if supplied.
     *
     * When a non-negative value of [limit] is provided, a maximum of [limit] items are used to
     * generate the string. If the collection holds more than [limit] items, the string is
     * terminated with [truncated].
     *
     * [transform] may be supplied to convert each element to a custom String.
     */
    @JvmOverloads
    public fun joinToString(
        separator: CharSequence = ", ",
        prefix: CharSequence = "",
        postfix: CharSequence = "", // I know this should be suffix, but this is kotlin's name
        limit: Int = -1,
        truncated: CharSequence = "...",
        transform: ((E) -> CharSequence)? = null
    ): String = buildString {
        append(prefix)
        var index = 0
        this@ScatterSet.forEach { element ->
            if (index == limit) {
                append(truncated)
                return@buildString
            }
            if (index != 0) {
                append(separator)
            }
            if (transform == null) {
                append(element)
            } else {
                append(transform(element))
            }
            index++
        }
        append(postfix)
    }

    /**
     * Returns the hash code value for this set. The hash code of a set is based on the sum of the
     * hash codes of the elements in the set, where the hash code of a null element is defined to be
     * zero.
     */
    public override fun hashCode(): Int {
        var hash = _capacity
        hash = 31 * hash + _size

        forEach { element ->
            if (element != this) {
                hash += element.hashCode()
            }
        }

        return hash
    }

    /**
     * Compares the specified object [other] with this hash set for equality. The two objects are
     * considered equal if [other]:
     * - Is a [ScatterSet]
     * - Has the same [size] as this set
     * - Contains elements equal to this set's elements
     */
    public override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        if (other !is ScatterSet<*>) {
            return false
        }
        if (other.size != size) {
            return false
        }

        @Suppress("UNCHECKED_CAST") val o = other as ScatterSet<Any?>

        forEach { element ->
            if (element !in o) {
                return false
            }
        }

        return true
    }

    /**
     * Returns a string representation of this set. The set is denoted in the string by the `[]`.
     * Each element is separated by `, `.
     */
    override fun toString(): String =
        joinToString(prefix = "[", postfix = "]") { element ->
            if (element === this) {
                "(this)"
            } else {
                element.toString()
            }
        }

    /**
     * Scans the set to find the index in the backing arrays of the specified [element]. Returns -1
     * if the element is not present.
     */
    internal inline fun findElementIndex(element: E): Int {
        val hash = hash(element)
        val hash1 = h1(hash)
        val hash2 = h2(hash)

        val probeMask = _capacity
        var probeOffset = hash1 and probeMask
        var probeIndex = 0

        while (true) {
            val g = groupFromFlat(metadataFlat, probeOffset)
            var m = g.match(hash2)
            while (m.hasNext()) {
                val index = (probeOffset + m.get()) and probeMask
                if (elements[index] == element) {
                    return index
                }
                m = m.next()
            }
            if (g.maskEmpty() != 0L) return -1
            probeIndex += GroupWidth
            probeOffset = (probeOffset + probeIndex) and probeMask
        }
    }

    /**
     * Wraps this [ScatterSet] with a [Set] interface. The [Set] is backed by the [ScatterSet], so
     * changes to the [ScatterSet] are reflected in the [Set]. If the [ScatterSet] is modified while
     * an iteration over the [Set] is in progress, the results of the iteration are undefined.
     *
     * **Note**: while this method is useful to use this [ScatterSet] with APIs accepting [Set]
     * interfaces, it is less efficient to do so than to use [ScatterSet]'s APIs directly. While the
     * [Set] implementation returned by this method tries to be as efficient as possible, the
     * semantics of [Set] may require the allocation of temporary objects for access and iteration.
     */
    public fun asSet(): Set<E> = SetWrapper(this)
}

/**
 * [MutableScatterSet] is a container with a [MutableSet]-like interface based on a flat hash table
 * implementation. The underlying implementation is designed to avoid all allocations on insertion,
 * removal, retrieval, and iteration. Allocations may still happen on insertion when the underlying
 * storage needs to grow to accommodate newly added elements to the set.
 *
 * This implementation makes no guarantee as to the order of the elements stored, nor does it make
 * guarantees that the order remains constant over time.
 *
 * This implementation is not thread-safe: if multiple threads access this container concurrently,
 * and one or more threads modify the structure of the set (insertion or removal for instance), the
 * calling code must provide the appropriate synchronization. Concurrent reads are however safe.
 *
 * **Note**: when a [Set] is absolutely necessary, you can use the method [asSet] to create a thin
 * wrapper around a [MutableScatterSet]. Please refer to [asSet] for more details and caveats.
 *
 * **Note**: when a [MutableSet] is absolutely necessary, you can use the method [asMutableSet] to
 * create a thin wrapper around a [MutableScatterSet]. Please refer to [asMutableSet] for more
 * details and caveats.
 *
 * @param initialCapacity The initial desired capacity for this container. the container will honor
 *   this value by guaranteeing its internal structures can hold that many elements without
 *   requiring any allocations. The initial capacity can be set to 0.
 * @constructor Creates a new [MutableScatterSet]
 * @see Set
 */
public class MutableScatterSet<E>(initialCapacity: Int = DefaultScatterCapacity) : ScatterSet<E>() {
    // Number of elements we can add before we need to grow
    private var growthLimit = 0

    init {
        requirePrecondition(initialCapacity >= 0) { "Capacity must be a positive value." }
        initializeStorage(unloadedCapacity(initialCapacity))
    }

    private fun initializeStorage(initialCapacity: Int) {
        val newCapacity =
            if (initialCapacity > 0) {
                // Since we use longs for storage, our capacity is never < 7, enforce
                // it here. We do have a special case for 0 to create small empty maps
                maxOf(7, normalizeCapacity(initialCapacity))
            } else {
                0
            }
        _capacity = newCapacity
        initializeMetadata(newCapacity)
        elements = if (newCapacity == 0) EMPTY_OBJECTS else arrayOfNulls(newCapacity)
    }

    private fun initializeMetadata(capacity: Int) {
        // metadataFlat is the only metadata array - flat byte view for C intrinsics
        metadataFlat =
            if (capacity == 0) {
                EmptyIntArray
            } else {
                val byteCount = (capacity + 1 + ClonedMetadataCount + 7) and 0x7.inv()
                IntArray(byteCount).apply { fill(0x80) }  // All Empty (0x80)
            }
        // Write sentinel byte at position capacity
        writeMetaByte(metadataFlat, capacity, 0xFF)
        initializeGrowth()
    }

    private fun initializeGrowth() {
        growthLimit = loadedCapacity(capacity) - _size
    }

    /**
     * Adds the specified element to the set.
     *
     * @param element The element to add to the set.
     * @return `true` if the element has been added or `false` if the element is already contained
     *   within the set.
     */
    public fun add(element: E): Boolean {
        val oldSize = size
        val index = findAbsoluteInsertIndex(element)
        elements[index] = element
        return size != oldSize
    }

    /**
     * Adds the specified element to the set.
     *
     * @param element The element to add to the set.
     */
    public operator fun plusAssign(element: E) {
        val index = findAbsoluteInsertIndex(element)
        elements[index] = element
    }

    /**
     * Adds all the [elements] into this set.
     *
     * @param elements An array of elements to add to the set.
     * @return `true` if any of the specified elements were added to the collection, `false` if the
     *   collection was not modified.
     */
    public fun addAll(@Suppress("ArrayReturn") elements: Array<out E>): Boolean {
        val oldSize = size
        plusAssign(elements)
        return oldSize != size
    }

    /**
     * Adds all the [elements] into this set.
     *
     * @param elements Iterable elements to add to the set.
     * @return `true` if any of the specified elements were added to the collection, `false` if the
     *   collection was not modified.
     */
    public fun addAll(elements: Iterable<E>): Boolean {
        val oldSize = size
        plusAssign(elements)
        return oldSize != size
    }

    /**
     * Adds all the [elements] into this set.
     *
     * @param elements The sequence of elements to add to the set.
     * @return `true` if any of the specified elements were added to the collection, `false` if the
     *   collection was not modified.
     */
    public fun addAll(elements: Sequence<E>): Boolean {
        val oldSize = size
        plusAssign(elements)
        return oldSize != size
    }

    /**
     * Adds all the elements in the [elements] set into this set.
     *
     * @param elements A [ScatterSet] whose elements are to be added to the set
     * @return `true` if any of the specified elements were added to the collection, `false` if the
     *   collection was not modified.
     */
    public fun addAll(elements: ScatterSet<E>): Boolean {
        val oldSize = size
        plusAssign(elements)
        return oldSize != size
    }

    /**
     * Adds all the elements in the [elements] set into this set.
     *
     * @param elements A [OrderedScatterSet] whose elements are to be added to the set
     * @return `true` if any of the specified elements were added to the collection, `false` if the
     *   collection was not modified.
     */
    public fun addAll(elements: OrderedScatterSet<E>): Boolean {
        val oldSize = size
        plusAssign(elements)
        return oldSize != size
    }

    /**
     * Adds all the elements in the [elements] set into this set.
     *
     * @param elements An [ObjectList] whose elements are to be added to the set
     * @return `true` if any of the specified elements were added to the collection, `false` if the
     *   collection was not modified.
     */
    public fun addAll(elements: ObjectList<E>): Boolean {
        val oldSize = size
        plusAssign(elements)
        return oldSize != size
    }

    /**
     * Adds all the [elements] into this set.
     *
     * @param elements An array of elements to add to the set.
     */
    public operator fun plusAssign(@Suppress("ArrayReturn") elements: Array<out E>) {
        elements.forEach { element -> plusAssign(element) }
    }

    /**
     * Adds all the [elements] into this set.
     *
     * @param elements Iterable elements to add to the set.
     */
    public operator fun plusAssign(elements: Iterable<E>) {
        elements.forEach { element -> plusAssign(element) }
    }

    /**
     * Adds all the [elements] into this set.
     *
     * @param elements The sequence of elements to add to the set.
     */
    public operator fun plusAssign(elements: Sequence<E>) {
        elements.forEach { element -> plusAssign(element) }
    }

    /**
     * Adds all the elements in the [elements] set into this set.
     *
     * @param elements A [ScatterSet] whose elements are to be added to the set
     */
    public operator fun plusAssign(elements: ScatterSet<E>) {
        elements.forEach { element -> plusAssign(element) }
    }

    /**
     * Adds all the elements in the [elements] set into this set.
     *
     * @param elements A [OrderedScatterSet] whose elements are to be added to the set
     */
    public operator fun plusAssign(elements: OrderedScatterSet<E>) {
        elements.forEach { element -> plusAssign(element) }
    }

    /**
     * Adds all the elements in the [elements] set into this set.
     *
     * @param elements An [ObjectList] whose elements are to be added to the set
     */
    public operator fun plusAssign(elements: ObjectList<E>) {
        elements.forEach { element -> plusAssign(element) }
    }

    /**
     * Removes the specified [element] from the set.
     *
     * @param element The element to be removed from the set.
     * @return `true` if the [element] was present in the set, or `false` if it wasn't present
     *   before removal.
     */
    public fun remove(element: E): Boolean {
        val index = findElementIndex(element)
        val exists = index >= 0
        if (exists) {
            removeElementAt(index)
        }
        return exists
    }

    /**
     * Removes the specified [element] from the set if it is present.
     *
     * @param element The element to be removed from the set.
     */
    public operator fun minusAssign(element: E) {
        val index = findElementIndex(element)
        if (index >= 0) {
            removeElementAt(index)
        }
    }

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements An array of elements to be removed from the set.
     * @return `true` if the set was changed or `false` if none of the elements were present.
     */
    public fun removeAll(@Suppress("ArrayReturn") elements: Array<out E>): Boolean {
        val oldSize = size
        minusAssign(elements)
        return oldSize != size
    }

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements A sequence of elements to be removed from the set.
     * @return `true` if the set was changed or `false` if none of the elements were present.
     */
    public fun removeAll(elements: Sequence<E>): Boolean {
        val oldSize = size
        minusAssign(elements)
        return oldSize != size
    }

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements A Iterable of elements to be removed from the set.
     * @return `true` if the set was changed or `false` if none of the elements were present.
     */
    public fun removeAll(elements: Iterable<E>): Boolean {
        val oldSize = size
        minusAssign(elements)
        return oldSize != size
    }

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements A [ScatterSet] whose elements should be removed from the set.
     * @return `true` if the set was changed or `false` if none of the elements were present.
     */
    public fun removeAll(elements: ScatterSet<E>): Boolean {
        val oldSize = size
        minusAssign(elements)
        return oldSize != size
    }

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements A [OrderedScatterSet] whose elements should be removed from the set.
     * @return `true` if the set was changed or `false` if none of the elements were present.
     */
    public fun removeAll(elements: OrderedScatterSet<E>): Boolean {
        val oldSize = size
        minusAssign(elements)
        return oldSize != size
    }

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements An [ObjectList] whose elements should be removed from the set.
     * @return `true` if the set was changed or `false` if none of the elements were present.
     */
    public fun removeAll(elements: ObjectList<E>): Boolean {
        val oldSize = size
        minusAssign(elements)
        return oldSize != size
    }

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements An array of elements to be removed from the set.
     */
    public operator fun minusAssign(@Suppress("ArrayReturn") elements: Array<out E>) {
        elements.forEach { element -> minusAssign(element) }
    }

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements A sequence of elements to be removed from the set.
     */
    public operator fun minusAssign(elements: Sequence<E>) {
        elements.forEach { element -> minusAssign(element) }
    }

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements A Iterable of elements to be removed from the set.
     */
    public operator fun minusAssign(elements: Iterable<E>) {
        elements.forEach { element -> minusAssign(element) }
    }

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements A [ScatterSet] whose elements should be removed from the set.
     */
    public operator fun minusAssign(elements: ScatterSet<E>) {
        elements.forEach { element -> minusAssign(element) }
    }

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements A [OrderedScatterSet] whose elements should be removed from the set.
     */
    public operator fun minusAssign(elements: OrderedScatterSet<E>) {
        elements.forEach { element -> minusAssign(element) }
    }

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements An [ObjectList] whose elements should be removed from the set.
     */
    public operator fun minusAssign(elements: ObjectList<E>) {
        elements.forEach { element -> minusAssign(element) }
    }

    /** Removes any values for which the specified [predicate] returns true. */
    public inline fun removeIf(predicate: (E) -> Boolean) {
        val elements = elements
        forEachIndex { index ->
            @Suppress("UNCHECKED_CAST")
            if (predicate(elements[index] as E)) {
                removeElementAt(index)
            }
        }
    }

    /**
     * Removes all the entries in this set that are not contained in [elements].
     *
     * @param elements A collection of elements to preserve in this set.
     * @return `true` if this set was modified, `false` otherwise.
     */
    public fun retainAll(elements: Collection<E>): Boolean {
        val internalElements = this.elements
        val startSize = _size
        forEachIndex { index ->
            if (internalElements[index] !in elements) {
                removeElementAt(index)
            }
        }
        return startSize != _size
    }

    /**
     * Removes all the entries in this set that are not contained in [elements].
     *
     * @params elements A set of elements to preserve in this set.
     * @return `true` if this set was modified, `false` otherwise.
     */
    public fun retainAll(elements: ScatterSet<E>): Boolean {
        val internalElements = this.elements
        val startSize = _size
        forEachIndex { index ->
            @Suppress("UNCHECKED_CAST")
            if (internalElements[index] as E !in elements) {
                removeElementAt(index)
            }
        }
        return startSize != _size
    }

    /**
     * Removes all the entries in this set that are not contained in [elements].
     *
     * @params elements A set of elements to preserve in this set.
     * @return `true` if this set was modified, `false` otherwise.
     */
    public fun retainAll(elements: OrderedScatterSet<E>): Boolean {
        val internalElements = this.elements
        val startSize = _size
        forEachIndex { index ->
            @Suppress("UNCHECKED_CAST")
            if (internalElements[index] as E !in elements) {
                removeElementAt(index)
            }
        }
        return startSize != _size
    }

    /**
     * Removes all the elements in this set for which the specified [predicate] is `true`. For each
     * element in the set, the predicate is invoked with that element as the sole parameter.
     *
     * @param predicate Predicate invoked for each element in the set. When the predicate returns
     *   `true`, the element is kept in the set, otherwise it is removed.
     * @return `true` if this set was modified, `false` otherwise.
     */
    public fun retainAll(predicate: (E) -> Boolean): Boolean {
        val elements = elements
        val startSize = _size
        forEachIndex { index ->
            @Suppress("UNCHECKED_CAST")
            if (!predicate(elements[index] as E)) {
                removeElementAt(index)
            }
        }
        return startSize != _size
    }

    @PublishedApi
    internal fun removeElementAt(index: Int) {
        _size -= 1
        writeMetaByte(metadataFlat, index, 0xFE)  // Deleted
        elements[index] = null
    }

    /** Removes all elements from this set. */
    public fun clear() {
        _size = 0
        if (metadataFlat.isNotEmpty()) {
            // Fill with 0x80808080 to represent all bytes being 0x80 (Empty)
            for (i in metadataFlat.indices) {
                metadataFlat[i] = -0x7f7f7f80.toInt()
            }
            writeMetaByte(metadataFlat, _capacity, 0xFF)  // Sentinel
        }
        elements.fill(null, 0, _capacity)
        initializeGrowth()
    }

    /**
     * Scans the set to find the index at which we can store the given [element]. If the element
     * already exists in the set, its index will be returned, otherwise the index of an empty slot
     * will be returned. Calling this function may cause the internal storage to be reallocated if
     * the set is full.
     */
    private fun findAbsoluteInsertIndex(element: E): Int {
        val hash = hash(element)
        val hash1 = h1(hash)
        val hash2 = h2(hash)

        val probeMask = _capacity
        var probeOffset = hash1 and probeMask
        var probeIndex = 0

        while (true) {
            val g = groupFromFlat(metadataFlat, probeOffset)
            var m = g.match(hash2)
            while (m.hasNext()) {
                val index = (probeOffset + m.get()) and probeMask
                if (elements[index] == element) {
                    return index
                }
                m = m.next()
            }

            if (g.maskEmpty() != 0L) {
                break
            }

            probeIndex += GroupWidth
            probeOffset = (probeOffset + probeIndex) and probeMask
        }

        // No existing element. Find first available slot (Empty or Deleted).
        var index = findFirstAvailableSlot(hash1)
        if (growthLimit == 0 && !isDeletedFlat(metadataFlat, index)) {
            adjustStorage()
            index = findFirstAvailableSlot(hash1)
        }

        _size += 1
        growthLimit -= if (isEmptyFlat(metadataFlat, index)) 1 else 0
        writeMetaByte(metadataFlat, index, hash2)

        return index
    }

    /**
     * Finds the first empty or deleted slot in the set in which we can store an element without
     * resizing the internal storage.
     */
    private fun findFirstAvailableSlot(hash1: Int): Int {
        val probeMask = _capacity
        var probeOffset = hash1 and probeMask
        var probeIndex = 0

        while (true) {
            val g = groupFromFlat(metadataFlat, probeOffset)
            val m = g.maskEmptyOrDeleted()
            if (m != 0L) {
                return (probeOffset + m.lowestBitSet()) and probeMask
            }
            probeIndex += GroupWidth
            probeOffset = (probeOffset + probeIndex) and probeMask
        }
    }

    /**
     * Trims this [MutableScatterSet]'s storage so it is sized appropriately to hold the current
     * elements.
     *
     * Returns the number of empty elements removed from this set's storage. Returns 0 if no
     * trimming is necessary or possible.
     */
    @IntRange(from = 0)
    public fun trim(): Int {
        val previousCapacity = _capacity
        val newCapacity = normalizeCapacity(unloadedCapacity(_size))
        if (newCapacity < previousCapacity) {
            resizeStorage(newCapacity)
            return previousCapacity - _capacity
        }
        return 0
    }

    /**
     * Grow internal storage if necessary. This function can instead opt to remove deleted elements
     * from the set to avoid an expensive reallocation of the underlying storage. This "rehash in
     * place" occurs when the current size is <= 25/32 of the set capacity. The choice of 25/32 is
     * detailed in the implementation of abseil's `raw_hash_map`.
     */
    internal fun adjustStorage() { // Internal to prevent inlining
        if (_capacity > GroupWidth && _size.toULong() * 32UL <= _capacity.toULong() * 25UL) {
            dropDeletes()
        } else {
            resizeStorage(nextCapacity(_capacity))
        }
    }

    // Internal to prevent inlining
    internal fun dropDeletes() {
        val flat = metadataFlat
        val capacity = _capacity
        val elements = elements

        // Cleanup phase 1: convert Full->Deleted, Empty/Deleted->Empty
        // Don't touch sentinel at position capacity (0xFF)
        for (i in 0 until capacity) {
            val byte = readMetaByte(flat, i)
            if (byte < 0x80) {
                // Full -> Deleted
                writeMetaByte(flat, i, 0xFE)
            } else {
                // Empty/Deleted -> Empty
                writeMetaByte(flat, i, 0x80)
            }
        }

        var index = 0

        // Drop deleted items and re-hashes surviving entries
        while (index != capacity) {
            val m = readMetaByte(flat, index)
            if (m == 0x80) {
                // Empty - skip
                index++
                continue
            }

            if (m != 0xFE) {
                // Full (hash2) - skip
                index++
                continue
            }

            // m == Deleted - need to rehash this element
            val hash = hash(elements[index])
            val hash1 = h1(hash)
            val targetIndex = findFirstAvailableSlot(hash1)

            val probeOffset = hash1 and capacity
            val newProbeIndex = ((targetIndex - probeOffset) and capacity) / GroupWidth
            val oldProbeIndex = ((index - probeOffset) and capacity) / GroupWidth

            if (newProbeIndex == oldProbeIndex) {
                val hash2 = h2(hash)
                writeMetaByte(flat, index, hash2)
                index++
                continue
            }

            val targetByte = readMetaByte(flat, targetIndex)
            if (targetByte == 0x80) {
                val hash2 = h2(hash)
                writeMetaByte(flat, targetIndex, hash2)
                writeMetaByte(flat, index, 0x80)
                elements[targetIndex] = elements[index]
                elements[index] = null
            } else {
                val hash2 = h2(hash)
                writeMetaByte(flat, targetIndex, hash2)
                val oldElement = elements[targetIndex]
                elements[targetIndex] = elements[index]
                elements[index] = oldElement
                index--
            }

            index++
        }

        initializeGrowth()
    }

    // Internal to prevent inlining
    internal fun resizeStorage(newCapacity: Int) {
        val previousFlat = metadataFlat
        val previousElements = elements
        val previousCapacity = _capacity

        initializeStorage(newCapacity)

        val newFlat = metadataFlat
        val newElements = elements
        val capacity = _capacity

        for (i in 0 until previousCapacity) {
            if (isFullFlat(previousFlat, i)) {
                val previousElement = previousElements[i]
                val hash = hash(previousElement)
                val index = findFirstAvailableSlot(h1(hash))
                writeMetaByte(newFlat, index, h2(hash))
                newElements[index] = previousElement
            }
        }
    }

    /**
     * Wraps this [ScatterSet] with a [MutableSet] interface. The [MutableSet] is backed by the
     * [ScatterSet], so changes to the [ScatterSet] are reflected in the [MutableSet] and
     * vice-versa. If the [ScatterSet] is modified while an iteration over the [MutableSet] is in
     * progress (and vice- versa), the results of the iteration are undefined.
     *
     * **Note**: while this method is useful to use this [MutableScatterSet] with APIs accepting
     * [MutableSet] interfaces, it is less efficient to do so than to use [MutableScatterSet]'s APIs
     * directly. While the [MutableSet] implementation returned by this method tries to be as
     * efficient as possible, the semantics of [MutableSet] may require the allocation of temporary
     * objects for access and iteration.
     */
    public fun asMutableSet(): MutableSet<E> = MutableSetWrapper(this)
}

private open class SetWrapper<E>(private val parent: ScatterSet<E>) : Set<E> {
    override val size: Int
        get() = parent._size

    override fun containsAll(elements: Collection<E>): Boolean {
        elements.forEach { element ->
            if (!parent.contains(element)) {
                return false
            }
        }
        return true
    }

    @Suppress("KotlinOperator")
    override fun contains(element: E): Boolean {
        return parent.contains(element)
    }

    override fun isEmpty(): Boolean = parent.isEmpty()

    override fun iterator(): Iterator<E> {
        return iterator { parent.forEach { element -> yield(element) } }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SetWrapper<*>

        return parent == other.parent
    }

    override fun hashCode(): Int {
        return parent.hashCode()
    }

    override fun toString(): String = parent.toString()
}

private class MutableSetWrapper<E>(private val parent: MutableScatterSet<E>) :
    SetWrapper<E>(parent), MutableSet<E> {
    override fun add(element: E): Boolean = parent.add(element)

    override fun addAll(elements: Collection<E>): Boolean = parent.addAll(elements)

    override fun clear() {
        parent.clear()
    }

    override fun iterator(): MutableIterator<E> =
        object : MutableIterator<E> {
            var current = -1
            val iterator = iterator {
                parent.forEachIndex { index ->
                    current = index
                    @Suppress("UNCHECKED_CAST") yield(parent.elements[index] as E)
                }
            }

            override fun hasNext(): Boolean = iterator.hasNext()

            override fun next(): E = iterator.next()

            override fun remove() {
                if (current != -1) {
                    parent.removeElementAt(current)
                    current = -1
                }
            }
        }

    override fun remove(element: E): Boolean = parent.remove(element)

    override fun retainAll(elements: Collection<E>): Boolean = parent.retainAll(elements)

    override fun removeAll(elements: Collection<E>): Boolean = parent.removeAll(elements)
}

// ============================================================================
// Helpers to read/write metadata directly from metadataFlat (IntArray)
// These treat metadataFlat as a flat byte array for C intrinsic compatibility
// ============================================================================

@PublishedApi internal inline fun readMetaByte(flat: IntArray, offset: Int): Int =
    (flat[offset shr 2] ushr ((offset and 3) * 8)) and 0xFF

@PublishedApi internal inline fun writeMetaByte(flat: IntArray, offset: Int, byte: Int) {
    val idx = offset shr 2
    val shift = (offset and 3) * 8
    flat[idx] = (flat[idx] and (0xFF shl shift).inv()) or ((byte and 0xFF) shl shift)
}

@PublishedApi internal inline fun readGroupFromFlat(flat: IntArray, offset: Int): Long {
    var g: Long = 0
    for (i in 0 until 8) {
        g = g or (readMetaByte(flat, offset + i).toLong() shl (i * 8))
    }
    return g
}

@PublishedApi internal inline fun writeGroupToFlat(flat: IntArray, offset: Int, value: Long) {
    for (i in 0 until 8) {
        writeMetaByte(flat, offset + i, ((value ushr (i * 8)) and 0xFF).toInt())
    }
}

@PublishedApi internal inline fun groupFromFlat(flat: IntArray, offset: Int): Long {
    return readGroupFromFlat(flat, offset)
}

@PublishedApi internal inline fun isEmptyFlat(flat: IntArray, index: Int): Boolean =
    readMetaByte(flat, index) == 0x80

@PublishedApi internal inline fun isDeletedFlat(flat: IntArray, index: Int): Boolean =
    readMetaByte(flat, index) == 0xFE

@PublishedApi internal inline fun isFullFlat(flat: IntArray, index: Int): Boolean =
    readMetaByte(flat, index) < 0x80
