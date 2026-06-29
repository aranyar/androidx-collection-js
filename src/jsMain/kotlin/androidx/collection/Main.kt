package androidx.collection

fun main() {
    println("Testing CircularArray...")

    val array = CircularArray<String>()
    array.addFirst("x")
    array.addLast("y")
    array.addLast("z")

    check(array.size() == 3) { "size should be 3" }
    check(array.first == "x") { "first should be x" }
    check(array.last == "z") { "last should be z" }

    val popped = array.popFirst()
    check(popped == "x") { "popFirst should return x" }
    check(array[0] == "y") { "array[0] should be y" }

    println("CircularArray: OK")

    println("Testing ArraySet...")

    val set = ArraySet<Int>()
    set.add(1)
    set.add(2)
    set.add(3)

    check(set.size == 3) { "size should be 3" }
    check(set.contains(2)) { "should contain 2" }

    set.remove(2)
    check(set.size == 2) { "size should be 2 after remove" }

    println("ArraySet: OK")

    println("Testing LongSparseArray...")

    val sparse = LongSparseArray<String>()
    sparse.put(100L, "hundred")
    sparse.put(200L, "two hundred")

    check(sparse[100L] == "hundred") { "sparse[100L] should be hundred" }
    check(sparse.size() == 2) { "size should be 2" }

    sparse.remove(100L)
    check(sparse.size() == 1) { "size should be 1 after remove" }

    println("LongSparseArray: OK")

    println("Testing SparseArrayCompat...")

    val sparseCompat = SparseArrayCompat<String>()
    sparseCompat.put(10, "ten")
    sparseCompat.put(20, "twenty")

    check(sparseCompat[10] == "ten") { "sparseCompat[10] should be ten" }
    check(sparseCompat.size() == 2) { "size should be 2" }

    sparseCompat.remove(10)
    check(sparseCompat.size() == 1) { "size should be 1 after remove" }

    println("SparseArrayCompat: OK")

    println("All tests passed!")
}

private fun check(condition: Boolean, msg: String) {
    if (!condition) throw RuntimeException(msg)
}
