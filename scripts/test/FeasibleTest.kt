import glassbricks.factorio.blueprint.entity.ElectricPole
import glassbricks.factorio.blueprint.json.BlueprintJson
import glassbricks.factorio.blueprint.json.importBlueprintJson
import glassbricks.factorio.blueprint.model.Blueprint
import glassbricks.factorio.blueprint.placement.BpModelBuilder
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import kotlin.io.path.fileSize
import kotlin.test.assertTrue

class FeasibleTest {
    companion object {
        @JvmStatic
        fun findFiles(): Array<File> = File("../test-blueprints").listFiles()!!
    }

    @ParameterizedTest
    @MethodSource("findFiles")
    fun testIsFeasible(file: File) {
        if (file.toPath().fileSize() > 1024 * 100) return
        val blueprint = importBlueprintJson(file) as? BlueprintJson ?: return
        val bp = Blueprint(blueprint)
        val model = BpModelBuilder(bp).apply {
            optimizeBeltLines = true
            if (entities.any { it is ElectricPole })
                optimizePoles = entities.mapNotNull {
                    if (it is ElectricPole) it.prototype else null
                }.distinct()
        }.build()
        model.timeLimitInSeconds = 30.0
        val solution = model.solve(optimize = false)
        assertTrue(solution.isOk, solution.status.toString())
    }
}
