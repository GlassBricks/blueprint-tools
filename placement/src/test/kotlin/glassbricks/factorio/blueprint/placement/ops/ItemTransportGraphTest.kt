package glassbricks.factorio.blueprint.placement.ops

import glassbricks.factorio.blueprint.Direction
import glassbricks.factorio.blueprint.entity.*
import glassbricks.factorio.blueprint.json.IOType
import glassbricks.factorio.blueprint.pos
import glassbricks.factorio.blueprint.prototypes.VanillaPrototypes
import glassbricks.factorio.blueprint.tilePos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ItemTransportGraphTest {
    private fun belt(
        x: Int,
        y: Int,
        direction: Direction = Direction.North
    ): TransportBelt = VanillaPrototypes.createBpEntity("transport-belt", tilePos(x, y).center(), direction)

    private fun inputUG(
        x: Int,
        y: Int,
        direction: Direction = Direction.North
    ): UndergroundBelt =
        VanillaPrototypes.createBpEntity<UndergroundBelt>("underground-belt", tilePos(x, y).center(), direction)
            .also { it.ioType = IOType.Input }

    private fun outputUg(
        x: Int,
        y: Int,
        direction: Direction = Direction.North
    ): UndergroundBelt =
        VanillaPrototypes.createBpEntity<UndergroundBelt>("underground-belt", tilePos(x, y).center(), direction)
            .also { it.ioType = IOType.Output }

    private fun upwardsSplitter(x: Int, y: Int): Splitter =
        VanillaPrototypes.createBpEntity("splitter", tilePos(x, y).center() + pos(0.5, 0.0), Direction.North)

    private fun testEntities(
        vararg entities: BlueprintEntity
    ): Pair<ItemTransportGraph, List<ItemTransportGraph.Node>> {
        val graph = getItemTransportGraph(entities.asList().let(::DefaultSpatialDataStructure))
        return graph to entities.map { graph.entityToNode[it]!! }
    }


    private fun testEntitiesSimple(vararg entities: BlueprintEntity): List<ItemTransportGraph.Node> =
        testEntities(*entities).second

    @Test
    fun `transport belt connects to another belt`() {
        val (node1, node2) = testEntitiesSimple(
            belt(0, 0),
            belt(0, -1)
        )
        assertEquals(node1.edgeTo(node2)?.type, LogisticsEdgeType.Belt)
        assertEquals(node2.edgeFrom(node1)?.type, LogisticsEdgeType.Belt)
    }

    @Test
    fun `transport connects sideways belt`() {
        val (node1, node2) = testEntitiesSimple(
            belt(0, 0),
            belt(0, -1, Direction.East)
        )
        assertEquals(node1.edgeTo(node2)?.type, LogisticsEdgeType.Belt)
        assertEquals(node2.edgeFrom(node1)?.type, LogisticsEdgeType.Belt)
    }

    @Test
    fun `sideloaded belt`() {
        val (node1, node2) = testEntitiesSimple(
            belt(0, 0),
            belt(0, -1, Direction.East),
            belt(-1, -1, Direction.East),
        )
        assertEquals(node1.edgeTo(node2)?.type, LogisticsEdgeType.Belt)
        assertEquals(node2.edgeFrom(node1)?.type, LogisticsEdgeType.Belt)
        assertTrue(node2.isSideloaded())
    }

    @Test
    fun `transport belt does not connect to opposite belt`() {
        val (node1, node2) = testEntitiesSimple(
            belt(0, 0),
            belt(0, -1, Direction.South)
        )
        assertTrue(node1.outEdges.isEmpty())
        assertTrue(node2.inEdges.isEmpty())
    }

    @Test
    fun `belt does not connect to output underground belt`() {
        val (node1, node2) = testEntitiesSimple(
            belt(0, 0),
            outputUg(0, -1)
        )
        assertTrue(node1.outEdges.isEmpty())
        assertTrue(node2.inEdges.isEmpty())
    }

    @Test
    fun `underground belt can connect to belt`() {
        val (node1, node2) = testEntitiesSimple(
            outputUg(0, 0),
            belt(0, -1)
        )
        assertEquals(node1.edgeTo(node2)?.type, LogisticsEdgeType.Belt)
        assertEquals(node2.edgeFrom(node1)?.type, LogisticsEdgeType.Belt)
    }

    @Test
    fun `underground belt can connect to pair`() {
        val (node1, node2) = testEntitiesSimple(
            inputUG(0, 0),
            outputUg(0, -3)
        )
        assertEquals(node1.edgeTo(node2)?.type, LogisticsEdgeType.Belt)
        assertEquals(node2.edgeFrom(node1)?.type, LogisticsEdgeType.Belt)
    }

    @Test
    fun `underground belt does not connect if blocked`() {
        val (node1) = testEntitiesSimple(
            inputUG(0, 0),
            inputUG(0, -2),
            outputUg(0, -3)
        )
        assertTrue(node1.outEdges.isEmpty())
    }

    @Test
    fun `ug belt connects in different directions`() {
        val (node1, node2) = testEntitiesSimple(
            inputUG(0, 0, Direction.East),
            outputUg(3, 0, Direction.East)
        )
        assertEquals(node1.edgeTo(node2)?.type, LogisticsEdgeType.Belt)
        assertEquals(node2.edgeFrom(node1)?.type, LogisticsEdgeType.Belt)
    }

    @Test
    fun `splitter connects to belts`() {
        val (splitter, belt1, belt2) = testEntitiesSimple(
            upwardsSplitter(0, 0),
            belt(0, -1),
            belt(1, -1)
        )
        assertEquals(splitter.edgeTo(belt1)?.type, LogisticsEdgeType.Belt)
        assertEquals(splitter.edgeTo(belt2)?.type, LogisticsEdgeType.Belt)
        assertEquals(belt1.edgeFrom(splitter)?.type, LogisticsEdgeType.Belt)
        assertEquals(belt2.edgeFrom(splitter)?.type, LogisticsEdgeType.Belt)
    }

    @Test
    fun `belts connect to splitter`() {
        val (belt1, belt2, splitter) = testEntitiesSimple(
            belt(0, 0),
            belt(1, 0),
            upwardsSplitter(0, -1)
        )
        assertEquals(belt1.edgeTo(splitter)?.type, LogisticsEdgeType.Belt)
        assertEquals(belt2.edgeTo(splitter)?.type, LogisticsEdgeType.Belt)
        assertEquals(splitter.edgeFrom(belt1)?.type, LogisticsEdgeType.Belt)
        assertEquals(splitter.edgeFrom(belt2)?.type, LogisticsEdgeType.Belt)
    }

    private fun inserter(
        x: Int,
        y: Int,
        direction: Direction = Direction.North
    ): Inserter = VanillaPrototypes.createBpEntity("inserter", tilePos(x, y).center(), direction)

    @Test
    fun `inserter grabbing from belt`() {
        val (sourceBelt, inserter, destBelt) = testEntitiesSimple(
            belt(0, -1),
            inserter(0, 0),
            belt(0, 1)
        )
        assertEquals(sourceBelt.edgeTo(inserter)?.type, LogisticsEdgeType.Inserter)
        assertEquals(inserter.edgeTo(destBelt)?.type, LogisticsEdgeType.Inserter)

        assertEquals(inserter.edgeFrom(sourceBelt)?.type, LogisticsEdgeType.Inserter)
        assertEquals(destBelt.edgeFrom(inserter)?.type, LogisticsEdgeType.Inserter)
    }


}
