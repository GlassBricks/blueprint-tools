package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.Color

public interface WithColor {
    public var color: Color?
}

internal val EntityInit<WithColor>.color: Color?
    get() = self?.color ?: json?.color
