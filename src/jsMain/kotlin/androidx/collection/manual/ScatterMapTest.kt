package androidx.collection.manual

import androidx.collection.MutableScatterMap
import androidx.collection.mutableScatterMapOf
import androidx.collection.emptyScatterMap

class ScatterMapTest : Tests {
    override fun tests(): List<Pair<String, () -> Unit>> = listOf(
        "scatterMap" to { scatterMap() },
        "emptyScatterMap" to { emptyScatterMap() },
        "scatterMapFunction" to { scatterMapFunction() },
        "zeroCapacityMap" to { zeroCapacityMap() },
        "scatterMapPairsFunction" to { scatterMapPairsFunction() },
        "addToMap" to { addToMap() },
        "getOrDefault" to { getOrDefault() },
        "put" to { put() },
        "remove" to { remove() },
        "clear" to { clear() },
        "containsKey" to { containsKey() },
        "containsValue" to { containsValue() },
        "size" to { size() }
    )

    private fun scatterMap() {
        val map = MutableScatterMap<String, String>()
        checkEquals(7, map.capacity)
        checkEquals(0, map.size)
    }

    private fun emptyScatterMap() {
        val map = emptyScatterMap<String, String>()
        checkEquals(0, map.capacity)
        checkEquals(0, map.size)
        checkCondition(map === emptyScatterMap<String, String>())
    }

    private fun scatterMapFunction() {
        val map = mutableScatterMapOf<String, String>()
        checkEquals(7, map.capacity)
        checkEquals(0, map.size)
    }

    private fun zeroCapacityMap() {
        val map = MutableScatterMap<String, String>(0)
        checkEquals(0, map.capacity)
        checkEquals(0, map.size)
    }

    private fun scatterMapPairsFunction() {
        val map = mutableScatterMapOf("Hello" to "World", "Bonjour" to "Monde")
        checkEquals(2, map.size)
        checkEquals("World", map["Hello"])
        checkEquals("Monde", map["Bonjour"])
    }

    private fun addToMap() {
        val map = MutableScatterMap<String, String>()
        map["Hello"] = "World"
        checkEquals(1, map.size)
        checkEquals("World", map["Hello"])
    }

    private fun getOrDefault() {
        val map = mutableScatterMapOf("Hello" to "World")
        checkEquals("World", map.getOrDefault("Hello", "Default"))
        checkEquals("Default", map.getOrDefault("Missing", "Default"))
    }

    private fun put() {
        val map = MutableScatterMap<String, String>()
        map.put("Hello", "World")
        checkEquals("World", map["Hello"])
        checkEquals(1, map.size)
        map.put("Hello", "Monde")
        checkEquals("Monde", map["Hello"])
        checkEquals(1, map.size)
    }

    private fun remove() {
        val map = mutableScatterMapOf("Hello" to "World", "Bonjour" to "Monde")
        map.remove("Hello")
        checkEquals(1, map.size)
        checkCondition(!map.containsKey("Hello"))
    }

    private fun clear() {
        val map = mutableScatterMapOf("Hello" to "World", "Bonjour" to "Monde")
        map.clear()
        checkEquals(0, map.size)
    }

    private fun containsKey() {
        val map = mutableScatterMapOf("Hello" to "World")
        checkCondition(map.containsKey("Hello"))
        checkCondition(!map.containsKey("Missing"))
    }

    private fun containsValue() {
        val map = mutableScatterMapOf("Hello" to "World", "Bonjour" to "Monde")
        checkCondition(map.containsValue("World"))
        checkCondition(!map.containsValue("Missing"))
    }

    private fun size() {
        val map = MutableScatterMap<String, String>()
        checkEquals(0, map.size)
        map.put("Hello", "World")
        checkEquals(1, map.size)
        map.put("Bonjour", "Monde")
        checkEquals(2, map.size)
        map.remove("Hello")
        checkEquals(1, map.size)
    }
}
