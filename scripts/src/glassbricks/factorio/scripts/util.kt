package glassbricks.factorio.scripts

import glassbricks.factorio.blueprint.prototypes.VanillaPrototypes
import glassbricks.factorio.blueprint.prototypes.ElectricPolePrototype

val smallPole = VanillaPrototypes.getPrototype<ElectricPolePrototype>("small-electric-pole")
val mediumPole = VanillaPrototypes.getPrototype<ElectricPolePrototype>("medium-electric-pole")
val bigPole = VanillaPrototypes.getPrototype<ElectricPolePrototype>("big-electric-pole")
val substation = VanillaPrototypes.getPrototype<ElectricPolePrototype>("substation")
