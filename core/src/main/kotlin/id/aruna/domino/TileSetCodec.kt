package id.aruna.domino

/**
 * Small dependency-free codec for saving known tile sets in local preferences.
 * It stores tile identities only; no hidden player assignments are represented.
 */
object TileSetCodec {
    fun encode(tiles: Collection<DominoTile>): String =
        tiles.distinct()
            .sortedWith(compareBy<DominoTile> { it.low }.thenBy { it.high })
            .joinToString(",") { "${it.low}-${it.high}" }

    fun decode(value: String): Set<DominoTile> {
        if (value.isBlank()) return emptySet()
        val result = linkedSetOf<DominoTile>()
        value.split(",").forEach { token ->
            val parts = token.split("-")
            require(parts.size == 2) { "Format kartu tersimpan tidak valid." }
            val a = parts[0].toIntOrNull()
                ?: throw IllegalArgumentException("Angka kartu tersimpan tidak valid.")
            val b = parts[1].toIntOrNull()
                ?: throw IllegalArgumentException("Angka kartu tersimpan tidak valid.")
            require(result.add(DominoTile.of(a, b))) { "Ada kartu tersimpan yang ganda." }
        }
        return result
    }
}
