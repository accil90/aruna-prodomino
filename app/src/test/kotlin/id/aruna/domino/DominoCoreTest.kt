package id.aruna.domino

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DominoCoreTest {
    @Test
    fun doubleSixSetContainsTwentyEightUniqueTiles() {
        assertEquals(28, DominoTile.fullSet().size)
    }

    @Test
    fun tileFactoryNormalizesOrder() {
        assertEquals(DominoTile.of(2, 5), DominoTile.of(5, 2))
    }

    @Test
    fun chainUpdatesBothEndsWhenTilesAreReversed() {
        val chain = DominoChain()
        chain.placeFirst(DominoTile.of(2, 5))
        chain.place(DominoTile.of(1, 2), ChainSide.LEFT)
        chain.place(DominoTile.of(5, 6), ChainSide.RIGHT)

        assertEquals(1, chain.leftEnd)
        assertEquals(6, chain.rightEnd)
        assertEquals(3, chain.size)
    }

    @Test
    fun analyzerReturnsOnlyMovesMatchingOpenEnds() {
        val chain = DominoChain()
        chain.placeFirst(DominoTile.of(3, 4))

        val moves = MoveAnalyzer.legalMoves(
            listOf(
                DominoTile.of(0, 3),
                DominoTile.of(4, 6),
                DominoTile.of(1, 2)
            ),
            chain
        )

        assertEquals(2, moves.size)
        assertTrue(moves.any { it.tile == DominoTile.of(0, 3) })
        assertTrue(moves.any { it.tile == DominoTile.of(4, 6) })
        assertFalse(moves.any { it.tile == DominoTile.of(1, 2) })
    }

    @Test
    fun unknownTilesAreTheComplementOfVisibleTiles() {
        val visible = setOf(DominoTile.of(0, 0), DominoTile.of(6, 6))
        val unknown = MoveAnalyzer.remainingUnknown(visible)

        assertEquals(26, unknown.size)
        assertFalse(unknown.contains(DominoTile.of(0, 0)))
        assertTrue(unknown.contains(DominoTile.of(0, 1)))
    }
}
