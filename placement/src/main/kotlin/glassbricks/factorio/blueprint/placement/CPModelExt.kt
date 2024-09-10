package glassbricks.factorio.blueprint.placement

import com.google.ortools.sat.BoolVar
import com.google.ortools.sat.Constraint
import com.google.ortools.sat.CpModel
import com.google.ortools.sat.DoubleLinearExpr
import com.google.ortools.sat.LinearExpr
import com.google.ortools.sat.Literal
import com.google.ortools.sat.NotBoolVar

fun CpModel.addExactlyOneLenient(literals: Collection<Literal>): Constraint = when (literals.size) {
    0 -> error("addExactlyOneLenient called with no literals, impossible to satisfy")
    1 -> addEquality(literals.first(), 1L)
    else -> addExactlyOne(literals)
}

fun CpModel.addBoolAndLenient(
    literals: Collection<Literal>,
    onlyIf: Literal,
): Constraint = when (literals.size) {
    0 -> addEquality(onlyIf, 0L)
    1 -> addImplication(onlyIf, literals.first())
    else -> addBoolAnd(literals).apply { onlyEnforceIf(onlyIf) }
}

fun CpModel.addBoolAndLenient(literals: Collection<Literal>): Constraint = when (literals.size) {
    0 -> error("addBoolAndLenient called with no literals, impossible to satisfy")
    1 -> addBoolOr(literals)
    else -> addBoolAnd(literals)
}

fun intLinearExpr(values: Map<Literal, Number>): LinearExpr = LinearExpr.newBuilder().apply {
    for ((literal, value) in values) {
        addTerm(literal, value.toLong())
    }
}.build()

fun doubleLinearExpr(values: Map<Literal, Double>): DoubleLinearExpr {
    val terms = arrayOfNulls<Literal>(values.size)
    val coefficients = DoubleArray(values.size)
    for ((i, entry) in values.entries.withIndex()) {
        val (literal, value) = entry
        terms[i] = literal
        coefficients[i] = value
    }
    return DoubleLinearExpr(terms.requireNoNulls(), coefficients, 0.0)
}

fun CpModel.addLiteralHint(literal: Literal, value: Boolean) {
    when (literal) {
        is BoolVar -> addHint(literal, if (value) 1 else 0)
        is NotBoolVar -> addHint(literal.not() as BoolVar, if (value) 0 else 1)
    }
}

fun CpModel.addLiteralEquality(literal: Literal, value: Boolean): Constraint {
    return this.addEquality(literal, if (value) 1L else 0L)
}
