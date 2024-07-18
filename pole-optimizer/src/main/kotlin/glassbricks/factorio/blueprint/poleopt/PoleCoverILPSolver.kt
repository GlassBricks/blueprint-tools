package glassbricks.factorio.blueprint.poleopt

import com.google.ortools.Loader
import com.google.ortools.linearsolver.MPSolver
import glassbricks.factorio.blueprint.entity.Entity
import kotlin.math.floor


public class PoleCoverILPSolver(
    public val poles: PoleCoverProblem,
    public val solver: ILPLikeSolver,
) {
    public val poleVariables: Map<CandidatePole, ILPLikeSolver.Variable> = poles.candidatePoles.associateWith { pole ->
        solver.addVariable("${pole.prototype.name}(${pole.position.x.floorToInt()},${pole.position.y.floorToInt()})")
    }

    public fun setObjective() {
        solver.setObjectiveMinimze(poleVariables.entries.associate { it.value to it.key.cost })
    }

    public fun addEntitiesPoweredConstraint() {
        for ((_, poles) in poles.poweredByMap) {
            if (poles.isEmpty()) continue
            solver.addDisjunction(poles.map { poleVariables[it]!!.asTrue() })
        }
    }

    public fun addNonOverlappingConstraint() {
        for (tile in poles.candidatePoles.occupiedTiles()) {
            val poles = poles.candidatePoles.getInTile(tile).asIterable()
            solver.addAtMostOne(poles.map { poleVariables[it]!!.asTrue() })
        }
    }

    public fun addForceIncludePolesConstraint() {
        for ((pole, variable) in poleVariables) {
            if (pole.forceInclude) solver.addMustBeTrue(variable.asTrue())
        }
    }

    public fun solve(): Any = solver.solve()

    public fun poleIsSelected(pole: CandidatePole): Boolean =
        poleVariables[pole].let { it != null && it.solutionValue() }

    /**
     * Gets any one of the poles that power the given entity, if any.
     */
    public fun getPoweringPole(entity: Entity): CandidatePole? =
        poles.poweredByMap[entity]?.firstOrNull { poleIsSelected(it) }

    public fun getSelectedPoles(): List<CandidatePole> =
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
