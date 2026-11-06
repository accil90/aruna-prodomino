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
    fun unknownTilesExcludeVisibleTilesAndOwnHand() {
        val visible = setOf(DominoTile.of(0, 0), DominoTile.of(6, 6))
        val ownHand = setOf(DominoTile.of(0, 1), DominoTile.of(2, 3))
        val unknown = MoveAnalyzer.remainingUnknown(visible, ownHand)

        assertEquals(24, unknown.size)
        assertFalse(unknown.contains(DominoTile.of(0, 0)))
        assertFalse(unknown.contains(DominoTile.of(0, 1)))
        assertTrue(unknown.contains(DominoTile.of(0, 2)))
    }

    @Test
    fun gapleSessionTracksOwnPlayAndUpdatesOpenEnds() {
        val session = GapleSession()
        session.replaceHand(listOf(DominoTile.of(2, 5), DominoTile.of(5, 6)))
        session.playOwnTile(DominoTile.of(2, 5), ChainSide.LEFT)

        assertEquals(1, session.handTiles().size)
        assertEquals(2, session.chain.leftEnd)
        assertEquals(5, session.chain.rightEnd)
        assertEquals(1, session.currentPlayer)
        assertTrue(DominoTile.of(2, 5) in session.tracker.visibleTiles())
    }

    @Test
    fun fourConsecutivePassesMarkTheRoundBlocked() {
        val session = GapleSession()
        repeat(4) { session.passTurn() }
        assertTrue(session.roundBlocked)
        assertEquals(0, session.currentPlayer)
    }

    @Test
    fun sessionUnknownTilesExcludeVisibleTilesAndOwnHand() {
        val session = GapleSession()
        session.replaceHand(listOf(DominoTile.of(1, 2), DominoTile.of(3, 4)))
        session.tracker.markVisible(DominoTile.of(0, 0))
        assertEquals(25, session.unknownTiles().size)
    }


    @Test
    fun sessionKeepsPublicPlayAndPassHistory() {
        val session = GapleSession()
        session.replaceHand(listOf(DominoTile.of(1, 2), DominoTile.of(2, 3)))
        session.playOwnTile(DominoTile.of(1, 2), ChainSide.LEFT)
        session.passTurn()

        assertEquals(2, session.history().size)
        assertEquals(GapleEventType.PLAY, session.history()[0].type)
        assertEquals(DominoTile.of(1, 2), session.history()[0].tile)
        assertEquals(GapleEventType.PASS, session.history()[1].type)
    }

    @Test(expected = IllegalArgumentException::class)
    fun sessionRejectsRecordingTheSamePublicTileTwice() {
        val session = GapleSession()
        session.replaceHand(listOf(DominoTile.of(1, 2), DominoTile.of(2, 3)))
        session.playOwnTile(DominoTile.of(1, 2), ChainSide.LEFT)
        session.recordObservedPlay(DominoTile.of(1, 2), ChainSide.RIGHT, 1)
    }


    @Test
    fun statisticsReportOnlyKnownRoundFacts() {
        val session = GapleSession()
        session.replaceHand(listOf(DominoTile.of(1, 2), DominoTile.of(3, 4)))
        session.tracker.markVisible(DominoTile.of(0, 0))
        session.playOwnTile(DominoTile.of(1, 2), ChainSide.LEFT)

        val stats = session.statistics()
        assertEquals(1, stats.recordedPlays)
        assertEquals(1, stats.ownTilesRemaining)
        assertEquals(7, stats.ownRemainingPips)
        assertEquals(3, stats.knownTiles)
        assertEquals(25, stats.unknownTiles)
    }


    @Test
    fun recommendationChoosesLegalTileWithHighestPipTotal() {
        val chain = DominoChain()
        chain.placeFirst(DominoTile.of(3, 4))

        val recommendation = MoveAnalyzer.recommendMove(
            listOf(
                DominoTile.of(0, 3),
                DominoTile.of(4, 6),
                DominoTile.of(1, 2)
            ),
            chain
        )

        assertEquals(DominoTile.of(4, 6), recommendation?.move?.tile)
        assertEquals(10, recommendation?.pipValue)
        assertEquals(setOf(ChainSide.RIGHT), recommendation?.move?.sides)
    }

}
