import glassbricks.factorio.blueprint.DefaultSpatialDataStructure
import glassbricks.factorio.blueprint.Direction
import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.entity.BlueprintEntity
import glassbricks.factorio.blueprint.entity.copyEntities
import glassbricks.factorio.blueprint.entity.createBpEntity
import glassbricks.factorio.blueprint.entity.placedAtTile
import glassbricks.factorio.blueprint.json.exportTo
import glassbricks.factorio.blueprint.model.Blueprint
import glassbricks.factorio.blueprint.placement.getAllUnrotatedTilePlacements
import glassbricks.factorio.blueprint.prototypes.VanillaPrototypes
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals


class PolesAroundCurvedRailTest {
    @Test
    fun `test entities around curved rail`() {
        val bp = Blueprint()
        bp.entities.add(VanillaPrototypes.createBpEntity("curved-rail", Position.ZERO, Direction.East))

        val poles = getAllUnrotatedTilePlacements<_, BlueprintEntity>(
            listOf(VanillaPrototypes["small-electric-pole"]!!),
            bp.entities.enclosingTileBox(),
            { bp.entities.getColliding(it).none() },
            { proto, pos -> proto.placedAtTile(pos) }
        )
        run {
            val entities2 = bp.entities.copyEntities()
            entities2.addAll(poles)
            Blueprint(entities = entities2).toJson().exportTo(File("output/poles-around-curved-rail.txt"))
        }

        val chests = getAllUnrotatedTilePlacements<_, BlueprintEntity>(
            listOf(VanillaPrototypes["wooden-chest"]!!),
            bp.entities.enclosingTileBox(),
            { bp.entities.getColliding(it).none() },
            { proto, pos -> proto.placedAtTile(pos) }
        )

        run {
            val entities2 = DefaultSpatialDataStructure<BlueprintEntity>()
            entities2.addAll(chests)
            Blueprint(entities = entities2).toJson().exportTo(File("output/chests-around-curved-rail.txt"))
        }

        assertEquals(25, poles.size)
        assertEquals(21, chests.size)
    }
}
