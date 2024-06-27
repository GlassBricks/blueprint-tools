package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.Direction
import glassbricks.factorio.blueprint.json.EntityNumber
import glassbricks.factorio.blueprint.json.Position
import glassbricks.factorio.prototypes.EntityWithOwnerPrototype
import kotlinx.serialization.json.JsonObject

public interface EntityProps {
    public val name: String
    public val position: Position
    public val direction: Direction
    public val tags: JsonObject?
}

public interface Entity : EntityProps {
    public val prototype: EntityWithOwnerPrototype
    public val type: String get() = prototype.type

    public override val name: String get() = prototype.name
    public override var position: Position
    public override var direction: Direction
    public override var tags: JsonObject?

    public fun toJsonIsolated(entityNumber: EntityNumber): EntityJson

    public fun copy(): Entity
}

internal fun EntityJson.deepCopy() = copy() // todo: actually deep copy
