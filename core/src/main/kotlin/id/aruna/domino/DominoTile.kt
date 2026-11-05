package id.aruna.domino

/**
 * A standard double-six tile. Values are normalized so (a,b) == (b,a).
 */
data class DominoTile private constructor(val low: Int, val high: Int) {
    init {
        require(low in 0..6 && high in 0..6 && low <= high)
    }

    val isDouble: Boolean
        get() = low == high

    fun matches(value: Int): Boolean = low == value || high == value

    companion object {
        fun of(a: Int, b: Int): DominoTile {
            require(a in 0..6 && b in 0..6) {
                "Tile values must be between 0 and 6."
            }
            return DominoTile(minOf(a, b), maxOf(a, b))
        }

        fun fullSet(): Set<DominoTile> = buildSet {
            for (a in 0..6) {
                for (b in a..6) {
                    add(DominoTile(a, b))
                }
            }
        }
    }

    override fun toString(): String = "$low|$high"
}
