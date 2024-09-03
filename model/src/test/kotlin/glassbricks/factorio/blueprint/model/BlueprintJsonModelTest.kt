package glassbricks.factorio.blueprint.model

import glassbricks.factorio.blueprint.entity.BlueprintEntity
import glassbricks.factorio.blueprint.entity.entityFromJson
import glassbricks.factorio.blueprint.entity.setEntitiesFrom
import glassbricks.factorio.blueprint.json.*
import glassbricks.factorio.blueprint.prototypes.VanillaPrototypes
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class BlueprintJsonModelTest {

    fun assertBpMatches(
        bp: BlueprintJson,
        model: Blueprint,
    ) {
        assertEquals(bp.label, model.label)
        assertEquals(bp.label_color, model.label_color)
        assertEquals(bp.description, model.description)
        assertEquals(bp.icons, model.icons)
        assertEquals(bp.snap_to_grid, model.snapToGridSettings?.snapToGrid)
        assertEquals(bp.absolute_snapping, model.snapToGridSettings?.positionRelativeToGrid != null)
        if (bp.absolute_snapping) {
            assertEquals(bp.position_relative_to_grid, model.snapToGridSettings?.positionRelativeToGrid)
        }
        assertEquals(bp.item, model.item)
        assertEquals(bp.version, model.version)

        assertEquals(bp.tiles, model.tiles.toTileList())
        assertEquals(bp.tiles.toTileMap(), model.tiles)

        val entities = BlueprintJson(icons = emptyList()).apply {
            setEntitiesFrom(model.entities)
        }.entities
        for ((entity1, entity2) in
        bp.entities!!.sortedBy { it.position }
            .zip(entities!!.sortedBy { it.position })) {
            assertEntitiesSame(entity1, entity2)
        }
    }

    private fun assertEntitiesSame(
        entity1: EntityJson,
        entity2: EntityJson,
    ) {
        removeEntityNumber(entity1)
        removeEntityNumber(entity2)
        if (entity1 != entity2) {
            val toModelEntity = VanillaPrototypes.entityFromJson<BlueprintEntity>(entity1)
            println(toModelEntity)
        }
        assertEquals(entity1, entity2)
    }

    private fun removeEntityNumber(it: EntityJson) {
        it.entity_number = EntityNumber(1)
        it.neighbours = null
        it.connections = null
    }

    fun testBlueprint(name: String) {
        val bp = importBlueprintFrom(File("../test-blueprints/$name.txt").inputStream()) as BlueprintJson

        bp.entities?.forEach {
            if (it.connections == null && it.control_behavior != null && !it.control_behavior!!.connect_to_logistic_network) {
                it.control_behavior = null
            }
            it.control_behavior?.let { cb ->
                if (!cb.connect_to_logistic_network) {
                    cb.logistic_condition = null
                }
            }
            if (it.name in VanillaPrototypes.dataRaw.inserter) it.control_behavior?.let { cb ->
                if (cb.circuit_condition != null && cb.circuit_mode_of_operation.let { mode ->
                        !(mode == null || mode == InserterModeOfOperation.EnableDisable.asMode())
                    }) {
                    cb.circuit_condition = null
                }
                if (!cb.circuit_read_hand_contents) {
                    cb.circuit_hand_read_mode = null
                }
            }
            if (it.name in VanillaPrototypes.dataRaw.`transport-belt`) it.control_behavior?.let { cb ->
                if (cb.circuit_condition != null && cb.circuit_enable_disable != true) {
                    cb.circuit_condition = null
                }
                if (!cb.circuit_read_hand_contents) {
                    cb.circuit_contents_read_mode = BeltReadMode.Pulse
                }
            }
            if (it.name in VanillaPrototypes.dataRaw.`train-stop`) it.control_behavior?.let { cb ->
                if (!cb.read_stopped_train) {
                    cb.train_stopped_signal = null
                }
            }
            VanillaPrototypes.dataRaw.accumulator[it.name]?.let { proto ->
                if (it.control_behavior?.output_signal == proto.default_output_signal.toJsonBasic()) {
                    it.control_behavior = null
                }
            }
        }

        val model = Blueprint(bp)
        assertBpMatches(bp, model)

        val back = model.toJson()
        val backAgain = Blueprint(back).toJson()
//        assertEquals(back.entities, backAgain.entities)
        for ((e1, e2) in back.entities!!.zip(backAgain.entities!!)) {
            assertEquals(e1, e2)
        }
    }

    @Test
    fun `test bp1`() {
        testBlueprint("bp1")
    }

    @Test
    fun `test base8`() {
        testBlueprint("base8")
    }

}
