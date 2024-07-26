package glassbricks.factorio.blueprint.placement

import com.google.ortools.sat.Constraint
import com.google.ortools.sat.CpModel
import com.google.ortools.sat.Literal


public fun CpModel.addExactlyOneLenient(literals: Collection<Literal>): Constraint = when (literals.size) {
    0 -> error("addExactlyOneLenient called with no literals, impossible to satisfy")
    1 -> addEquality(literals.first(), 1L)
    else -> addExactlyOne(literals)
}

public fun CpModel.addExactlyOneLenient(
    literals: Collection<Literal>,
    onlyIf: Literal
): Constraint = when (literals.size) {
    0 -> addEquality(onlyIf, 0L)
    1 -> addImplication(onlyIf, literals.first())
    else -> addExactlyOne(literals).apply { onlyEnforceIf(onlyIf) }
}


public fun CpModel.addBoolAndLenient(literals: Collection<Literal>): Constraint =
    when (literals.size) {
        0 -> error("addBoolAnd called with no literals, impossible to satisfy")
        1 -> addEquality(literals.first(), 1L)
        else -> addBoolAnd(literals)
    }

public fun CpModel.addBoolAndLenient(
    literals: Collection<Literal>,
    onlyIf: Literal
): Constraint = when (literals.size) {
    0 -> addEquality(onlyIf, 0L)
    1 -> addImplication(onlyIf, literals.first())
    else -> addBoolAnd(literals).apply { onlyEnforceIf(onlyIf) }
}