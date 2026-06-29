package androidx.collection.manual

import androidx.collection.MutableScatterSet
import androidx.collection.mutableScatterSetOf
import androidx.collection.emptyScatterSet
import androidx.collection.scatterSetOf

class ScatterSetTest : Tests {
    override fun tests(): List<Pair<String, () -> Unit>> = listOf(
        "emptyScatterSetConstructor" to { emptyScatterSetConstructor() },
        "zeroCapacityScatterSet" to { zeroCapacityScatterSet() },
        "mutableScatterSetBuilder" to { mutableScatterSetBuilder() },
        "addToScatterSet" to { addToScatterSet() },
        "addExistingElement" to { addExistingElement() },
        "remove" to { remove() },
        "clear" to { clear() },
        "contains" to { contains() },
        "size" to { size() }
    )

    private fun emptyScatterSetConstructor() {
        val set = MutableScatterSet<String>()
        checkEquals(7, set.capacity)
        checkEquals(0, set.size)
    }

    private fun zeroCapacityScatterSet() {
        val set = MutableScatterSet<String>(0)
        checkEquals(0, set.capacity)
        checkEquals(0, set.size)
    }

    private fun mutableScatterSetBuilder() {
        val empty = mutableScatterSetOf<String>()
        checkEquals(0, empty.size)
        val withElements = mutableScatterSetOf("Hello", "World")
        checkEquals(2, withElements.size)
        checkCondition("Hello" in withElements)
        checkCondition("World" in withElements)
    }

    private fun addToScatterSet() {
        val set = MutableScatterSet<String>()
        set += "Hello"
        checkCondition(set.add("World"))
        checkEquals(2, set.size)
        val elements = Array(2) { "" }
        var index = 0
        set.forEach { element -> elements[index++] = element }
        elements.sort()
        checkEquals("Hello", elements[0])
        checkEquals("World", elements[1])
    }

    private fun addExistingElement() {
        val set = MutableScatterSet<String>(12)
        set += "Hello"
        checkCondition(!set.add("Hello"))
        set += "Hello"
        checkEquals(1, set.size)
    }

    private fun remove() {
        val set = mutableScatterSetOf("Hello", "World")
        checkCondition(set.remove("Hello"))
        checkEquals(1, set.size)
        checkCondition(!set.contains("Hello"))
    }

    private fun clear() {
        val set = mutableScatterSetOf("Hello", "World")
        set.clear()
        checkEquals(0, set.size)
    }

    private fun contains() {
        val set = mutableScatterSetOf("Hello", "World")
        checkCondition(set.contains("Hello"))
        checkCondition(!set.contains("Missing"))
    }

    private fun size() {
        val set = MutableScatterSet<String>()
        checkEquals(0, set.size)
        set.add("Hello")
        checkEquals(1, set.size)
        set.add("World")
        checkEquals(2, set.size)
        set.remove("Hello")
        checkEquals(1, set.size)
    }
}
