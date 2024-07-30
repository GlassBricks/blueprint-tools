# Basic belts and underground belts
## Tiles

Every tile can be exactly one of:
- Empty
- A belt
- An input underground
- An output underground

Belt tiles may have an "output" direction:
- If empty, no direction
- If belt, must exist, and is direction of belt
- If input ug, no direction
- If output ug, must exist, and is direction of ug

Belt tiles may have an "input" direction. If no sideloading:
- If empty, no direction
- If belt, may be any direction except the output direction
- If input ug, may exist only in direction of ug
- If output ug, no direction

Todo: sideloading (inputs not in the main input direction)

### Constraints
- There can only be one belt type for each tile

If a belt has a main input direction, the corresponding tile must have an output in that direction.
- For belts:
  - If the main input is opposite the output, there must be an input from that direction.
  - If the main input direction is 90 deg from output, there must be no other inputs.

If a belt has an output direction, the corresponding tile must have an input in that direction.
Todo: sideloading

Exceptions: designated input/output undergrounds

## Underground belts
(Todo: for each belt tier)
Each tile may have virtual "underground" belts in all directions, which connect underground entrances/exit.

### Constraints
- For each axis:
  - Only one of the directions can be used (e.g. north or south, east or west, but not both).
  - Virtual ug belts cannot overlap with input or output undergrounds with the same axis.

- If a virtual ug belt OR input ug exists, the forward tile must be a virtual ug or an output ug in that direction
- If a virtual ug belt OR output ug exists, the backward tile must be a virtual ug or an input ug in that direction.
  - This is technically optional but helps with constraint propagation

The constrains ensure that every input underground "propagates" virtual ug belts until an output ug is reached.


#### Underground distance
- If an underground belt exists, there must be some output underground within range of it.

## Color/path propogation
- Every belt may have a color that labels it. Color 0 is reserved for "no color".

- Empty tiles must have color 0.
- For a non-sideloaded belt, the input and output belt colors must match.
- For an underground, the input/output belt colors must match.
- For a virtual underground belt, the input/output belt colors must match.