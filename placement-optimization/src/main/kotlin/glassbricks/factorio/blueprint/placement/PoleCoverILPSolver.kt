package glassbricks.factorio.blueprint.placement

import com.google.ortools.Loader
import com.google.ortools.linearsolver.MPSolver
import glassbricks.factorio.blueprint.entity.Entity
import glassbricks.factorio.blueprint.placement.poles.PoleCoverProblem
import glassbricks.factorio.blueprint.placement.poles.PolePlacement
import kotlin.math.floor


public class PoleCoverILPSolver(
    public val poles: PoleCoverProblem,
    public val solver: ILPLikeSolver,
) {
    public val poleVariables: Map<PolePlacement, ILPLikeSolver.BoolVar> = poles.candidatePoles.associateWith { pole ->
        solver.addVariable("${pole.prototype.name}(${pole.position.x.floorToInt()},${pole.position.y.floorToInt()})")
    }

    public fun setObjective() {
        solver.setObjectiveMinimze(poleVariables.entries.associate { it.value to it.key.cost })
    }

    public fun addEntitiesPoweredConstraint() {
        for ((_, poles) in poles.poweredByMap) {
            if (poles.isEmpty()) continue
            solver.addDisjunction(poles.map { poleVariables[it]!! })
        }
    }

    public fun addNonOverlappingConstraint() {
        for (tile in poles.candidatePoles.occupiedTiles()) {
            val poles = poles.candidatePoles.getInTile(tile).asIterable()
            solver.addAtMostOne(poles.map { poleVariables[it]!! })
        }
    }

    public fun addForceIncludePolesConstraint() {
        for ((pole, variable) in poleVariables) {
            if (pole.forceInclude) solver.addMustBeTrue(variable)
        }
    }

    public fun solve(): Any = solver.solve()

    public fun poleIsSelected(pole: PolePlacement): Boolean =
        poleVariables[pole].let { it != null && it.solutionValue() }

    /**
     * Gets any one of the poles that power the given entity, if any.
     */
    public fun getPoweringPole(entity: Entity): PolePlacement? =
        poles.poweredByMap[entity]?.firstOrNull { poleIsSelected(it) }

    public fun getSelectedPoles(): List<PolePlacement> =
        poleVariables.keys.filter { poleIsSelected(it) }
}

private fun Double.floorToInt(): Int = floor(this).toInt()

public fun defaultPoleCoverILPSolver(
    poles: PoleCoverProblem,
    solver: ILPLikeSolver = ILPSolver(MPSolver.createSolver("SAT"))
): PoleCoverILPSolver = PoleCoverILPSolver(poles, solver).apply {
        setObjective()
        addEntitiesPoweredConstraint()
        addNonOverlappingConstraint()
        addForceIncludePolesConstraint()
    }


@Suppress("unused")
private val init = Loader.loadNativeLibraries()
