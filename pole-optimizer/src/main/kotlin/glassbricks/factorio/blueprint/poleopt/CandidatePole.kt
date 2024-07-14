package glassbricks.factorio.glassbricks.factorio.blueprint.poleopt

import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.entity.Entity
import glassbricks.factorio.blueprint.model.BlueprintModel
import glassbricks.factorio.blueprint.prototypes.ElectricPolePrototype

public class CandidatePole(
    public val position: Position,
    public val prototype: ElectricPolePrototype,
    public val poweredEntities: Set<Entity>
)

public class CandidatePoleSet(
    public val poles: Set<CandidatePole>,
    public val poweredBy: Map<Entity, Set<CandidatePole>>
)

public fun getCandidatePoles(model: BlueprintModel) {

}
