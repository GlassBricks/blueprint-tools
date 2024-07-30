package glassbricks.factorio.scripts

import glassbricks.factorio.blueprint.prototypes.VanillaPrototypes
import glassbricks.factorio.blueprint.prototypes.ElectricPolePrototype

val smallPole = VanillaPrototypes.get<ElectricPolePrototype>("small-electric-pole")
val mediumPole = VanillaPrototypes.get<ElectricPolePrototype>("medium-electric-pole")
val bigPole = VanillaPrototypes.get<ElectricPolePrototype>("big-electric-pole")
val substation = VanillaPrototypes.get<ElectricPolePrototype>("substation")
