package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.EntityJson
import glassbricks.factorio.blueprint.prototypes.InventoryType
import glassbricks.factorio.blueprint.prototypes.ItemStackIndex
import glassbricks.factorio.blueprint.prototypes.LinkedContainerPrototype

public class LinkedContainer(
    override val prototype: LinkedContainerPrototype,
    json: EntityJson,
) : BaseEntity(json), WithInventory {

    override var bar: Int? = json.bar
    override val inventorySize: ItemStackIndex get() = prototype.inventory_size
    override val allowsFilters: Boolean get() = prototype.inventory_type == InventoryType.with_filters_and_bar
    override val filters: Array<String?> = json.filters.toFilterArray(prototype.inventory_size.toInt())

    public var linkId: Int = json.link_id ?: 0

    override fun exportToJson(json: EntityJson) {
        json.bar = bar
        json.link_id = linkId
        json.filters = filtersAsIndexList()
    }

    override fun copyIsolated(): LinkedContainer = LinkedContainer(prototype, jsonForCopy())
}
