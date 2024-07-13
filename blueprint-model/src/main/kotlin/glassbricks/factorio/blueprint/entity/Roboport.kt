package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.SignalID
import glassbricks.factorio.blueprint.prototypes.RoboportPrototype

public class Roboport(
    override val prototype: RoboportPrototype,
    json: EntityJson,
) : BaseEntity(json), CircuitConnectable {
    override val connectionPoint1: CircuitConnectionPoint = CircuitConnectionPoint(this)
    public override val controlBehavior: RoboportControlBehavior = RoboportControlBehavior(json.control_behavior)

    override fun exportToJson(json: EntityJson) {
        if (this.hasCircuitConnections()) json.control_behavior = controlBehavior.exportToJson()
    }
}

public class RoboportControlBehavior(json: ControlBehaviorJson?) : ControlBehavior {
    public var readLogistics: Boolean = json?.read_logistics ?: true
    public var readRobotStats: Boolean = json?.read_robot_stats ?: false
    public var availableLogisticOutputSignal: SignalID? = json?.available_logistic_output_signal
    public var totalLogisticOutputSignal: SignalID? = json?.total_logistic_output_signal
    public var availableConstructionOutputSignal: SignalID? = json?.available_construction_output_signal
    public var totalConstructionOutputSignal: SignalID? = json?.total_construction_output_signal

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
            available_logistic_output_signal = availableLogisticOutputSignal,
            total_logistic_output_signal = totalLogisticOutputSignal,
            available_construction_output_signal = availableConstructionOutputSignal,
            total_construction_output_signal = totalConstructionOutputSignal
        )
    }
}
