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

package androidx.collection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for ScatterMap with objects that have no equals() method.
 * This verifies that reference equality is used for such objects.
 */
class CliveTest { // Clive = Class without equals

    // Plain class WITHOUT equals() method - uses reference equality
    class Box<T>(val value: T)

    @Test
    fun scatterMapWithObjectWithoutEquals() {
        val map = MutableScatterMap<Box<Int>, String>()

        val key1 = Box(1)
        val key2 = Box(2)
        val key3 = Box(1) // Same value as key1, but different reference

        map[key1] = "one"
        assertEquals(1, map.size)
        assertEquals("one", map[key1])

        // key3 is a different object with same value - should NOT be found
        // because reference equality is used when there's no equals()
        assertNull(map[key3], "Object without equals should use reference equality")

        // key2 is a different reference with different value
        assertNull(map[key2])

        // Add key3 explicitly
        map[key3] = "one again"
        assertEquals(2, map.size, "key3 is different from key1, so should be added")

        // Original key1 should still work
        assertEquals("one", map[key1])

        // key3 should now be findable
        assertEquals("one again", map[key3])
    }

    @Test
    fun scatterSetWithObjectWithoutEquals() {
        val set = MutableScatterSet<Box<Int>>()

        val key1 = Box(1)
        val key2 = Box(2)
        val key3 = Box(1) // Same value as key1, but different reference

        set.add(key1)
        assertEquals(1, set.size)
        assertTrue(key1 in set)

        // key3 is a different object with same value - should NOT be found
        assertFalse(key3 in set, "Object without equals should use reference equality")

        // key2 is a different reference
        assertFalse(key2 in set)

        // Add key3 explicitly
        set.add(key3)
        assertEquals(2, set.size, "key3 is different from key1, so should be added")

        // Original key1 should still work
        assertTrue(key1 in set)
        assertTrue(key3 in set)
    }

    @Test
    fun intSetBasic() {
        val set = MutableIntSet()
        set.add(1)
        set.add(2)
        set.add(3)

        assertEquals(3, set.size)
        assertTrue(set.contains(1))
        assertTrue(set.contains(2))
        assertTrue(set.contains(3))
        assertFalse(set.contains(4))

        set.remove(2)
        assertEquals(2, set.size)
        assertFalse(set.contains(2))
        assertTrue(set.contains(1))
        assertTrue(set.contains(3))
    }

    @Test
    fun intObjectMapBasic() {
        val map = MutableIntObjectMap<String>()

        map.put(1, "one")
        map.put(2, "two")
        map.put(3, "three")

        assertEquals(3, map.size)
        assertEquals("one", map[1])
        assertEquals("two", map[2])
        assertEquals("three", map[3])
        assertNull(map[4])

        map.remove(2)
        assertEquals(2, map.size)
        assertNull(map[2])
        assertEquals("one", map[1])
        assertEquals("three", map[3])
    }
}