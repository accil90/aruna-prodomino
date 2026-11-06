package id.aruna.domino

data class LegalMove(
    val tile: DominoTile,
    val sides: Set<ChainSide>
)

data class MoveRecommendation(
    val move: LegalMove,
    val pipValue: Int,
    val reason: String
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

    /**
     * Selects one transparent practice recommendation for the offline simulator.
     * The heuristic prioritizes shedding the highest pip total; ties prefer a double,
     * then a tile playable on both ends. It uses no opponent-hand inference.
     */
    fun recommendMove(
        hand: Collection<DominoTile>,
        chain: DominoChain
    ): MoveRecommendation? {
        val move = legalMoves(hand, chain)
            .maxWithOrNull(
                compareBy<LegalMove> { it.tile.low + it.tile.high }
                    .thenBy { if (it.tile.isDouble) 1 else 0 }
                    .thenBy { it.sides.size }
                    .thenBy { it.tile.high }
                    .thenBy { it.tile.low }
            ) ?: return null

        val pips = move.tile.low + move.tile.high
        val sideText = move.sides.joinToString(" atau ") {
            if (it == ChainSide.LEFT) "kiri" else "kanan"
        }
        val reason = "Melepas $pips pip; kartu ini cocok di sisi $sideText."
        return MoveRecommendation(move, pips, reason)
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
