package id.aruna.domino

/**
 * Tracks only tiles explicitly confirmed as visible.
 * Unknown tiles remain unknown; this class never assigns them to players.
 */
class VisibleTileTracker {
    private val visible = linkedSetOf<DominoTile>()

    fun markVisible(tile: DominoTile): Boolean {
        require(tile in DominoTile.fullSet()) {
            "Tile is not part of a double-six set."
        }
        return visible.add(tile)
    }

    fun removeVisible(tile: DominoTile): Boolean = visible.remove(tile)

    fun visibleTiles(): Set<DominoTile> = visible.toSet()

    fun unknownTiles(): Set<DominoTile> = DominoTile.fullSet() - visible

    fun visibleCount(): Int = visible.size

    fun unknownCount(): Int = 28 - visible.size

    fun clear() {
        visible.clear()
    }
}
