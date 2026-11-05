package id.aruna.domino

/**
 * A local record of a four-seat standard double-six Gaple round.
 *
 * The app stores only the user's entered hand and tiles confirmed visible on
 * the public table. It never allocates unknown tiles to another seat.
 */
class GapleSession(
    val playerNames: List<String> = listOf("Kamu", "Pemain 2", "Pemain 3", "Pemain 4")
) {
    val tracker = VisibleTileTracker()
    val chain = DominoChain()
    private val hand = linkedSetOf<DominoTile>()

    var currentPlayer: Int = 0
        private set
    var consecutivePasses: Int = 0
        private set
    var roundBlocked: Boolean = false
        private set
    var roundFinished: Boolean = false
        private set

    init {
        require(playerNames.size == 4) { "Gaple standar memakai empat pemain." }
    }

    fun handTiles(): Set<DominoTile> = hand.toSet()

    fun addToHand(tile: DominoTile): Boolean {
        require(tile !in tracker.visibleTiles()) { "Kartu ini sudah tercatat terlihat di meja." }
        require(hand.size < 7 || tile in hand) { "Maksimal tujuh kartu di tangan." }
        return hand.add(tile)
    }

    fun removeFromHand(tile: DominoTile): Boolean = hand.remove(tile)

    fun replaceHand(tiles: Collection<DominoTile>) {
        require(tiles.size <= 7) { "Maksimal tujuh kartu di tangan." }
        require(tiles.distinct().size == tiles.size) { "Kartu tangan tidak boleh ganda." }
        require(tiles.none { it in tracker.visibleTiles() }) {
            "Kartu tangan tidak boleh sama dengan kartu yang sudah terlihat di meja."
        }
        hand.clear()
        hand.addAll(tiles)
    }

    /** Records the user's own confirmed play and advances the turn. */
    fun playOwnTile(tile: DominoTile, side: ChainSide): Boolean {
        check(!roundFinished && !roundBlocked) { "Ronde sudah selesai." }
        require(currentPlayer == 0) { "Sekarang bukan giliran kamu." }
        require(tile in hand) { "Kartu tidak ada di tanganmu." }
        placeOnChain(tile, side)
        hand.remove(tile)
        afterPlay()
        if (hand.isEmpty()) roundFinished = true
        return roundFinished
    }

    /** Records a tile publicly seen being played by another player. */
    fun recordObservedPlay(tile: DominoTile, side: ChainSide, playerIndex: Int) {
        check(!roundFinished && !roundBlocked) { "Ronde sudah selesai." }
        require(playerIndex in 1..3) { "Pilih pemain lain (2 sampai 4)." }
        require(currentPlayer == playerIndex) { "Urutan giliran tidak cocok." }
        require(tile !in hand) { "Kartu itu tercatat di tanganmu." }
        placeOnChain(tile, side)
        afterPlay()
    }

    fun passTurn() {
        check(!roundFinished && !roundBlocked) { "Ronde sudah selesai." }
        consecutivePasses += 1
        currentPlayer = (currentPlayer + 1) % 4
        if (consecutivePasses >= 4) roundBlocked = true
    }

    fun legalOwnMoves(): List<LegalMove> =
        if (chain.isEmpty) hand.sortedWith(compareBy<DominoTile> { it.low }.thenBy { it.high })
            .map { LegalMove(it, setOf(ChainSide.LEFT)) }
        else MoveAnalyzer.legalMoves(hand, chain)

    fun unknownTiles(): Set<DominoTile> =
        MoveAnalyzer.remainingUnknown(tracker.visibleTiles(), hand)

    fun ownHandPipTotal(): Int = hand.sumOf { it.low + it.high }

    fun resetRound() {
        hand.clear()
        tracker.clear()
        chain.clear()
        currentPlayer = 0
        consecutivePasses = 0
        roundBlocked = false
        roundFinished = false
    }

    private fun placeOnChain(tile: DominoTile, side: ChainSide) {
        if (chain.isEmpty) chain.placeFirst(tile) else chain.place(tile, side)
        tracker.markVisible(tile)
    }

    private fun afterPlay() {
        consecutivePasses = 0
        currentPlayer = (currentPlayer + 1) % 4
    }
}
