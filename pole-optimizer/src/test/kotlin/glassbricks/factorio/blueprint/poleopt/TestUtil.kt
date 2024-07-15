package glassbricks.factorio.blueprint.poleopt

import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.entity.Container
import glassbricks.factorio.blueprint.entity.ElectricPole
import glassbricks.factorio.blueprint.entity.Inserter
import glassbricks.factorio.blueprint.entity.createTileSnappedEntity
import glassbricks.factorio.blueprint.prototypes.ElectricPolePrototype
import glassbricks.factorio.blueprintPrototypes

val smallPole get() = blueprintPrototypes.getPrototype<ElectricPolePrototype>("small-electric-pole")
fun powerable(position: TilePosition) = blueprintPrototypes.createTileSnappedEntity("inserter", position) as Inserter
fun nonPowerable(position: TilePosition) =
    blueprintPrototypes.createTileSnappedEntity("iron-chest", position) as Container

fun pole(position: TilePosition) =
    blueprintPrototypes.createTileSnappedEntity("small-electric-pole", position) as ElectricPole
