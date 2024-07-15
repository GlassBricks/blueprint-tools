package glassbricks.factorio.blueprint.model

import glassbricks.factorio.blueprint.*
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals


class Oracle : MutableSpatialDataStructure<SimpleSpatial>,
    MutableCollection<SimpleSpatial> by HashSet() {
    override fun getInArea(area: BoundingBox): Sequence<SimpleSpatial> =
        asSequence().filter { it.collisionBox intersects area }

    override fun getInArea(area: TileBoundingBox): Sequence<SimpleSpatial> =
        getInArea(area.toBoundingBox())

    override fun getInTile(tile: TilePosition): Sequence<SimpleSpatial> =
        getInArea(tile.mapBoundingBox())

    override fun getAtPoint(position: Position): Sequence<SimpleSpatial> =
        asSequence().filter { position in it.collisionBox }

    override fun getPosInCircle(center: Position, radius: Double): Sequence<SimpleSpatial> =
        asSequence().filter { center.squaredDistanceTo(it.position) <= radius * radius }

    override fun getColliding(other: Spatial): Sequence<SimpleSpatial> =
        asSequence().filter { it.collisionBox intersects other.collisionBox }

    override fun occupiedTiles(): Iterable<TilePosition> =
        flatMapTo(hashSetOf()) { it.allOccupiedTiles().asSequence() }
}

class TileHashMapTest {
    val oracle = Oracle()
    val testee = TileHashMap<SimpleSpatial>()
    val random = Random("space".hashCode())
    val bounds = 50

    private fun addRandom() {
        repeat(1000) {
            val pos = pos(random.nextDouble(bounds.toDouble()), random.nextDouble(bounds.toDouble()))
            val sizeDown = pos(random.nextDouble(2.0), random.nextDouble(2.0))
            val sizeUp = pos(random.nextDouble(2.0), random.nextDouble(2.0))

            val spatial = SimpleSpatial(
                pos,
                BoundingBox(-sizeDown, +sizeUp)
            )

            oracle.add(spatial)
            testee.add(spatial)
        }
    }

    private fun removeHalf() {
        val toRemove = oracle.toList().shuffled(random).take(oracle.size / 2)
        oracle.removeAll(toRemove)
        testee.removeAll(toRemove)
    }

    @Test
    fun testBasicOps() {
        assert(testee.isEmpty())

        addRandom()
        assert(oracle.size == testee.size)
        assert(oracle.containsAll(testee))
        assert(testee.containsAll(oracle))

        testee.clear()
        assert(testee.isEmpty())
    }

    fun <T> assertMatches(
        oracleSeq: Sequence<T>,
        testeeSeq: Sequence<T>
    ) {
        val oracleResult = oracleSeq.toSet()
        val testeeListResult = testeeSeq.toList()
        val testeeResult = testeeListResult.toSet()
        assertEquals(testeeListResult.size, testeeResult.size, "Duplicate elements in testee result")
        assertEquals(oracleResult, testeeResult)
    }

    @Test
    fun testQueryTile() {
        addRandom()
        removeHalf()
        for (i in 0 until 10) {
            val tile = tilePos(random.nextInt(bounds), random.nextInt(bounds))
            assertMatches(oracle.getInTile(tile), testee.getInTile(tile))
        }
    }

    @Test
    fun testQueryArea() {
        addRandom()
        removeHalf()
        repeat(100) {
            val top = pos(random.nextDouble(bounds.toDouble()), random.nextDouble(bounds.toDouble()))
            val size = pos(random.nextDouble(3.0), random.nextDouble(3.0))
            val area = bbox(top - size / 2, top + size / 2)

            assertMatches(oracle.getInArea(area), testee.getInArea(area))
            assertMatches(oracle.getInArea(area.roundOutToTileBbox()), testee.getInArea(area.roundOutToTileBbox()))
        }
    }

    @Test
    fun testQueryPoint() {
        addRandom()
        removeHalf()
        for (i in 0 until 10) {
            val point = pos(random.nextDouble(bounds.toDouble()), random.nextDouble(bounds.toDouble()))
            assertMatches(oracle.getAtPoint(point), testee.getAtPoint(point))
        }
    }

    @Test
    fun testQueryCircle() {
        addRandom()
        removeHalf()
        for (i in 0 until 10) {
            val center = pos(random.nextDouble(bounds.toDouble()), random.nextDouble(bounds.toDouble()))
            val radius = random.nextDouble(10.0)
            assertMatches(oracle.getPosInCircle(center, radius), testee.getPosInCircle(center, radius))
        }
    }

    @Test
    fun getOccupiedTiles() {
        addRandom()
        removeHalf()
        assertMatches(oracle.occupiedTiles().asSequence(), testee.occupiedTiles().asSequence())
    }

    @Test
    fun tileIsOccupied() {
        addRandom()
        removeHalf()
        tileBbox(0, 0, bounds, bounds).iterateTiles().forEach {
            assertEquals(oracle.tileIsOccupied(it), testee.tileIsOccupied(it))
        }
    }

    @Test
    fun getEnclosingBox() {
        val s1 = SimpleSpatial(pos(1.5, 1.5), BoundingBox.around(Position.ZERO, 0.5))
        val s2 = SimpleSpatial(pos(3.5, 4.5), BoundingBox.around(Position.ZERO, 0.5))
        val s3 = SimpleSpatial(pos(5.5, 2.5), BoundingBox.around(Position.ZERO, 0.5))
        testee.addAll(listOf(s1, s2, s3))
        assertEquals(BoundingBox(1.0, 1.0, 6.0, 5.0), testee.enclosingBox())
        testee.clear()


        addRandom()
        removeHalf()
        assertEquals(oracle.enclosingBox(), testee.enclosingBox())

    }
}
