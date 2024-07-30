package glassbricks.factorio.blueprint.placement

import com.google.ortools.sat.Constraint
import com.google.ortools.sat.CpModel
import com.google.ortools.sat.LinearExpr
import com.google.ortools.sat.Literal as CPLiteral


interface WithCp {
    val cp: CpModel
}

interface Literal : CPLiteral, Conjunction, Disjunction {
    override operator fun not(): Literal
    override val literals: List<Literal> get() = listOf(this)
    override val conjunctsToOr: List<Conjunction> get() = listOf(this)
    override val disjunctsToAnd: List<Disjunction> get() = listOf(this)
}

class LiteralWrapper(private val literal: CPLiteral) : Literal {
    override fun build(): LinearExpr = literal.build()
    override fun getIndex(): Int = literal.getIndex()
    private var not: Literal? = null
    override fun not(): Literal = not ?: NotLiteral().also { this.not = it }
    private inner class NotLiteral : Literal {
        override fun build(): LinearExpr = literal.not().build()
        override fun getIndex(): Int = literal.not().index
        override fun not(): Literal = this@LiteralWrapper
    }
}

fun CPLiteral.asLit(): Literal = if (this is Literal) this else LiteralWrapper(this)

/**
 * A conjunction (AND) of literals.
 */
interface Conjunction : DNF, CNF {
    val literals: Iterable<Literal>

    override val conjunctsToOr: Iterable<Conjunction> get() = listOf(this)
    override val disjunctsToAnd: Iterable<Disjunction> get() = literals

    operator fun not(): Disjunction = Disjunction(literals.map { !it })
}

/** A disjunction (OR) of [Conjunction]s. */
interface DNF {
    val conjunctsToOr: Iterable<Conjunction>
}

/** A disjunction (OR) of literals. */
interface Disjunction : DNF, CNF {
    val literals: Iterable<Literal>

    override val conjunctsToOr: Iterable<Conjunction> get() = literals
    override val disjunctsToAnd: Iterable<Disjunction> get() = listOf(this)

    operator fun not(): Conjunction = Conjunction(literals.map { !it })
}

/** A conjunction (AND) of [Disjunction]s. */
interface CNF {
    val disjunctsToAnd: Iterable<Disjunction>
}


fun Conjunction(literals: Iterable<Literal>): Conjunction = object : Conjunction {
    override val literals: Iterable<Literal> = literals
}

fun Disjunction(literals: Iterable<Literal>): Disjunction = object : Disjunction {
    override val literals: Iterable<Literal> = literals
}

fun DNF(disjuncts: Iterable<Conjunction>): DNF = object : DNF {
    override val conjunctsToOr: Iterable<Conjunction> = disjuncts
}

fun CNF(conjuncts: Iterable<Disjunction>): CNF = object : CNF {
    override val disjunctsToAnd: Iterable<Disjunction> = conjuncts
}

private fun <T> Iterable<T>.asCollection(): Collection<T> = when (this) {
    is Collection -> this
    else -> toList()
}

context(WithCp)
fun DNF.implies(conjunction: Conjunction) {
    for (disjunct in this.conjunctsToOr) {
        val constraint = cp.addBoolAndLenient(conjunction.literals.asCollection())
        for (literal in disjunct.literals) {
            constraint.onlyEnforceIf(literal)
        }
    }
}

// Uses quadratic constraints instead of Tseitin transformation, for better propagation?
context(WithCp)
fun DNF.implies(cnf: CNF) {
    if (cnf is Conjunction) {
        return implies(cnf)
    }
    // (a & b) | (c & d) => (e | f) & (g | h)
    // ((!a | !b) & (!c | !d)) | (e | f) & (g | h)
    // (!a | !b | e | f) & (!c | !d | e | f) & ...
    for (disjunct in this.conjunctsToOr) {
        val invLiterals = disjunct.literals.map { !it }
        for (conjunct in cnf.disjunctsToAnd) {
            cp.addBoolOr(invLiterals + conjunct.literals)
        }
    }
}

context(WithCp)
fun DNF.impliesAll(expressions: Iterable<CNF>) {
    for (cnf in expressions) implies(cnf)
}

context(WithCp)
fun DNF.impliesAll(vararg expressions: CNF) {
    for (expression in expressions) implies(expression)
}

fun any(literals: Iterable<Literal>): Disjunction = Disjunction(literals)
fun all(literals: Iterable<Literal>): Conjunction = Conjunction(literals)
fun none(literals: Iterable<Literal>): Conjunction = all(literals.map { !it })

fun any(disjunctions: Iterable<Disjunction>): Disjunction = Disjunction(disjunctions.flatMap { it.literals })
fun all(disjunctions: Iterable<Disjunction>): CNF = CNF(disjunctions)
fun none(disjunctions: Iterable<Disjunction>): Conjunction = all(disjunctions.map { !it })

fun any(conjunctions: Iterable<Conjunction>): DNF = DNF(conjunctions)
fun all(conjunctions: Iterable<Conjunction>): Conjunction = Conjunction(conjunctions.flatMap { it.literals })
fun none(conjunctions: Iterable<Conjunction>): Disjunction = any(conjunctions.map { !it })

fun any(vararg literals: Literal): Disjunction = any(literals.asList())
fun all(vararg literals: Literal): Conjunction = all(literals.asList())
fun none(vararg literals: Literal): Conjunction = none(literals.asList())
fun any(vararg disjunctions: Disjunction): Disjunction = any(disjunctions.asList())
fun all(vararg disjunctions: Disjunction): CNF = all(disjunctions.asList())
fun none(vararg disjunctions: Disjunction): Conjunction = none(disjunctions.asList())
fun any(vararg conjunctions: Conjunction): DNF = any(conjunctions.asList())
fun all(vararg conjunctions: Conjunction): Conjunction = all(conjunctions.asList())
fun none(vararg conjunctions: Conjunction): Disjunction = none(conjunctions.asList())

fun CpModel.addExactlyOneLenient(literals: Collection<CPLiteral>): Constraint = when (literals.size) {
    0 -> error("addExactlyOneLenient called with no literals, impossible to satisfy")
    1 -> addEquality(literals.first(), 1L)
    else -> addExactlyOne(literals)
}

fun CpModel.addBoolAndLenient(
    literals: Collection<CPLiteral>,
    onlyIf: CPLiteral
): Constraint = when (literals.size) {
    0 -> addEquality(onlyIf, 0L)
    1 -> addImplication(onlyIf, literals.first())
    else -> addBoolAnd(literals).apply { onlyEnforceIf(onlyIf) }
}

fun CpModel.addBoolAndLenient(literals: Collection<CPLiteral>): Constraint = when (literals.size) {
    0 -> error("addBoolAndLenient called with no literals, impossible to satisfy")
    1 -> addBoolOr(literals)
    else -> addBoolAnd(literals)
}
