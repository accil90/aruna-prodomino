# Game rules and assumptions

## Tile set

A standard double-six set contains 28 unique tiles. Each tile is represented as a normalized pair from 0 through 6, so `(1, 4)` and `(4, 1)` represent the same tile.

## Initial target mode

The initial target is four-player Gaple, assuming seven tiles are dealt to each player. The analyzer does not assume that the contents of other players' hands are known.

## Chain and open ends

A tile can connect when one of its values matches an open end of the chain. Local variants may differ in starting-player rules, pass rules, and scoring; those rules should be configurable after the target app's variant is confirmed.

## Information boundaries

- Tiles on the table and the user's own visible/entered tiles may be recorded.
- Tiles not observed remain unknown.
- The system must not assign unknown tiles to a particular player without evidence.
