package glassbricks.factorio.blueprint.placement.ops

import com.google.ortools.sat.CpSolverStatus
import glassbricks.factorio.blueprint.Direction
import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.entity.*
import glassbricks.factorio.blueprint.json.IOType
import glassbricks.factorio.blueprint.placement.CardinalDirection
import glassbricks.factorio.blueprint.placement.EntityPlacementModel
import glassbricks.factorio.blueprint.placement.OptionalEntityPlacement
import glassbricks.factorio.blueprint.placement.belt.BeltGridConfig
import glassbricks.factorio.blueprint.placement.belt.BeltTier
import glassbricks.factorio.blueprint.placement.belt.addBeltGrid
import glassbricks.factorio.blueprint.placement.shifted
import glassbricks.factorio.blueprint.placement.toBlueprintEntities
import glassbricks.factorio.blueprint.placement.toCardinalDirection
import glassbricks.factorio.blueprint.prototypes.EntityPrototype
import glassbricks.factorio.blueprint.prototypes.TransportBeltConnectablePrototype
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

    private fun createBeltLine(
        inStr: String,
        startPos: TilePosition = tilePos(2, 2),
        direction: CardinalDirection = CardinalDirection.East,
    ): Pair<MutableSpatialDataStructure<BlueprintEntity>, BeltLine> {
        val entities = DefaultSpatialDataStructure<BlueprintEntity>()
        val mustBeNotEmpty = mutableSetOf<TilePosition>()
        for ((index, c) in inStr.withIndex()) {
            val pos = startPos.shifted(direction, index)
            when (c) {
                'r' -> mustBeNotEmpty.add(pos)
                '#' -> entities.add(blocker.placedAtTile(pos))
                else -> {}
            }
        }

        val line = BeltLine(
            start = startPos,
            direction = direction,
            length = inStr.length,
            mustBeNotEmpty = mustBeNotEmpty.toList(),
            mustMatchExisting = emptyMap(), // todo: test
            beltTiers = setOf(BeltTier(belt, ugBelt)),
        )
        return entities to line
    }

    private fun testBeltLine(
        inStr: String,
        ugRelCost: Double,
        startPos: TilePosition = tilePos(0, 0),
        direction: CardinalDirection = CardinalDirection.East,
    ): String {
        val (entities, line) = createBeltLine(
            inStr,
            startPos,
            direction,
        )
        val beltGrid = BeltGridConfig()
        beltGrid.addBeltLine(line)

        val model = EntityPlacementModel()
        model.addBeltGrid(beltGrid)
        model.addFixedEntities(entities)

        for (placement in model.placements) {
            if (placement.prototype is UndergroundBeltPrototype && placement is OptionalEntityPlacement) {
                placement.cost = ugRelCost
            }
        }
        val solution = model.solve()
        assertEquals(CpSolverStatus.OPTIMAL, solution.status)

        val resultEntities = solution.toBlueprintEntities()
        return getBeltsAsStr(resultEntities, startPos, direction, inStr.length)
    }

    private fun getBeltsAsStr(
        entities: MutableSpatialDataStructure<BlueprintEntity>,
        startPos: TilePosition,
        direction: CardinalDirection,
        length: Int,
    ): String {
        val chars = (0..<length).map { i ->
            val pos = startPos.shifted(direction, i)
            val entity = entities.getInTile(pos)
                .firstOrNull()
            if (entity is TransportBeltConnectable) {
                assertEquals(direction, entity.direction.toCardinalDirection())
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
        val result = testBeltLine(" ".repeat(6), ugRelCost = 2.3)
        assertEquals(">    <", result)
    }

    @Test
    fun `uses at least 1 belt if longer than reach`() {
        val result = testBeltLine(" ".repeat(7), ugRelCost = 2.3)
        assertEquals("=>    <", result)
    }

    @Test
    fun `fully undergrounded`() {
        val result = testBeltLine(" ".repeat(6 * 2), ugRelCost = 2.3)
        assertEquals(">    <>    <", result)
    }

    @Test
    fun `underground spam`() {
        val result = testBeltLine("r".repeat(8), ugRelCost = 0.5)
        assertEquals("><><><><", result)
    }

    private val inserter: EntityPrototype = VanillaPrototypes.getAs("fast-inserter")
    private val beltTier = BeltTier(belt, ugBelt)

    private fun createEntities(
        inStr: String,
        startPos: TilePosition,
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

    @Test
    fun `getting straight belt line`() {
        val entities = createEntities("> # <=>     <", startPos = tilePos(0, 0))
        val lines = getBeltLines(entities)
        val line = lines.maxBy { it.length }
        assertEquals(7, line.length)
        assertEquals(CardinalDirection.East, line.direction)
        assertTrue(line.mustBeNotEmpty.isEmpty())
        assertEquals(setOf(BeltTier(belt, ugBelt)), line.beltTiers)
    }

    @Test
    fun `getting belt line with side loading`() {
        val entities = createEntities("===+", startPos = tilePos(0, 0))
        val lines = getBeltLines(entities)
        val line = lines.maxBy { it.length }
        assertEquals(4, line.length)
        assertEquals(CardinalDirection.East, line.direction)
        assertEquals(mapOf(tilePos(3, 0) to beltTier.belt), line.mustMatchExisting)
    }

    @Test
    fun `getting belt line with circuit connections`() {
        val entities = createEntities("=====", startPos = tilePos(0, 0))
        val entity4 = entities.getInTile(tilePos(3, 0)).first() as TransportBelt
        val entity5 = entities.getInTile(tilePos(4, 0)).first() as TransportBelt
        entity4.circuitConnections.red.add(entity5)
        val lines = getBeltLines(entities)
        val line = lines.maxBy { it.length }
        assertEquals(5, line.length)
        assertEquals(CardinalDirection.East, line.direction)
        assertEquals(mapOf(tilePos(3, 0) to beltTier.belt, tilePos(4, 0) to beltTier.belt), line.mustMatchExisting)
    }

    @Test
    fun `belt with inserter connections must be not empty`() {
        val entities = createEntities("==v=^=", startPos = tilePos(0, 0))
        val lines = getBeltLines(entities)
        assertEquals(1, lines.size)
        val line = lines.first()
        assertEquals(6, line.length)
        assertEquals(
            setOf(
                tilePos(2, 0),
                tilePos(4, 0),
            ), line.mustBeNotEmpty.toSet()
        )
    }

    fun testOptimizeBelts(inStr: String, ugRelCost: Double): String {
        val startPos = tilePos(0, 0)
        val entities = createEntities(inStr, startPos)

        val model = EntityPlacementModel()
        model.addBeltLinesFrom(entities)
        model.addFixedEntities(entities.filter<BlueprintEntity> { it.prototype !is TransportBeltConnectablePrototype })
        for (placement in model.placements) {
            if (placement.prototype is UndergroundBeltPrototype && placement is OptionalEntityPlacement) {
                placement.cost = ugRelCost
            }
        }

        val solution = model.solve()
        assertEquals(CpSolverStatus.OPTIMAL, solution.status)

        val resultEntities = solution.toBlueprintEntities()
        return getBeltsAsStr(resultEntities, startPos, CardinalDirection.East, inStr.length)
    }

    @Test
    fun `replacing ug with normal belt`() {
        val result = testOptimizeBelts(" >  < ", ugRelCost = 2.3)
        assertEquals(" ==== ", result)
    }

    @Test
    fun `replacing belt with ug`() {
        val result = testOptimizeBelts(" ===== ", ugRelCost = 2.3)
        assertEquals(" >   < ", result)
    }

    @Test
    fun `better handling over obstacle`() {
        val result = testOptimizeBelts("=>#<>#<==", ugRelCost = 2.3)
        assertEquals("=>#  #<==", result)
    }


}
