package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.SignalID
import glassbricks.factorio.blueprint.json.*
import glassbricks.factorio.blueprint.prototypes.ArithmeticCombinatorPrototype
import glassbricks.factorio.blueprint.prototypes.CombinatorPrototype
import glassbricks.factorio.blueprint.prototypes.ConstantCombinatorPrototype
import glassbricks.factorio.blueprint.prototypes.DeciderCombinatorPrototype
import glassbricks.factorio.blueprint.json.ConstantCombinatorParameters as ConstantCombinatorParametersJson

/**
 * Represents either an [ArithmeticCombinator], [DeciderCombinator].
 *
 * [ConstantCombinator]s are _not_ a subclass of this.
 */
public sealed class Combinator(
    json: EntityJson,
) : BaseEntity(json), CombinatorConnections, WithControlBehavior {
    override val input: CircuitConnectionPoint = ConnectionPoint(CircuitID.First)
    override val output: CircuitConnectionPoint = ConnectionPoint(CircuitID.Second)

    abstract override val prototype: CombinatorPrototype

    abstract override fun copyIsolated(): Combinator

    private inner class ConnectionPoint(override val circuitID: CircuitID) : CircuitConnectionPoint {
        override val entity: Entity get() = this@Combinator
        override val circuitConnections: CircuitConnections = CircuitConnections(this)
    }
}

public class ArithmeticCombinator(
    override val prototype: ArithmeticCombinatorPrototype,
    json: EntityJson,
) : Combinator(json) {
    override val controlBehavior: ArithmeticCombinatorControlBehavior =
        ArithmeticCombinatorControlBehavior(json.control_behavior)

    override fun exportToJson(json: EntityJson) {
        json.control_behavior = controlBehavior.exportToJson()
    }

    override fun copyIsolated(): ArithmeticCombinator = ArithmeticCombinator(prototype, toDummyJson())
}

public class ArithmeticCombinatorControlBehavior(
    source: ControlBehaviorJson? = null,
) : ControlBehavior {
    public var parameters: ArithmeticCombinatorParameters = source?.arithmetic_conditions
        ?: ArithmeticCombinatorParameters.DEFAULT

    override fun exportToJson(): ControlBehaviorJson =
        ControlBehaviorJson(arithmetic_conditions = parameters)
}

public class DeciderCombinator(
    override val prototype: DeciderCombinatorPrototype,
    json: EntityJson,
) : Combinator(json) {
    override val controlBehavior: DeciderCombinatorControlBehavior =
        DeciderCombinatorControlBehavior(json.control_behavior)

    override fun exportToJson(json: EntityJson) {
        json.control_behavior = controlBehavior.exportToJson()
    }

    override fun copyIsolated(): DeciderCombinator = DeciderCombinator(prototype, toDummyJson())
}

public class DeciderCombinatorControlBehavior(
    source: ControlBehaviorJson? = null,
) : ControlBehavior {
    public var parameters: DeciderCombinatorParameters = source?.decider_conditions
        ?: DeciderCombinatorParameters.DEFAULT

    override fun exportToJson(): ControlBehaviorJson =
        ControlBehaviorJson(decider_conditions = parameters)
}

public class ConstantCombinator(
    override val prototype: ConstantCombinatorPrototype,
    json: EntityJson,
) : BaseEntity(json), WithControlBehavior, CircuitConnectionPoint {
    override val circuitConnections: CircuitConnections = CircuitConnections(this)
    override val controlBehavior: ConstantCombinatorControlBehavior =
        ConstantCombinatorControlBehavior(prototype, json.control_behavior)

    override fun exportToJson(json: EntityJson) {
        json.control_behavior = controlBehavior.exportToJson()
    }

    override fun copyIsolated(): ConstantCombinator = ConstantCombinator(prototype, toDummyJson())
}

public class ConstantCombinatorControlBehavior(
    prototype: ConstantCombinatorPrototype,
    source: ControlBehaviorJson? = null,
) : ControlBehavior {
    public var isOn: Boolean = source?.is_on ?: true
    public var parameters: Array<ConstantCombinatorParameter?> =
        source?.filters.toArray(prototype.item_slot_count.toInt())

    override fun exportToJson(): ControlBehaviorJson? {
        if (isOn && parameters.none { it != null }) {
            return null
        }
        return ControlBehaviorJson(
            is_on = isOn,
            filters = parameters.toIndexList(),
        )
    }
}

public class ConstantCombinatorParameter(
    public val signal: SignalID,
    public val count: Int,
)

private fun List<ConstantCombinatorParametersJson>?.toArray(size: Int): Array<ConstantCombinatorParameter?> =
    indexListToArray(size, { it.index }) {
        it.signal.toSignalIDBasic()?.let { signal -> ConstantCombinatorParameter(signal, it.count) }
    }

private fun Array<out ConstantCombinatorParameter?>.toIndexList(): List<ConstantCombinatorParametersJson> =
    arrayToIndexList { index, item ->
        ConstantCombinatorParametersJson(index = index, signal = item.signal.toJsonBasic(), count = item.count)
    }
