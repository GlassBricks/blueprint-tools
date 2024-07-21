package glassbricks.factorio.blueprint.placement

import org.junit.jupiter.api.Assertions.*
import kotlin.test.Test

class WeightedUnGraphTest {
    @Test
    fun size() {
        val graph = WeightedUnGraph(listOf("a", "b", "c"))
        assertEquals(3, graph.size)
    }

    @Test
    fun indexOf() {
        val graph = WeightedUnGraph(listOf("a", "b", "c"))
        assertEquals(0, graph.indexOf("a"))
        assertEquals(1, graph.indexOf("b"))
        assertEquals(2, graph.indexOf("c"))
        assertEquals(-1, graph.indexOf("d"))
    }

    @Test
    fun `adding an edge and getters`() {
        val graph = WeightedUnGraph(listOf("a", "b", "c"))
        assertTrue(graph.addEdge("a", "b", 1.0))
        assertTrue(graph.addEdge("b", "c", 2.0))

        assertTrue(graph.hasEdge("a", "b"))
        assertTrue(graph.hasEdge("b", "a"))

        assertEquals(listOf(WeightedUnGraph.ToEdge(1, 1.0)), graph.neighborsOf("a"))
        assertEquals(listOf(WeightedUnGraph.ToEdge(0, 1.0), WeightedUnGraph.ToEdge(2, 2.0)), graph.neighborsOf("b"))
        assertEquals(listOf(WeightedUnGraph.ToEdge(1, 2.0)), graph.neighborsOf("c"))
    }
}
