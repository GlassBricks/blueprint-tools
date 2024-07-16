package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.SignalID
import glassbricks.factorio.blueprint.json.*
import glassbricks.factorio.blueprint.prototypes.RoboportPrototype

public class Roboport(
    override val prototype: RoboportPrototype,
    json: EntityJson,
) : BaseEntity(json), CircuitConnectionPoint, WithControlBehavior {
    override val circuitConnections: CircuitConnections = CircuitConnections(this)
    public override val controlBehavior: RoboportControlBehavior =
        RoboportControlBehavior(prototype, json.control_behavior)

    override fun exportToJson(json: EntityJson) {
        if (this.shouldExportControlBehavior()) json.control_behavior = controlBehavior.exportToJson()
    }

    override fun copyIsolated(): Roboport = Roboport(prototype, toDummyJson())
}

public class RoboportControlBehavior(
    private val prototype: RoboportPrototype,
    json: ControlBehaviorJson?
) : ControlBehavior {
    public var readLogistics: Boolean = json?.read_logistics ?: true
    public var readRobotStats: Boolean = json?.read_robot_stats ?: false

    public val defaultAvailableLogisticSignal: SignalID? get() = prototype.default_available_logistic_output_signal
    public val defaultTotalLogisticSignal: SignalID? get() = prototype.default_total_logistic_output_signal
    public val defaultAvailableConstructionSignal: SignalID? get() = prototype.default_available_construction_output_signal
    public val defaultTotalConstructionSignal: SignalID? get() = prototype.default_total_construction_output_signal

    public var availableLogisticOutputSignal: SignalID? =
        (json?.available_logistic_output_signal).toSignalIdWithDefault(defaultAvailableLogisticSignal)
    public var totalLogisticOutputSignal: SignalID? =
        (json?.total_logistic_output_signal).toSignalIdWithDefault(defaultTotalLogisticSignal)
    public var availableConstructionOutputSignal: SignalID? =
        (json?.available_construction_output_signal).toSignalIdWithDefault(defaultAvailableConstructionSignal)
    public var totalConstructionOutputSignal: SignalID? =
        (json?.total_construction_output_signal).toSignalIdWithDefault(defaultTotalConstructionSignal)

    override fun exportToJson(): ControlBehaviorJson? {
        val availableLogi =
            availableLogisticOutputSignal.toJsonWithDefault(defaultAvailableLogisticSignal)
        val totalLogi = totalLogisticOutputSignal.toJsonWithDefault(defaultTotalLogisticSignal)
        val availableConstr = availableConstructionOutputSignal.toJsonWithDefault(defaultAvailableConstructionSignal)
        val totalConstr = totalConstructionOutputSignal.toJsonWithDefault(defaultTotalConstructionSignal)
        if (readLogistics && !readRobotStats &&
            availableLogi == null &&
            totalLogi == null &&
            availableConstr == null &&
            totalConstr == null
        ) {
            return null
        }

        return ControlBehaviorJson(
            read_logistics = readLogistics,
            read_robot_stats = readRobotStats,
            available_logistic_output_signal = availableLogi,
            total_logistic_output_signal = totalLogi,
            available_construction_output_signal = availableConstr,
            total_construction_output_signal = totalConstr,
        )
    }
}
