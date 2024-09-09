package glassbricks.factorio.blueprint.placement

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals


class SlidingWindowMinTest {
    @Test
    fun testRandom() {
        val random = Random("SlidingWindowMinTest".hashCode())
        val size = 200
        val winSize = 5
        val ints = IntArray(size) { random.nextInt() }.withIndex().toList()
        val window = SlidingWindowMin<IndexedValue<Int>>(compareBy { it.value })

        for (i in 0 until winSize) window.add(ints[i])
        for (i in winSize until size) {
            window.removeWhile { it.index < i + 1 - winSize }
            window.add(ints[i])
            val min = ints.subList(i + 1 - winSize, i + 1).minBy { it.value }
            assertEquals(min, window.min())
        }
    }
}
