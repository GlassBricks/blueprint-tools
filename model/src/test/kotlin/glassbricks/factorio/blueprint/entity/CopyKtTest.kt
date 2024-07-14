package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.pos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CopyKtTest {
    @Test
    fun `can copy basic entity`() {
        val entity = blueprintPrototypes.createEntity("iron-chest", Position.ZERO) as Container
        entity.bar = 3
        val copy = entity.copyWithOldConnections()
        assertEquals(entity.prototype, copy.prototype)
        assertEquals(entity.position, copy.position)
        assertEquals(entity.direction, copy.direction)
        assertEquals(entity.bar, copy.bar)
    }

    private fun createPole(): ElectricPole {
        return blueprintPrototypes.createEntity("small-electric-pole", Position.ZERO) as ElectricPole
    }

    @Test
    fun `can copy cable connection point`() {
        val pole1 = createPole()
        val pole2 = createPole()
        val chest1 = blueprintPrototypes.createEntity("iron-chest", Position.ZERO) as Container

        pole1.cableConnections.add(pole2)
        pole1.circuitConnections.red.add(chest1)
        pole1.circuitConnections.green.add(pole2)

        val pole1Copy = pole1.copyWithOldConnections()
        assertEquals(setOf<CableConnectionPoint>(pole2), pole1Copy.cableConnections)
        assertEquals(setOf<CircuitConnectionPoint>(chest1), pole1Copy.circuitConnections.red)
        assertEquals(setOf<CircuitConnectionPoint>(pole2), pole1Copy.circuitConnections.green)

        assertTrue(pole2.cableConnections.contains(pole1Copy))
        assertTrue(chest1.circuitConnections.red.contains(pole1Copy))
        assertTrue(pole2.circuitConnections.green.contains(pole1Copy))
    }

    @Test
    fun `can copy power switch connections`() {
        val powerSwitch = blueprintPrototypes.createEntity("power-switch", Position.ZERO) as PowerSwitch
        val pole1 = createPole()
        val pole2 = createPole()

        powerSwitch.leftConnections.add(pole1)
        powerSwitch.rightConnections.add(pole2)
        powerSwitch.circuitConnections.red.add(pole1)

        val powerSwitchCopy = powerSwitch.copyWithOldConnections()
        assertEquals(setOf<CableConnectionPoint>(pole1), powerSwitchCopy.leftConnections)
        assertEquals(setOf<CableConnectionPoint>(pole2), powerSwitchCopy.rightConnections)
        assertEquals(setOf<CircuitConnectionPoint>(pole1), powerSwitchCopy.circuitConnections.red)

        assertTrue(pole1.cableConnections.contains(powerSwitchCopy.left))
        assertTrue(pole2.cableConnections.contains(powerSwitchCopy.right))
    }

    @Test
    fun `can copy combinator connections`() {
        val combinator =
            blueprintPrototypes.createEntity("arithmetic-combinator", Position.ZERO) as ArithmeticCombinator
        val pole1 = createPole()
        val pole2 = createPole()

        combinator.inputConnections.red.add(pole1)
        combinator.outputConnections.green.add(pole2)

        val combinatorCopy = combinator.copyWithOldConnections()
        assertEquals(setOf<CircuitConnectionPoint>(pole1), combinatorCopy.inputConnections.red)
        assertEquals(setOf<CircuitConnectionPoint>(pole2), combinatorCopy.outputConnections.green)

        assertTrue(pole1.circuitConnections.red.contains(combinatorCopy.input))
        assertTrue(pole2.circuitConnections.green.contains(combinatorCopy.output))
    }

    @Test
    fun `can copy multiple basic entities`() {
        val entity1 = blueprintPrototypes.createEntity("iron-chest", Position.ZERO) as Container
        val entity2 = blueprintPrototypes.createEntity("iron-chest", pos(1.0, 1.0)) as Container
        val entity3 = blueprintPrototypes.createEntity("iron-chest", pos(2.0, 2.0)) as Container
        val entities = listOf(entity1, entity2, entity3)
        val copies = copyEntitiesWithConnections(listOf(entity1, entity2, entity3))
        assertEquals(entities.size, copies.size)
        for (entity in entities) {
            val copy = copies[entity]!!
            assertEquals(entity.prototype, copy.prototype)
            assertEquals(entity.position, copy.position)
            assertEquals(entity.direction, copy.direction)
        }
    }

    @Test
    fun `can copy multiple entities with connections`() {
        val pole1 = createPole()
        val pole2 = createPole()
        val chest1 = blueprintPrototypes.createEntity("iron-chest", Position.ZERO) as Container
        val chest2 = blueprintPrototypes.createEntity("iron-chest", Position.ZERO) as Container
        val entities = listOf(pole1, pole2, chest1, chest2)
        pole1.cableConnections.add(pole2)
        pole1.circuitConnections.red.add(chest1)
        pole1.circuitConnections.green.add(chest2)
        val copies = copyEntitiesWithConnections(entities)
        val pole1Copy = copies[pole1] as ElectricPole
        val pole2Copy = copies[pole2] as ElectricPole
        val chest1Copy = copies[chest1] as Container
        val chest2Copy = copies[chest2] as Container
        assertEquals(setOf<CableConnectionPoint>(pole2Copy), pole1Copy.cableConnections)
        assertEquals(setOf<CircuitConnectionPoint>(chest1Copy), pole1Copy.circuitConnections.red)
        assertEquals(setOf<CircuitConnectionPoint>(chest2Copy), pole1Copy.circuitConnections.green)
        assertTrue(pole2Copy.cableConnections.contains(pole1Copy))
        assertTrue(chest1Copy.circuitConnections.red.contains(pole1Copy))
        assertTrue(chest2Copy.circuitConnections.green.contains(pole1Copy))
    }

    @Test
    fun `can copy multiple entities with power switch connections`() {
        val powerSwitch = blueprintPrototypes.createEntity("power-switch", Position.ZERO) as PowerSwitch
        val pole1 = createPole()
        val pole2 = createPole()
        val entities = listOf(powerSwitch, pole1, pole2)
        powerSwitch.leftConnections.add(pole1)
        powerSwitch.rightConnections.add(pole2)
        powerSwitch.circuitConnections.red.add(pole1)
        val copies = copyEntitiesWithConnections(entities)
        val powerSwitchCopy = copies[powerSwitch] as PowerSwitch
        val pole1Copy = copies[pole1] as ElectricPole
        val pole2Copy = copies[pole2] as ElectricPole
        assertEquals(setOf<CableConnectionPoint>(pole1Copy), powerSwitchCopy.leftConnections)
        assertEquals(setOf<CableConnectionPoint>(pole2Copy), powerSwitchCopy.rightConnections)
        assertEquals(setOf<CircuitConnectionPoint>(pole1Copy), powerSwitchCopy.circuitConnections.red)
        assertTrue(pole1Copy.cableConnections.contains(powerSwitchCopy.left))
        assertTrue(pole2Copy.cableConnections.contains(powerSwitchCopy.right))
    }

    @Test
    fun `can copy multiple entities with combinator connections`() {
        val combinator =
            blueprintPrototypes.createEntity("arithmetic-combinator", Position.ZERO) as ArithmeticCombinator
        val pole1 = createPole()
        val pole2 = createPole()
        val entities = listOf(combinator, pole1, pole2)
        combinator.inputConnections.red.add(pole1)
        combinator.outputConnections.green.add(pole2)
        val copies = copyEntitiesWithConnections(entities)
        val combinatorCopy = copies[combinator] as ArithmeticCombinator
        val pole1Copy = copies[pole1] as ElectricPole
        val pole2Copy = copies[pole2] as ElectricPole
        assertEquals(setOf<CircuitConnectionPoint>(pole1Copy), combinatorCopy.inputConnections.red)
        assertEquals(setOf<CircuitConnectionPoint>(pole2Copy), combinatorCopy.outputConnections.green)
        assertTrue(pole1Copy.circuitConnections.red.contains(combinatorCopy.input))
        assertTrue(pole2Copy.circuitConnections.green.contains(combinatorCopy.output))
    }
}
