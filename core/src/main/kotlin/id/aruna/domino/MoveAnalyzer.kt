package id.aruna.domino

data class LegalMove(
    val tile: DominoTile,
    val sides: Set<ChainSide>
)

/**
 * Analyzes only a hand explicitly entered by the user and the public table chain.
 * It does not estimate or reveal other players' concealed tiles.
 */
object MoveAnalyzer {
    fun legalMoves(hand: Collection<DominoTile>, chain: DominoChain): List<LegalMove> {
        return hand.distinct()
            .mapNotNull { tile ->
                val sides = chain.legalSides(tile)
                if (sides.isEmpty()) null else LegalMove(tile, sides)
            }
            .sortedWith(compareBy<LegalMove> { it.tile.low }.thenBy { it.tile.high })
    }

    fun remainingUnknown(
        visibleTiles: Collection<DominoTile>,
        knownHand: Collection<DominoTile> = emptyList()
    ): Set<DominoTile> {
        // Publicly visible tiles and the user's explicitly entered hand are both known.
        // The returned set remains unassigned: it does not infer any opponent's hand.
        return DominoTile.fullSet() - visibleTiles.toSet() - knownHand.toSet()
    }
}
