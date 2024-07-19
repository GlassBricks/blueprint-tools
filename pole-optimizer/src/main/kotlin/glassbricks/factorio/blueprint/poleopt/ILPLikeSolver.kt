package glassbricks.factorio.blueprint.poleopt

import com.google.ortools.linearsolver.MPSolver
import com.google.ortools.linearsolver.MPSolverParameters
import com.google.ortools.linearsolver.MPVariable
import com.google.ortools.sat.*
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.function.Consumer

private val logger = KotlinLogging.logger {}

public interface ILPLikeSolver {
    public fun addVariable(name: String): BoolVar

    // constraints
    public fun addDisjunction(literals: Iterable<Literal>)
    public fun addDisjunction(vararg literals: Literal): Unit = addDisjunction(literals.toList())

    public fun addImplies(
        condition: Iterable<Literal>,
        consequence: Iterable<Literal>
    ) {
        addDisjunction(condition.map { it.not() } + consequence)
    }

    public fun addAtMostOne(literals: Iterable<Literal>)

    public fun addMustBeTrue(literal: Literal): Unit

    // objective
    public fun setObjectiveMinimze(costs: Map<Literal, Double>)

    public interface Literal {
        public val variable: BoolVar
        public val isTrue: Boolean
        public operator fun not(): Literal
    }

    public interface BoolVar : Literal {
        override val variable: BoolVar get() = this
        override val isTrue: Boolean get() = true

        public fun solutionValue(): Boolean
    }

    public fun setTimeLimit(millis: Long)

    public fun solve(): Any
}

public class ILPSolver(
    public val mpSolver: MPSolver
) : ILPLikeSolver {
    override fun addVariable(name: String): ILPLikeSolver.BoolVar = Variable(mpSolver.makeBoolVar(name))

    private class Variable(val mpVariable: MPVariable) :
        ILPLikeSolver.BoolVar {
        override fun solutionValue(): Boolean = mpVariable.solutionValue() > 0.5
        override fun not(): ILPLikeSolver.Literal = NegatedLiteral(this)
    }

    private class NegatedLiteral(override val variable: Variable) : ILPLikeSolver.Literal {
        override val isTrue: Boolean get() = false
        override fun not(): ILPLikeSolver.Literal = variable
    }

    private fun ILPLikeSolver.Literal.coefValue(): Double = if (isTrue) 1.0 else -1.0

    override fun addDisjunction(literals: Iterable<ILPLikeSolver.Literal>) {
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

    override fun addAtMostOne(literals: Iterable<ILPLikeSolver.Literal>) {
        val numFalse = literals.count { !it.isTrue }
        val constraint = mpSolver.makeConstraint(Double.NEGATIVE_INFINITY, 1.0 - numFalse)
        for (literal in literals) {
            val mpVariable = (literal.variable as Variable).mpVariable
            constraint.setCoefficient(mpVariable, literal.coefValue())
        }
    }

    override fun setObjectiveMinimze(costs: Map<ILPLikeSolver.Literal, Double>) {
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

public class CPSolver(public val model: CpModel = CpModel()) : ILPLikeSolver {
    private val solver = CpSolver()
    override fun addVariable(name: String): ILPLikeSolver.BoolVar = Variable(model.newBoolVar(name))

    private interface Literal : ILPLikeSolver.Literal {
        val cpLiteral: com.google.ortools.sat.Literal
    }

    private inner class Variable(val cpVariable: BoolVar) : ILPLikeSolver.BoolVar, Literal {
        override fun not(): ILPLikeSolver.Literal = NegatedLiteral(this)
        override val cpLiteral get() = cpVariable
        override fun solutionValue(): Boolean = solver.booleanValue(cpVariable)
    }

    private class NegatedLiteral(override val variable: Variable) : Literal {
        override val isTrue: Boolean get() = false
        override fun not(): ILPLikeSolver.Literal = variable
        override val cpLiteral: com.google.ortools.sat.Literal get() = variable.cpVariable.not()
    }


    override fun addDisjunction(literals: Iterable<ILPLikeSolver.Literal>) {
        model.addBoolOr(literals.map { (it as Literal).cpLiteral })
    }

    override fun addAtMostOne(literals: Iterable<ILPLikeSolver.Literal>) {
        model.addAtMostOne(literals.map { (it as Literal).cpLiteral })
    }

    override fun addMustBeTrue(literal: ILPLikeSolver.Literal) {
        model.addBoolOr(listOf((literal as Literal).cpLiteral))
    }

    override fun setObjectiveMinimze(costs: Map<ILPLikeSolver.Literal, Double>) {
        val literals: Array<com.google.ortools.sat.Literal> =
            costs.keys.map { (it as Literal).cpLiteral }.toTypedArray()
        val coeffs = costs.values.toDoubleArray()
        val sum = DoubleLinearExpr(literals, coeffs, 0.0)
        model.minimize(sum)
    }


    override fun setTimeLimit(millis: Long) {
        solver.parameters.maxTimeInSeconds = millis / 1000.0
    }

    override fun solve(): Any {
        val toDisplay = listOf(
            "Wall time",
            "Obj. value",
            "Best bound",
        )
        println(toDisplay.joinToString("\t| ") { it.padEnd(11) })
        val callback = object : CpSolverSolutionCallback() {
            override fun onSolutionCallback() {
                if (logger.isInfoEnabled()) {
                    println(
                        arrayOf<Any>(
                            "%.2f".format(wallTime()),
                            objectiveValue(),
                            bestObjectiveBound(),
                        ).joinToString("\t| ") { it.toString().padEnd(11) }
                    )
                }
            }
        }
        return solver.solve(model, callback)
    }
}
