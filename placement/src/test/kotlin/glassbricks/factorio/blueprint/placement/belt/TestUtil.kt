package glassbricks.factorio.blueprint.placement.belt

import glassbricks.factorio.blueprint.DefaultSpatialDataStructure
import glassbricks.factorio.blueprint.Direction
import glassbricks.factorio.blueprint.MutableSpatialDataStructure
import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.entity.BlueprintEntity
import glassbricks.factorio.blueprint.entity.TransportBelt
import glassbricks.factorio.blueprint.entity.TransportBeltConnectable
import glassbricks.factorio.blueprint.entity.UndergroundBelt
import glassbricks.factorio.blueprint.entity.Wall
import glassbricks.factorio.blueprint.entity.placedAtTile
import glassbricks.factorio.blueprint.json.IOType
import glassbricks.factorio.blueprint.placement.CardinalDirection
import glassbricks.factorio.blueprint.placement.shifted
import glassbricks.factorio.blueprint.placement.toCardinalDirection
import glassbricks.factorio.blueprint.prototypes.VanillaPrototypes
import glassbricks.factorio.blueprint.shifted
import kotlin.test.assertEquals

val normalBelt by lazy { VanillaPrototypes.getBeltTier("transport-belt")!! }
val fastTier by lazy { VanillaPrototypes.getBeltTier("fast-transport-belt")!! }
val belt get() = normalBelt.beltProto
val ug get() = normalBelt.ugProto
val blocker by lazy { VanillaPrototypes["stone-wall"]!! }
val inserter by lazy { VanillaPrototypes["inserter"]!! }

fun getBeltsAsStr(
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

fun createEntities(
    inStr: String,
    startPos: TilePosition,
): MutableSpatialDataStructure<BlueprintEntity> {
    val entities = DefaultSpatialDataStructure<BlueprintEntity>()
    for ((index, c) in inStr.withIndex()) {
        val pos = startPos.shifted(Direction.East, index)
        when (c) {
            '=', '+', 'v', '^', '|' -> entities.add(belt.placedAtTile(pos, Direction.East))
            '>', '/' -> entities.add(
                ug.placedAtTile(pos, Direction.East).also { it as UndergroundBelt; it.ioType = IOType.Input })

            '<', '\\' -> entities.add(
                ug.placedAtTile(pos, Direction.East).also { it as UndergroundBelt; it.ioType = IOType.Output })

            '#' -> entities.add(blocker.placedAtTile(pos, Direction.East))
        }
        when (c) {
            '+', '/', '\\', '|' -> entities.add(
                belt.placedAtTile(pos.shifted(Direction.North), Direction.South)
            )


            'v' -> entities.add(
                inserter.placedAtTile(pos.shifted(Direction.North), Direction.North)
            )
        }
        when (c) {
            '^', '|' -> entities.add(
                inserter.placedAtTile(pos.shifted(Direction.North), Direction.South)
            )
        }
    }
    return entities
}
