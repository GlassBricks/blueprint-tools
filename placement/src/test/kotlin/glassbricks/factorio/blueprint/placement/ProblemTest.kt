package glassbricks.factorio.blueprint.placement

import glassbricks.factorio.blueprint.json.BlueprintJson
import glassbricks.factorio.blueprint.json.importBlueprint
import glassbricks.factorio.blueprint.model.BlueprintModel
import glassbricks.factorio.blueprint.placement.ops.addEntityNudgingWithInserters
import glassbricks.factorio.blueprint.placement.poles.addPolePlacements
import glassbricks.factorio.blueprint.prototypes.ContainerPrototype
import glassbricks.factorio.blueprint.prototypes.ElectricPolePrototype
import glassbricks.factorio.blueprint.prototypes.InserterPrototype
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class ProblemTest {
    @Test
    fun `test inserter nudging for better poles`() {
        val bp = BlueprintModel(importBlueprint(File("../test-blueprints/two-inserters.txt")) as BlueprintJson)
        val model = EntityPlacementModel()
        model.addFixedEntities(bp.entities)
        model.addEntityNudgingWithInserters(
            model.placements
                .filter { it.prototype is InserterPrototype }
                .toSet()
        )
        model.addPolePlacements(listOf(smallPole), model.placements.enclosingTileBox())

        val result = model.solve()
        val entities = result.getSelectedEntities()
        val poles = entities.filter { it.prototype is ElectricPolePrototype }
        val inserters = entities.filter { it.prototype is InserterPrototype }
        assertEquals(2, inserters.size)
        assertEquals(1, poles.size)
    }

    @Test
    fun `test can nudge containers with inserters`() {
        val bp = BlueprintModel(importBlueprint(File("../test-blueprints/doublenudge.txt")) as BlueprintJson)
        val model = EntityPlacementModel()
        model.addFixedEntities(bp.entities)
        model.addEntityNudgingWithInserters(
            model.placements
                .filter {
                    it.prototype is InserterPrototype
                            || it.prototype is ContainerPrototype
                }
                .toSet()
        )
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
