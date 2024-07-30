package glassbricks.factorio.blueprint.placement

import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.entity.Container
import glassbricks.factorio.blueprint.entity.ElectricPole
import glassbricks.factorio.blueprint.entity.Inserter
import glassbricks.factorio.blueprint.entity.createTileSnappedEntity
import glassbricks.factorio.blueprint.prototypes.ElectricPolePrototype
import glassbricks.factorio.blueprint.prototypes.VanillaPrototypes

internal val smallPole get() = VanillaPrototypes.get<ElectricPolePrototype>("small-electric-pole")
fun powerable(position: TilePosition) = VanillaPrototypes.createTileSnappedEntity("inserter", position) as Inserter
fun nonPowerable(position: TilePosition) =
    VanillaPrototypes.createTileSnappedEntity("iron-chest", position) as Container

fun pole(position: TilePosition) =
    VanillaPrototypes.createTileSnappedEntity("small-electric-pole", position) as ElectricPole
