package glassbricks.factorio.blueprint.poleopt

import com.google.ortools.Loader
import com.google.ortools.linearsolver.MPSolver
import com.google.ortools.linearsolver.MPVariable
import glassbricks.factorio.blueprint.entity.Entity

public class PoleILP(
    public val solver: MPSolver,
    public val poleSet: CandidatePoleSet,
    public val poleVariables: Map<CandidatePole, MPVariable>,
) {

    public fun setObjective() {
        solver.objective().apply {
            setMinimization()
            for ((pole, variable) in poleVariables)
                setCoefficient(variable, pole.cost)
        }
    }

    public fun addAllPoweredConstraint() {
        for ((_, poles) in poleSet.poweredBy) {
            val constraint = solver.makeConstraint(1.0, Double.POSITIVE_INFINITY)
            for (pole in poles)
                constraint.setCoefficient(poleVariables[pole]!!, 1.0)
        }
    }

    public fun addNonOverlappingConstraint() {
        for (tile in poleSet.poles.occupiedTiles()) {
            val poles = poleSet.poles.getInTile(tile).toList()
            if (poles.size <= 1) continue
            val constraint = solver.makeConstraint(0.0, 1.0)
            for (pole in poles)
                constraint.setCoefficient(poleVariables[pole]!!, 1.0)
        }
    }

    public fun poleInSolution(pole: CandidatePole): Boolean =
        poleVariables[pole].let { it != null && it.solutionValue() > 0.5 }

    /**
     * Gets any one of the poles that power the given entity, if any.
     */
    public fun getPoweringPole(entity: Entity): CandidatePole? =
        poleSet.poweredBy[entity]?.firstOrNull { poleInSolution(it) }

    public fun getUsedPoles(): Set<CandidatePole> =
        poleVariables.keys.filter { poleInSolution(it) }.toSet()
}

public fun createEmptyPoleILP(poles: CandidatePoleSet, solver: MPSolver): PoleILP {
    val poleVariables = poles.poles.associateWith { pole ->
        solver.makeBoolVar("${pole.prototype.name}_${pole.position.x}_${pole.position.y}")
    }
    return PoleILP(solver, poles, poleVariables)
}

public fun createDefaultPoleILP(
    poles: CandidatePoleSet,
    solver: MPSolver = MPSolver.createSolver("GLOP")!!
): PoleILP = createEmptyPoleILP(poles, solver).apply {
    setObjective()
    addAllPoweredConstraint()
    addNonOverlappingConstraint()
}

@Suppress("unused")
private val init = Loader.loadNativeLibraries()
