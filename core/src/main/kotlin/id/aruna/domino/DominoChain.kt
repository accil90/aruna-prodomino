package id.aruna.domino

enum class ChainSide { LEFT, RIGHT }

/**
 * Stores the public table chain with explicit oriented values at both ends.
 * Only confirmed played tiles belong in this chain.
 */
class DominoChain {
    private data class PlacedTile(
        val tile: DominoTile,
        val leftValue: Int,
        val rightValue: Int
    )

    private val tiles = mutableListOf<PlacedTile>()

    val isEmpty: Boolean get() = tiles.isEmpty()
    val size: Int get() = tiles.size
    fun snapshot(): List<DominoTile> = tiles.map { it.tile }

    val leftEnd: Int? get() = tiles.firstOrNull()?.leftValue
    val rightEnd: Int? get() = tiles.lastOrNull()?.rightValue

    fun canPlace(tile: DominoTile): Boolean =
        tiles.isEmpty() || legalSides(tile).isNotEmpty()

    fun legalSides(tile: DominoTile): Set<ChainSide> {
        if (tiles.isEmpty()) return setOf(ChainSide.LEFT)
        return buildSet {
            if (tile.matches(leftEnd!!)) add(ChainSide.LEFT)
            if (tile.matches(rightEnd!!)) add(ChainSide.RIGHT)
        }
    }

    fun placeFirst(tile: DominoTile) {
        require(tiles.isEmpty()) { "The chain already has a starting tile." }
        tiles.add(PlacedTile(tile, tile.low, tile.high))
    }

    fun place(tile: DominoTile, side: ChainSide) {
        require(tiles.isNotEmpty()) { "Place the first tile before extending the chain." }
        when (side) {
            ChainSide.LEFT -> {
                val end = leftEnd!!
                require(tile.matches(end)) { "Tile does not match the left end." }
                val newOuter = if (tile.high == end) tile.low else tile.high
                tiles.add(0, PlacedTile(tile, newOuter, end))
            }
            ChainSide.RIGHT -> {
                val end = rightEnd!!
                require(tile.matches(end)) { "Tile does not match the right end." }
                val newOuter = if (tile.low == end) tile.high else tile.low
                tiles.add(PlacedTile(tile, end, newOuter))
            }
        }
    }

    fun clear() {
        tiles.clear()
    }
}
