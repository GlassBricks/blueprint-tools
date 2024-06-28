package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.IOType
import glassbricks.factorio.blueprint.json.SplitterPriority
import glassbricks.factorio.blueprint.json.TransportBeltContentReadMode
import glassbricks.factorio.prototypes.*


public sealed interface TransportBeltConnectable : Entity {
    override val prototype: TransportBeltConnectablePrototype
}

public class TransportBelt(
    override val prototype: TransportBeltPrototype,
    json: EntityJson,
) : BaseEntity(json), TransportBeltConnectable, CircuitConnectable {
    override val connectionPoint1: CircuitConnectionPoint = CircuitConnectionPoint(this)
    public val controlBehavior: TransportBeltControlBehavior = TransportBeltControlBehavior(json.control_behavior)

    override fun exportToJson(json: EntityJson) {
        if(this.hasCircuitConnections()) json.control_behavior = controlBehavior.exportToJson()
    }
}

public class TransportBeltControlBehavior(
    source: ControlBehaviorJson? = null,
) : GenericOnOffControlBehavior(source), ControlBehavior {
    public val enableDisable: Boolean get() = circuitCondition != null

    /** If null, sets circuit_contents_read_mode to false. */
    public var readContentsMode: TransportBeltContentReadMode? =
        source?.circuit_contents_read_mode
            ?.takeIf { source.circuit_read_hand_contents == true }

    public val readHandContents: Boolean get() = readContentsMode != null

    override fun exportToJson(): ControlBehaviorJson = baseExportToJson().apply {
        circuit_enable_disable = enableDisable
        circuit_read_hand_contents = readContentsMode != null
        circuit_contents_read_mode = readContentsMode
    }
}

public class UndergroundBelt(
    override val prototype: UndergroundBeltPrototype,
    json: EntityJson,
) : BaseEntity(json), TransportBeltConnectable {
    public var ioMode: IOType = json.type ?: IOType.Input
    override fun exportToJson(json: EntityJson) {
        json.type = ioMode
    }
}

public class Splitter(
    override val prototype: SplitterPrototype,
    json: EntityJson,
) : BaseEntity(json), TransportBeltConnectable {
    public var inputPriority: SplitterPriority? = json.input_priority
    public var outputPriority: SplitterPriority? = json.output_priority
    public var filter: String? = json.filter

    override fun exportToJson(json: EntityJson) {
        json.input_priority = inputPriority
        json.output_priority = outputPriority
        json.filter = filter
    }
}

public class Loader(
    override val prototype: LoaderPrototype,
    json: EntityJson,
) : BaseEntity(json),
    TransportBeltConnectable, WithItemFilters {
    public override val filters: Array<String?> = json.filters.toFilters(prototype.filter_count.toInt())
    public var ioType: IOType = json.type ?: IOType.Input

    override fun exportToJson(json: EntityJson) {
        json.filters = getFiltersAsList()
        json.type = ioType
    }
}
