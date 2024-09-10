package glassbricks.factorio.blueprint.placement.belt

import glassbricks.factorio.blueprint.SpatialDataStructure
import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.entity.BlueprintEntity
import glassbricks.factorio.blueprint.entity.UndergroundBelt
import glassbricks.factorio.blueprint.entity.WithIoType
import glassbricks.factorio.blueprint.entity.copyWithOldConnections
import glassbricks.factorio.blueprint.entity.createBpEntity
import glassbricks.factorio.blueprint.entity.placedAtTile
import glassbricks.factorio.blueprint.json.IOType
import glassbricks.factorio.blueprint.placement.CardinalDirection
import glassbricks.factorio.blueprint.placement.EntityPlacementModel
import glassbricks.factorio.blueprint.placement.OptionalEntityPlacement
import glassbricks.factorio.blueprint.placement.to8wayDirection
import glassbricks.factorio.blueprint.prototypes.BlueprintPrototypes
import glassbricks.factorio.blueprint.prototypes.TransportBeltConnectablePrototype
import glassbricks.factorio.blueprint.prototypes.TransportBeltPrototype
import glassbricks.factorio.blueprint.prototypes.UndergroundBeltPrototype


data class BeltTier(
    val beltProto: TransportBeltPrototype,
    val ugProto: UndergroundBeltPrototype,
) : Comparable<BeltTier> {
    val belt: BeltType.Belt = BeltType.Belt(beltProto)
    val inputUg: BeltType.InputUnderground = BeltType.InputUnderground(ugProto)
    val outputUg: BeltType.OutputUnderground = BeltType.OutputUnderground(ugProto)
    override fun compareTo(other: BeltTier): Int = beltProto.speed.compareTo(other.beltProto.speed)
    override fun toString(): String = "BeltTier(${beltProto.name}, ${ugProto.name})"
}

fun BlueprintPrototypes.getBeltTier(belt: TransportBeltPrototype): BeltTier? {
    val ugId = belt.related_underground_belt ?: return null
    val ug = this[ugId] as? UndergroundBeltPrototype ?: return null
    return BeltTier(belt, ug)
}

fun BlueprintPrototypes.getBeltTier(beltName: String): BeltTier? =
    this.dataRaw.`transport-belt`[beltName]?.let { getBeltTier(it) }

fun BlueprintPrototypes.getAllBeltTiers(): Map<TransportBeltConnectablePrototype, BeltTier> {
    val beltTiers = dataRaw.`transport-belt`.values.mapNotNull { getBeltTier(it) }
    return buildMap {
        for (tier in beltTiers) {
            put(tier.beltProto, tier)
            put(tier.ugProto, tier)
        }
    }
}

sealed interface BeltType {
    override fun toString(): String
    val hasOutput: Boolean
    val hasInput: Boolean
    val prototype: TransportBeltConnectablePrototype

    sealed interface Underground : BeltType {
        override val prototype: UndergroundBeltPrototype
        val isIsolated: Boolean
        val ioType: IOType
        fun oppositeNotIsolated(): Underground?
        fun opposite(isolated: Boolean): Underground
    }

    data class Belt(override val prototype: TransportBeltPrototype) : BeltType {
        override fun toString(): String = "<${prototype.name}>"
        override val hasInput: Boolean get() = true
        override val hasOutput: Boolean get() = true
    }

    data class InputUnderground(
        override val prototype: UndergroundBeltPrototype,
        override val isIsolated: Boolean = false,
    ) : Underground {
        override fun toString(): String = "<${prototype.name} in${if (isIsolated) " isolated" else ""}>"
        override val hasInput: Boolean get() = true
        override val hasOutput: Boolean get() = false
        override val ioType: IOType get() = IOType.Input

        override fun oppositeNotIsolated(): OutputUnderground? = if (!isIsolated) OutputUnderground(prototype) else null
        override fun opposite(isolated: Boolean): OutputUnderground = OutputUnderground(prototype, isolated)
    }

    data class OutputUnderground(
        override val prototype: UndergroundBeltPrototype,
        override val isIsolated: Boolean = false,
    ) : Underground {

        override fun toString(): String = "<${prototype.name} out${if (isIsolated) " isolated" else ""}>"
        override val hasInput: Boolean get() = false
        override val hasOutput: Boolean get() = true
        override val ioType: IOType get() = IOType.Output

        override fun oppositeNotIsolated(): InputUnderground? = if (!isIsolated) InputUnderground(prototype) else null
        override fun opposite(isolated: Boolean): InputUnderground = InputUnderground(prototype, isolated)
    }
}

fun BeltType.toBlueprintEntity(
    position: TilePosition,
    direction: CardinalDirection,
    entitiesToCopyFrom: SpatialDataStructure<BlueprintEntity>? = null,
): BlueprintEntity {
    if (entitiesToCopyFrom != null) {
        val existing = findMatchingEntity(this, entitiesToCopyFrom, position, direction)
        if (existing != null) return existing.copyWithOldConnections()
    }
    val entity = prototype.placedAtTile(position, direction.to8wayDirection())
    if (this is BeltType.Underground) {
        entity as UndergroundBelt
        entity.ioType = ioType
    }
    return entity
}

private fun findMatchingEntity(
    beltType: BeltType,
    entitiesToCopyFrom: SpatialDataStructure<BlueprintEntity>,
    position: TilePosition,
    direction: CardinalDirection,
): BlueprintEntity? {
    val ioType = (beltType as? BeltType.Underground)?.ioType
    val existing = entitiesToCopyFrom.getInTile(position).find {
        it.prototype == beltType.prototype && it.direction == direction.to8wayDirection()
                && (it as? WithIoType)?.ioType == ioType
    }
    return existing
}

fun EntityPlacementModel.createBeltPlacement(
    type: BeltType,
    position: TilePosition,
    direction: CardinalDirection,
): OptionalEntityPlacement<*> = when (type) {
    is BeltType.Belt -> addPlacement(type.prototype, position.tileCenter(), direction.to8wayDirection())
    is BeltType.Underground -> {
        val bpEntity =
            createBpEntity(type.prototype, position.tileCenter(), direction.to8wayDirection()) as UndergroundBelt
        bpEntity.ioType = type.ioType
        addPlacement(bpEntity)
    }
}
