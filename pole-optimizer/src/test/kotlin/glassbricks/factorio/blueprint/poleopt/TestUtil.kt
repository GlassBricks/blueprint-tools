package glassbricks.factorio.blueprint.poleopt

import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.entity.Container
import glassbricks.factorio.blueprint.entity.ElectricPole
import glassbricks.factorio.blueprint.entity.Inserter
import glassbricks.factorio.blueprint.entity.createTileSnappedEntity
import glassbricks.factorio.blueprint.prototypes.BlueprintPrototypes
import glassbricks.factorio.blueprint.prototypes.ElectricPolePrototype
import java.io.File

val blueprintPrototypes: BlueprintPrototypes by lazy {
    val file = File("../prototypes/src/test/resources/data-raw-dump.json")
    BlueprintPrototypes.loadFromDataRaw(file)
}
val smallPole get() = blueprintPrototypes.getPrototype<ElectricPolePrototype>("small-electric-pole")
fun powerable(position: TilePosition) = blueprintPrototypes.createTileSnappedEntity("inserter", position) as Inserter
fun nonPowerable(position: TilePosition) =
    blueprintPrototypes.createTileSnappedEntity("iron-chest", position) as Container

fun pole(position: TilePosition) =
    blueprintPrototypes.createTileSnappedEntity("small-electric-pole", position) as ElectricPole
