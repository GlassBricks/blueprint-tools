package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.IOType
import glassbricks.factorio.blueprint.json.SplitterPriority
import glassbricks.factorio.blueprint.json.TransportBeltContentReadMode
import glassbricks.factorio.prototypes.*


public sealed interface TransportBeltConnectable : Entity {
    override val prototype: TransportBeltConnectablePrototype
    override fun copy(): TransportBeltConnectable
}

public class TransportBelt
internal constructor(
    override val prototype: TransportBeltPrototype,
    init: EntityInit<TransportBelt>,
) : BaseEntity(init), TransportBeltConnectable, CircuitConnectable {
    override val connectionPoint1: CircuitConnectionPoint = CircuitConnectionPoint(this)
    public val controlBehavior: TransportBeltControlBehavior =
        init.self?.controlBehavior?.copy() ?: TransportBeltControlBehavior(init.json?.control_behavior)

    override fun exportToJson(json: EntityJson) {
        json.control_behavior = controlBehavior.exportToJson()
    }

    override fun copy(): TransportBelt = TransportBelt(prototype, copyInit(this))
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

    override fun exportToJson(): ControlBehaviorJson? {
        val superExport = super.exportToJson()
        if(superExport == null && readContentsMode == null) return null
        return (superExport ?: ControlBehaviorJson()).apply { 
            circuit_enable_disable = enableDisable
            circuit_read_hand_contents = readContentsMode != null
            circuit_contents_read_mode = readContentsMode
        }
    }

    override fun copy(): TransportBeltControlBehavior = TransportBeltControlBehavior().also { 
        copyTo(it)
        it.readContentsMode = readContentsMode
    }
}

public class UndergroundBelt
internal constructor(
    override val prototype: UndergroundBeltPrototype,
    init: EntityInit<UndergroundBelt>,
) : BaseEntity(init), TransportBeltConnectable {
    public var ioMode: IOType = init.self?.ioMode ?: init.json?.type ?: IOType.Input
    override fun exportToJson(json: EntityJson) {
        json.type = ioMode
    }

    override fun copy(): UndergroundBelt = UndergroundBelt(prototype, copyInit(this))
}

public class Splitter
internal constructor(
    override val prototype: SplitterPrototype,
    init: EntityInit<Splitter>,
) : BaseEntity(init), TransportBeltConnectable {
    public var inputPriority: SplitterPriority? =
        init.self?.inputPriority ?: init.json?.input_priority
    public var outputPriority: SplitterPriority? =
        init.self?.outputPriority ?: init.json?.output_priority
    public var filter: String? = init.self?.filter ?: init.json?.filter
    
    override fun exportToJson(json: EntityJson) {
        json.input_priority = inputPriority
        json.output_priority = outputPriority
        json.filter = filter
    }

    override fun copy(): Splitter = Splitter(prototype, copyInit(this))
}

public class Loader
internal constructor(
    override val prototype: LoaderPrototype,
    init: EntityInit<Loader>,
) : BaseEntity(init),
    TransportBeltConnectable, WithItemFilters {
    public override val filters: Array<String?> = init.getDirectFilters(prototype.filter_count.toInt())
    public var ioType: IOType = init.self?.ioType ?: init.json?.type ?: IOType.Input

    override fun exportToJson(json: EntityJson) {
        json.filters = getFiltersAsList()
        json.type = ioType
    }

    override fun copy(): Loader = Loader(prototype, copyInit(this))
}
