package glassbricks.factorio.blueprint.placement.ops

import glassbricks.factorio.blueprint.json.importBlueprintFrom
import glassbricks.factorio.blueprint.model.Blueprint
import glassbricks.factorio.blueprint.placement.EntityPlacementModel
import glassbricks.factorio.blueprint.placement.poles.addPolePlacements
import glassbricks.factorio.blueprint.placement.smallPole
import glassbricks.factorio.blueprint.prototypes.ContainerPrototype
import glassbricks.factorio.blueprint.prototypes.ElectricPolePrototype
import glassbricks.factorio.blueprint.prototypes.InserterPrototype
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class EntityNudgingTest {
    @Test
    fun `test inserter nudging for better poles`() {
        val bp = Blueprint(importBlueprintFrom(File("../test-blueprints/two-inserters.txt")))
        val model = EntityPlacementModel()
        val (toNudge, fixed) = bp.entities.partition { it.prototype is InserterPrototype }
        model.addFixedEntities(fixed)
        model.addEntityNudging(
            toNudge,
            getItemTransportGraph(bp.entities)
        )
        model.addPolePlacements(listOf(smallPole), bounds = model.placements.enclosingTileBox())

        val result = model.solve()
        val entities = result.getSelectedEntities()
        val poles = entities.filter { it.prototype is ElectricPolePrototype }
        val inserters = entities.filter { it.prototype is InserterPrototype }
        assertEquals(2, inserters.size)
        assertEquals(1, poles.size)
    }

    @Test
    fun `test can nudge containers with inserters`() {
        val bp = Blueprint(importBlueprintFrom(File("../test-blueprints/double-nudge.txt")))
        val model = EntityPlacementModel()
        val (toNudge, fixed) = bp.entities.partition { it.prototype is InserterPrototype || it.prototype is ContainerPrototype }
        model.addFixedEntities(fixed)
        model.addEntityNudging(toNudge, getItemTransportGraph(bp.entities))
        model.addPolePlacements(listOf(smallPole), model.placements.enclosingTileBox())

        val result = model.solve()
        val entities = result.getSelectedEntities()
        val poles = entities.filter { it.prototype is ElectricPolePrototype }
        val inserters = entities.filter { it.prototype is InserterPrototype }
        val containers = entities.filter { it.prototype is ContainerPrototype }
        assertEquals(1, inserters.size)
        assertEquals(2, containers.size)
        assertEquals(1, poles.size)
    }
}
