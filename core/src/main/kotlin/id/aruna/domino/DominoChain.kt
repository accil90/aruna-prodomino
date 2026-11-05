package id.aruna.domino

enum class ChainSide { LEFT, RIGHT }

/**
 * Represents the public chain on the table. It stores only tiles that have
 * actually been confirmed as played.
 */
class DominoChain {
    private val tiles = mutableListOf<DominoTile>()

    val isEmpty: Boolean get() = tiles.isEmpty()
    val size: Int get() = tiles.size
    fun snapshot(): List<DominoTile> = tiles.toList()

    val leftEnd: Int?
        get() = tiles.firstOrNull()?.let { it.low }

    val rightEnd: Int?
        get() = tiles.lastOrNull()?.let { it.high }

    fun canPlace(tile: DominoTile): Boolean {
        if (tiles.isEmpty()) return true
        return tile.matches(leftEnd!!) || tile.matches(rightEnd!!)
    }

    fun legalSides(tile: DominoTile): Set<ChainSide> {
        if (tiles.isEmpty()) return setOf(ChainSide.LEFT)
        return buildSet {
            if (tile.matches(leftEnd!!)) add(ChainSide.LEFT)
            if (tile.matches(rightEnd!!)) add(ChainSide.RIGHT)
        }
    }

    fun placeFirst(tile: DominoTile) {
        require(tiles.isEmpty()) { "The chain already has a starting tile." }
        tiles.add(tile)
    }

    fun place(tile: DominoTile, side: ChainSide) {
        require(tiles.isNotEmpty()) { "Place the first tile before extending the chain." }
        when (side) {
            ChainSide.LEFT -> {
                require(tile.matches(leftEnd!!)) { "Tile does not match the left end." }
                tiles.add(0, tile)
            }
            ChainSide.RIGHT -> {
                require(tile.matches(rightEnd!!)) { "Tile does not match the right end." }
                tiles.add(tile)
            }
        }
    }

    fun clear() = tiles.clear()
}
