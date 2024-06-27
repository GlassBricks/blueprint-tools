package glassbricks.factorio.blueprint.entity

public sealed interface ControlBehavior {
    public fun exportToJson(): ControlBehaviorJson
}

// todo:
// container
// generic_on_off
// inserter
// lamp
// logistic_container
// roboport
// storage_tank
// train_stop
// decider_combinator
// arithmetic_combinator
// constant_combinator
// transport_belt
// accumulator
// rail_signal
// rail_chain_signal
// wall
// mining_drill
// programmable_speaker
