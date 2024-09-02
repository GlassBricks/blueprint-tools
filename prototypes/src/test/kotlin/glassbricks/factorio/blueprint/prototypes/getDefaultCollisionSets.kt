package glassbricks.factorio.blueprint.prototypes

fun main() {
    val input = """
          ["accumulator"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["ammo-turret"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["arithmetic-combinator"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["arrow"] = {},
          ["artillery-flare"] = {},
          ["artillery-projectile"] = {},
          ["artillery-turret"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["artillery-wagon"] = {"train-layer"},
          ["assembling-machine"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["beacon"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["beam"] = {},
          ["boiler"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["burner-generator"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["car"] = {"player-layer", "train-layer", "consider-tile-transitions"},
          ["cargo-wagon"] = {"train-layer"},
          ["character-corpse"] = {},
          ["character"] = {"player-layer", "train-layer", "consider-tile-transitions"},
          ["cliff"] = {"item-layer", "object-layer", "player-layer", "water-tile", "not-colliding-with-itself"},
          ["combat-robot"] = {},
          ["constant-combinator"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["construction-robot"] = {},
          ["container"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["corpse"] = {},
          ["curved-rail"] = {"floor-layer", "item-layer", "object-layer", "rail-layer", "water-tile", "not-colliding-with-itself"},
          ["decider-combinator"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["deconstructible-tile-proxy"] = {"ground-tile"},
          ["electric-energy-interface"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["electric-pole"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["electric-turret"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["entity-ghost"] = {"ghost-layer"},
          ["explosion"] = {},
          ["fire"] = {},
          ["fish"] = {"ground-tile", "colliding-with-tiles-only"},
          ["flame-thrower-explosion"] = {},
          ["fluid-turret"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["fluid-wagon"] = {"train-layer"},
          ["flying-text"] = {},
          ["furnace"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["gate"] = {"item-layer", "object-layer", "player-layer", "train-layer", "water-tile"},
          ["generator"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["heat-interface"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["heat-pipe"] = {"floor-layer", "object-layer", "water-tile"},
          ["highlight-box"] = {},
          ["infinity-container"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["infinity-pipe"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["inserter"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["item-entity"] = {"item-layer"},
          ["item-request-proxy"] = {},
          ["lab"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["lamp"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["land-mine"] = {"object-layer", "water-tile", "rail-layer"},
          ["leaf-particle"] = {},
          ["linked-belt"] = {"item-layer", "object-layer", "transport-belt-layer", "water-tile"},
          ["linked-container"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["loader-1x1"] = {"item-layer", "object-layer", "transport-belt-layer", "water-tile"},
          ["loader"] = {"item-layer", "object-layer", "transport-belt-layer", "water-tile"},
          ["locomotive"] = {"train-layer"},
          ["logistic-container"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["logistic-robot"] = {},
          ["market"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["mining-drill"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["offshore-pump"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["particle-source"] = {},
          ["particle"] = {},
          ["pipe-to-ground"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["pipe"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["player-port"] = {"floor-layer", "object-layer", "water-tile"},
          ["power-switch"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["programmable-speaker"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["projectile"] = {},
          ["pump"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["radar"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["rail-chain-signal"] = {"floor-layer", "item-layer", "rail-layer"},
          ["rail-remnants"] = {},
          ["rail-signal"] = {"floor-layer", "item-layer", "rail-layer"},
          ["reactor"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["resource"] = {"resource-layer"},
          ["roboport"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["rocket-silo-rocket-shadow"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["rocket-silo-rocket"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["rocket-silo"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["simple-entity-with-force"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["simple-entity-with-owner"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["simple-entity"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["smoke-with-trigger"] = {},
          ["smoke"] = {},
          ["solar-panel"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["speech-bubble"] = {},
          ["spider-leg"] = {"player-layer", "rail-layer"},
          ["spider-vehicle"] = {"player-layer", "train-layer"},
          ["splitter"] = {"item-layer", "object-layer", "transport-belt-layer", "water-tile"},
          ["sticker"] = {},
          ["storage-tank"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["straight-rail"] = {"floor-layer", "item-layer", "object-layer", "rail-layer", "water-tile", "not-colliding-with-itself"},
          ["stream"] = {},
          ["tile"] = {},  -- Tile prototypes are required to have a collision mask so have no default
          ["tile-ghost"] = {"ghost-layer"},
          ["train-stop"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["transport-belt"] = {"floor-layer", "object-layer", "transport-belt-layer", "water-tile"},
          ["tree"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["turret"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["underground-belt"] = {"item-layer", "object-layer", "transport-belt-layer", "water-tile"},
          ["unit-spawner"] = {"item-layer", "object-layer", "player-layer", "water-tile"},
          ["unit"] = {"player-layer", "train-layer", "not-colliding-with-itself"},
          ["wall"] = {"item-layer", "object-layer", "player-layer", "water-tile"}
    """.trimIndent()
    val exists = setOf(
        "accumulator",
        "artillery-turret",
        "beacon",
        "boiler",
        "burner-generator",
        "arithmetic-combinator",
        "decider-combinator",
        "constant-combinator",
        "container",
        "logistic-container",
        "infinity-container",
        "assembling-machine",
        "rocket-silo",
        "furnace",
        "electric-energy-interface",
        "electric-pole",
        "gate",
        "generator",
        "heat-interface",
        "heat-pipe",
        "inserter",
        "lab",
        "lamp",
        "land-mine",
        "linked-container",
        "mining-drill",
        "offshore-pump",
        "pipe",
        "infinity-pipe",
        "pipe-to-ground",
        "player-port",
        "power-switch",
        "programmable-speaker",
        "pump",
        "radar",
        "straight-rail",
        "curved-rail",
        "rail-chain-signal",
        "rail-signal",
        "reactor",
        "roboport",
        "simple-entity-with-owner",
        "simple-entity-with-force",
        "solar-panel",
        "storage-tank",
        "train-stop",
        "linked-belt",
        "loader-1x1",
        "loader",
        "splitter",
        "transport-belt",
        "underground-belt",
        "turret",
        "ammo-turret",
        "electric-turret",
        "fluid-turret",
        "artillery-wagon",
        "cargo-wagon",
        "fluid-wagon",
        "locomotive",
        "wall",
        "item",
        "module",
        "ammo",
        "armor",
        "blueprint-book",
        "blueprint",
        "capsule",
        "copy-paste-tool",
        "deconstruction-item",
        "gun",
        "item-with-entity-data",
        "item-with-inventory",
        "item-with-label",
        "item-with-tags",
        "mining-tool",
        "rail-planner",
        "repair-tool",
        "selection-tool",
        "spidertron-remote",
        "tool",
        "upgrade-item"
    )
    val seenSets = mutableListOf<Set<String>>()
    val values = mutableListOf<Pair<String, Int>>()
    for (line in input.lines()) {
        val regex = Regex("""\["(.+?)"] = \{(.*?)}.*""")
        val match = regex.matchEntire(line)!!
        val name = match.groupValues[1]
        if (name !in exists) continue
        val layers = match.groupValues[2]
            .split(",")
            .map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotEmpty() }
            .toSet()
//        layers.forEach { CollisionMaskLayer.valueOf(it) }
        val index = seenSets.indexOf(layers).let {
            if (it == -1) {
                seenSets.add(layers)
                seenSets.lastIndex
            } else it
        }
        values.add(name to index)
    }
    println(buildString {
        append("val sets = arrayOf(\n")
        for (set in seenSets) {
            append("    CollisionMask(EnumSet.of(")
            append(set.sorted().joinToString(", ") { "CollisionMaskLayer.`${it}`" })
            append(")),\n")
        }
        append(")\n")
        append("DEFAULT_COLLISION_MASKS = mapOf(\n")
        for ((name, index) in values) {
            append("    \"${name}\" to sets[${index}],\n")
        }
        append(")\n")
    })
}
