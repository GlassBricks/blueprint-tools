package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.ArithmeticCombinatorParameters
import glassbricks.factorio.blueprint.json.CircuitID
import glassbricks.factorio.blueprint.json.DeciderCombinatorParameters
import glassbricks.factorio.blueprint.json.SignalID
import glassbricks.factorio.blueprint.prototypes.ArithmeticCombinatorPrototype
import glassbricks.factorio.blueprint.prototypes.ConstantCombinatorPrototype
import glassbricks.factorio.blueprint.prototypes.DeciderCombinatorPrototype
import glassbricks.factorio.blueprint.json.ConstantCombinatorParameters as ConstantCombinatorParametersJson

public sealed class Combinator(
    json: EntityJson,
) : BaseEntity(json), CombinatorConnections, WithControlBehavior {
    override val input: CircuitConnectionPoint = ConnectionPoint(CircuitID.First)
    override val output: CircuitConnectionPoint = ConnectionPoint(CircuitID.Second)

    private inner class ConnectionPoint(
        override val circuitID: CircuitID,
    ) : CircuitConnectionPoint {
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
}

public class ArithmeticCombinatorControlBehavior(
    source: ControlBehaviorJson? = null,
) : GenericOnOffControlBehavior(source), ControlBehavior {
    public var parameters: ArithmeticCombinatorParameters? = source?.arithmetic_conditions

    override fun exportToJson(): ControlBehaviorJson = baseExportToJson().apply {
        arithmetic_conditions = parameters
    }
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
}

public class DeciderCombinatorControlBehavior(
    source: ControlBehaviorJson? = null,
) : ControlBehavior {
    public var parameters: DeciderCombinatorParameters? = source?.decider_conditions

    override fun exportToJson(): ControlBehaviorJson = ControlBehaviorJson(decider_conditions = parameters)
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
}

public class ConstantCombinatorControlBehavior(
    prototype: ConstantCombinatorPrototype,
    source: ControlBehaviorJson? = null,
) : ControlBehavior {
    public var isOn: Boolean = source?.is_on ?: true
    public var parameters: Array<ConstantCombinatorParameter?> =
        source?.filters.toParameters(prototype.item_slot_count.toInt())

    override fun exportToJson(): ControlBehaviorJson? {
        if (isOn && parameters.none { it != null }) {
            return null
        }
        return ControlBehaviorJson(
            is_on = isOn,
            filters = parameters.arrayToIndexedList(),
        )

    }
}

public class ConstantCombinatorParameter(
    public val signal: SignalID,
    public val count: Int,
)

private fun List<ConstantCombinatorParametersJson>?.toParameters(size: Int): Array<ConstantCombinatorParameter?> =
    indexedToArray(size, { it.index }) { ConstantCombinatorParameter(it.signal, it.count) }

private fun Array<out ConstantCombinatorParameter?>.arrayToIndexedList(): List<ConstantCombinatorParametersJson> =
    arrayToIndexedList { index, item ->
        ConstantCombinatorParametersJson(index = index, signal = item.signal, count = item.count)
    }
