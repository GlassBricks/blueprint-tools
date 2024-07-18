package glassbricks.factorio.blueprint.poleopt

import com.google.ortools.linearsolver.MPSolver
import com.google.ortools.linearsolver.MPSolverParameters
import com.google.ortools.linearsolver.MPVariable

public interface ILPLikeSolver {
    public fun addVariable(name: String): Variable

    // constraints
    public fun addDisjunction(literals: Collection<Literal>)
    public fun addDisjunction(vararg literals: Literal): Unit = addDisjunction(literals.toList())

    public fun addImplies(
        condition: Collection<Literal>,
        consequence: Collection<Literal>
    ) {
        addDisjunction(condition.map { it.negate() } + consequence)
    }

    public fun addAtMostOne(literals: Collection<Literal>)

    public fun addMustBeTrue(literal: Literal): Unit

    // objective
    public fun setObjectiveMinimze(costs: Map<Variable, Double>)

    public class Literal(public val variable: Variable, public val isTrue: Boolean) {
        public fun negate(): Literal = Literal(variable, !isTrue)
    }

    public interface Variable {
        public fun asTrue(): Literal = get(true)
        public fun asFalse(): Literal = get(false)
        public operator fun get(value: Boolean): Literal = Literal(this, value)

        public fun solutionValue(): Boolean
    }

    public fun setTimeLimit(millis: Long)

    public fun solve(): Any
}

public class ILPSolver(
    public val mpSolver: MPSolver
) : ILPLikeSolver {
    override fun addVariable(name: String): ILPLikeSolver.Variable = Variable(mpSolver.makeBoolVar(name))

    private class Variable(val mpVariable: MPVariable) :
        ILPLikeSolver.Variable {
        override fun solutionValue(): Boolean = mpVariable.solutionValue() > 0.5
    }

    private fun ILPLikeSolver.Literal.coefValue(): Double = if (isTrue) 1.0 else -1.0

    override fun addDisjunction(literals: Collection<ILPLikeSolver.Literal>) {
        // sum of true + sum of (1 - false) >= 1
        // sum of true + num of false - sum of false >= 1
        // sum of true - sum of false >= 1 - num of false
        val numFalse = literals.count { !it.isTrue }
        val constraint = mpSolver.makeConstraint(1.0 - numFalse, Double.POSITIVE_INFINITY)
        for (literal in literals) {
            val mpVariable = (literal.variable as Variable).mpVariable
            constraint.setCoefficient(mpVariable, literal.coefValue())
        }
    }

    override fun addAtMostOne(literals: Collection<ILPLikeSolver.Literal>) {
        if (literals.size <= 1) return
        // sum of true + sum of (1 - false) <= 1
        // sum of true + num of false - sum of false <= 1
        // sum of true - sum of false <= 1 - num of false
        val numFalse = literals.count { !it.isTrue }
        val constraint = mpSolver.makeConstraint(Double.NEGATIVE_INFINITY, 1.0 - numFalse)
        for (literal in literals) {
            val mpVariable = (literal.variable as Variable).mpVariable
            constraint.setCoefficient(mpVariable, literal.coefValue())
        }
    }

    override fun setObjectiveMinimze(costs: Map<ILPLikeSolver.Variable, Double>) {
        val objective = mpSolver.objective()
        objective.setMinimization()
        for ((variable, cost) in costs) {
            objective.setCoefficient((variable as Variable).mpVariable, cost)
        }
    }

    override fun addMustBeTrue(literal: ILPLikeSolver.Literal) {
        val mpVariable = (literal.variable as Variable).mpVariable
        if (literal.isTrue) {
            mpVariable.setLb(1.0)
        } else {
            mpVariable.setUb(0.0)
        }
    }

    override fun setTimeLimit(millis: Long) {
        mpSolver.setTimeLimit(millis)
    }

    override fun solve(): MPSolver.ResultStatus {
        val params = MPSolverParameters()
        params.setIntegerParam(MPSolverParameters.IntegerParam.INCREMENTALITY, 1)
        return mpSolver.solve(params)
    }
}
