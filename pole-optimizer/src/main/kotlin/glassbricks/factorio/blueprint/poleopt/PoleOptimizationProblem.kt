package glassbricks.factorio.blueprint.poleopt

import com.google.ortools.Loader
import com.google.ortools.linearsolver.MPSolver
import com.google.ortools.linearsolver.MPVariable
import glassbricks.factorio.blueprint.entity.Entity

public class PoleOptimizationProblem(
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

    private val poweredByMap by lazy { poleSet.getPoweredByMap() }

    public fun addAllPoweredConstraint() {
        for ((_, poles) in poweredByMap) {
            if(poles.isEmpty()) continue
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

    public fun solve(): MPSolver.ResultStatus = solver.solve()

    public fun poleIsSelected(pole: CandidatePole): Boolean =
        poleVariables[pole].let { it != null && it.solutionValue() > 0.5 }

    /**
     * Gets any one of the poles that power the given entity, if any.
     */
    public fun getPoweringPole(entity: Entity): CandidatePole? =
        poweredByMap[entity]?.firstOrNull { poleIsSelected(it) }

    public fun getSelectedPoles(): List<CandidatePole> =
        poleVariables.keys.filter { poleIsSelected(it) }
}

public fun createEmptyPoleILP(poles: CandidatePoleSet, solver: MPSolver): PoleOptimizationProblem {
    val poleVariables = poles.poles.associateWith { pole ->
        solver.makeBoolVar("${pole.prototype.name}_${pole.position.x}_${pole.position.y}")
    }
    return PoleOptimizationProblem(solver, poles, poleVariables)
}

public fun createDefaultPoleILP(
    poles: CandidatePoleSet,
    solver: MPSolver = MPSolver.createSolver("SAT")
): PoleOptimizationProblem = createEmptyPoleILP(poles, solver).apply {
    setObjective()
    addAllPoweredConstraint()
    addNonOverlappingConstraint()
}

@Suppress("unused")
private val init = Loader.loadNativeLibraries()
