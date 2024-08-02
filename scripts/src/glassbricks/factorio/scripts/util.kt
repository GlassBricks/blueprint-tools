package glassbricks.factorio.scripts

import glassbricks.factorio.blueprint.prototypes.VanillaPrototypes
import glassbricks.factorio.blueprint.prototypes.ElectricPolePrototype

val smallPole = VanillaPrototypes.getAs<ElectricPolePrototype>("small-electric-pole")
val mediumPole = VanillaPrototypes.getAs<ElectricPolePrototype>("medium-electric-pole")
val bigPole = VanillaPrototypes.getAs<ElectricPolePrototype>("big-electric-pole")
val substation = VanillaPrototypes.getAs<ElectricPolePrototype>("substation")
