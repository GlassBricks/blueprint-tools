package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.SignalID
import glassbricks.factorio.blueprint.json.toJsonBasic
import glassbricks.factorio.blueprint.json.toSignalIDBasic
import glassbricks.factorio.blueprint.prototypes.RoboportPrototype

public class Roboport(
    override val prototype: RoboportPrototype,
    json: EntityJson,
) : BaseEntity(json), CircuitConnectionPoint, WithControlBehavior {
    override val circuitConnections: CircuitConnections = CircuitConnections(this)
    public override val controlBehavior: RoboportControlBehavior = RoboportControlBehavior(json.control_behavior)

    override fun exportToJson(json: EntityJson) {
        if (this.hasCircuitConnections()) json.control_behavior = controlBehavior.exportToJson()
    }
}

public class RoboportControlBehavior(json: ControlBehaviorJson?) : ControlBehavior {
    public var readLogistics: Boolean = json?.read_logistics ?: true
    public var readRobotStats: Boolean = json?.read_robot_stats ?: false
    public var availableLogisticOutputSignal: SignalID? = json?.available_logistic_output_signal?.toSignalIDBasic()
    public var totalLogisticOutputSignal: SignalID? = json?.total_logistic_output_signal?.toSignalIDBasic()
    public var availableConstructionOutputSignal: SignalID? = json?.available_construction_output_signal?.toSignalIDBasic()
    public var totalConstructionOutputSignal: SignalID? = json?.total_construction_output_signal?.toSignalIDBasic()

    override fun exportToJson(): ControlBehaviorJson? {
        if (readLogistics && !readRobotStats &&
            availableLogisticOutputSignal == null &&
            totalLogisticOutputSignal == null &&
            availableConstructionOutputSignal == null &&
            totalConstructionOutputSignal == null
        ) {
            return null
        }

        return ControlBehaviorJson(
            read_logistics = readLogistics,
            read_robot_stats = readRobotStats,
            available_logistic_output_signal = availableLogisticOutputSignal.toJsonBasic(),
            total_logistic_output_signal = totalLogisticOutputSignal.toJsonBasic(),
            available_construction_output_signal = availableConstructionOutputSignal.toJsonBasic(),
            total_construction_output_signal = totalConstructionOutputSignal.toJsonBasic()
        )
    }
}
