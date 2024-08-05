package glassbricks.factorio.blueprint.placement.ops

import glassbricks.factorio.blueprint.Direction
import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.entity.*
import glassbricks.factorio.blueprint.json.IOType
import glassbricks.factorio.blueprint.prototypes.EntityPrototype
import glassbricks.factorio.blueprint.prototypes.TransportBeltPrototype
import glassbricks.factorio.blueprint.prototypes.UndergroundBeltPrototype
import glassbricks.factorio.blueprint.prototypes.VanillaPrototypes
import glassbricks.factorio.blueprint.shifted
import glassbricks.factorio.blueprint.tilePos
import kotlin.test.*

class OptimizeStraightBeltsTest {
    private val belt: TransportBeltPrototype = VanillaPrototypes.getAs("transport-belt")
    private val ugBelt: UndergroundBeltPrototype = VanillaPrototypes.getAs("underground-belt")
    private val blocker: EntityPrototype = VanillaPrototypes.getAs("stone-wall")
    private val inserter: EntityPrototype = VanillaPrototypes.getAs("inserter")

    private fun createBeltLine(
        inStr: String,
        ugRelCost: Double = 2.5,
        startPos: TilePosition = tilePos(2, 2),
        direction: Direction = Direction.East,
        isUgInput: Boolean = false,
        isUgOutput: Boolean = false
    ): Pair<MutableSpatialDataStructure<BlueprintEntity>, BeltLine> {
        val entities = DefaultSpatialDataStructure<BlueprintEntity>()
        val mustNotBeEmpty = mutableSetOf<Int>()
        for ((index, c) in inStr.withIndex()) {
            val pos = startPos.shifted(direction, index)
            when (c) {
                'r' -> mustNotBeEmpty.add(index)
                '#' -> entities.add(blocker.placedAtTile(pos))
                else -> {}
            }
        }

        val line = BeltLine(
            start = startPos,
            direction = direction,
            isUgInput = isUgInput,
            isUgOutput = isUgOutput,
            length = inStr.length,
            mustNotBeEmpty = mustNotBeEmpty,
            originalEntities = emptySet(),
            beltPrototype = belt,
            ugPrototype = ugBelt,
            costs = BeltCosts(mapOf(belt to 1.0, ugBelt to ugRelCost))
        )
        return entities to line
    }

    private fun testBeltLine(
        inStr: String,
        ugRelCost: Double,
        startPos: TilePosition = tilePos(0, 0),
        direction: Direction = Direction.East,
        isUgInput: Boolean = false,
        isUgOutput: Boolean = false
    ): String {
        val (entities, line) = createBeltLine(inStr, ugRelCost, startPos, direction, isUgInput, isUgOutput)
        assertNotNull(optimizeBeltLineAndAdd(entities, line, null))
        return getBeltsAsStr(entities, startPos, direction, inStr.length)
    }

    private fun getBeltsAsStr(
        entities: MutableSpatialDataStructure<BlueprintEntity>,
        startPos: TilePosition,
        direction: Direction,
        length: Int
    ): String {
        val chars = (0..<length).map { i ->
            val pos = startPos.shifted(direction, i)
            val entity = entities.getInTile(pos)
                .firstOrNull()
            if (entity is TransportBeltConnectable) {
                assertEquals(direction, entity.direction)
            }
            when {
                entity == null -> ' '
                entity is TransportBelt -> '='
                entity is UndergroundBelt && entity.ioType == IOType.Input -> '>'
                entity is UndergroundBelt && entity.ioType == IOType.Output -> '<'
                entity is Wall -> '#'
                else -> '?'
            }
        }
        return chars.joinToString("")
    }

    @Test
    fun `if rel cost greater than length, uses belts where possible`() {
        val result = testBeltLine("      ", ugRelCost = 7.0)
        assertEquals("======", result)
    }

    @Test
    fun `uses ug belt if obstacle in the way`() {
        val result = testBeltLine("   #  ", ugRelCost = 7.0)
        assertEquals(">  # <", result)
    }

    @Test
    fun `does not skip tile if it must not be empty`() {
        val result = testBeltLine("  #r  ", ugRelCost = 7.0)
        assertEquals("> #<==", result)
        val result2 = testBeltLine(" r#   ", ugRelCost = 7.0)
        assertEquals("=>#  <", result2)
        val result3 = testBeltLine("  # r ", ugRelCost = 7.0)
        assertEquals("> # <=", result3)
        val result4 = testBeltLine(" r # r ", ugRelCost = 7.0)
        assertEquals("=> # <=", result4)
    }

    @Test
    fun `if rel cost gt 1, uses underground belts`() {
        val result = testBeltLine(" ".repeat(6), ugRelCost = 2.5)
        assertEquals(">    <", result)
    }

    @Test
    fun `uses at least 1 belt if longer than reach`() {
        val result = testBeltLine(" ".repeat(7), ugRelCost = 2.5)
        assertEquals("=>    <", result)
    }

    @Test
    fun `with isInputUg true`() {
        val result = testBeltLine(" ".repeat(7), ugRelCost = 2.5, isUgInput = true)
        assertEquals("    <==", result)
    }

    @Test
    fun `with isOutputUg true`() {
        val result = testBeltLine(" ".repeat(7), ugRelCost = 2.5, isUgOutput = true)
        assertEquals("==>    ", result)
    }

    @Test
    fun `fully undergrounded`() {
        val result = testBeltLine(" ".repeat(6 * 2), ugRelCost = 2.5)
        assertEquals(">    <>    <", result)
    }

    @Test
    fun `only middle underground`() {
        val result = testBeltLine(" ".repeat(5 * 2), ugRelCost = 2.5, isUgInput = true, isUgOutput = true)
        assertEquals("    <>    ", result)
    }

    @Test
    fun `underground spam`() {
        val result = testBeltLine("r".repeat(8), ugRelCost = 0.5)
        assertEquals("><><><><", result)
    }

    private fun createEntities(
        inStr: String,
        startPos: TilePosition = tilePos(2, 2),
    ): MutableSpatialDataStructure<BlueprintEntity> {
        val entities = DefaultSpatialDataStructure<BlueprintEntity>()
        for ((index, c) in inStr.withIndex()) {
            val pos = startPos.shifted(Direction.East, index)
            when (c) {
                '=', '+', 'v', '^' -> entities.add(belt.placedAtTile(pos, Direction.East))
                '>', '/' -> entities.add(
                    ugBelt.placedAtTile(pos, Direction.East).also { it as UndergroundBelt; it.ioType = IOType.Input })

                '<', '\\' -> entities.add(
                    ugBelt.placedAtTile(pos, Direction.East).also { it as UndergroundBelt; it.ioType = IOType.Output })

                '#' -> entities.add(blocker.placedAtTile(pos, Direction.East))
            }
            when (c) {
                '+', '/', '\\' -> entities.add(
                    belt.placedAtTile(pos.shifted(Direction.North), Direction.South)
                )

                '^' -> entities.add(
                    inserter.placedAtTile(pos.shifted(Direction.North), Direction.South)
                )

                'v' -> entities.add(
                    inserter.placedAtTile(pos.shifted(Direction.North), Direction.North)
                )
            }
        }
        return entities
    }

    private fun testOptimizeBelts(
        inStr: String,
        ugRelCost: Double,
        startPos: TilePosition = tilePos(2, 2),
    ): String {
        val entities = createEntities(inStr, startPos)
        optimizeBeltLinesInBp(entities, getBeltCosts(ugRelCost))

        return getBeltsAsStr(entities, startPos, Direction.East, inStr.length)
    }

    private fun getBeltCosts(ugRelCost: Double): BeltCosts = BeltCosts(
        mapOf(
            belt to 1.0,
            ugBelt to ugRelCost
        )
    )

    @Test
    fun `getting straight belt line`() {
        val entities = createEntities("> # <=>     <", startPos = tilePos(0, 0))
        val lines = getBeltLines(entities, getBeltCosts(2.5))
        assertEquals(1, lines.size)
        val line = lines.first()
        assertEquals(7, line.length)
        assertEquals(Direction.East, line.direction)
        assertFalse(line.isUgInput)
        assertFalse(line.isUgOutput)
        assertTrue(line.mustNotBeEmpty.isEmpty())
        assertTrue(line.originalEntities.size == 4)
        assertTrue(line.originalEntities.all { it is TransportBeltConnectable })
        assertEquals(belt, line.beltPrototype)
        assertEquals(ugBelt, line.ugPrototype)
    }

    @Test
    fun `getting belt line with side loading`() {
        val entities = createEntities("===+", startPos = tilePos(0, 0))
        val lines = getBeltLines(entities, getBeltCosts(2.5))
        assertEquals(1, lines.size)
        val line = lines.first()
        assertEquals(3, line.length)
        assertEquals(Direction.East, line.direction)
    }

    @Test
    fun `getting belt line with circuit connections`() {
        val entities = createEntities("=====", startPos = tilePos(0, 0))
        val entity4 = entities.getInTile(tilePos(3, 0)).first() as TransportBelt
        val entity5 = entities.getInTile(tilePos(4, 0)).first() as TransportBelt
        entity4.circuitConnections.red.add(entity5)
        val lines = getBeltLines(entities, getBeltCosts(2.5))
        assertEquals(1, lines.size)
        val line = lines.first()
        assertEquals(3, line.length)
        assertEquals(Direction.East, line.direction)
    }

    @Test
    fun `belt overlapped by ug belt ignored`() {
        val entities = createEntities(">====<")
        val lines = getBeltLines(entities, getBeltCosts(2.5))
        assertEquals(1, lines.size)
        val line = lines.first()
        assertEquals(6, line.length)
    }

    @Test
    fun `belt with inserter connections must be included`() {
        val entities = createEntities("==v=^=")
        val lines = getBeltLines(entities, getBeltCosts(2.5))
        assertEquals(1, lines.size)
        val line = lines.first()
        assertEquals(6, line.length)
        assertEquals(setOf(2, 4), line.mustNotBeEmpty)
    }

    @Test
    fun `replacing ug with normal belt`() {
        val result = testOptimizeBelts(" >  < ", ugRelCost = 2.5)
        assertEquals(" ==== ", result)
    }

    @Test
    fun `replacing belt with ug`() {
        val result = testOptimizeBelts(" ===== ", ugRelCost = 2.5)
        assertEquals(" >   < ", result)
    }

    @Test
    fun `better handling over obstacle`() {
        val result = testBeltLine("=>#<>#<==", ugRelCost = 2.5)
        assertEquals("=>#  #<==", result)
        val result2 = testOptimizeBelts("=>#<>#<==", ugRelCost = 2.5)
        assertEquals(result, result2)
    }

    @Test
    fun `ignores belts of different prototype`() {
        val entities = DefaultSpatialDataStructure<BlueprintEntity>()
        val fastBelt = VanillaPrototypes.getAs<TransportBeltPrototype>("fast-transport-belt")
        repeat(5) { index ->
            val pos = tilePos(0, 0).shifted(Direction.East, index)
            entities.add(fastBelt.placedAtTile(pos, Direction.East))
        }
        val lines = getBeltLines(entities, getBeltCosts(2.5))
        assertEquals(0, lines.size)
    }
}
