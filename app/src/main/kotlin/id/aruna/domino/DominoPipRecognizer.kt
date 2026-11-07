package id.aruna.domino

import android.graphics.Bitmap
import android.graphics.Rect
import kotlin.math.max
import kotlin.math.min

/**
 * Offline screenshot helper for counting pips inside a user-selected domino tile rectangle.
 *
 * The caller supplies the tile bounds; this class does not infer card locations or read a live game.
 * Both horizontal and vertical tile layouts are supported.
 */
data class TileReadResult(
    val tile: DominoTile?,
    val firstHalfPips: Int?,
    val secondHalfPips: Int?,
    val message: String
)

object DominoPipRecognizer {
    fun recognize(bitmap: Bitmap, tileBounds: Rect, vertical: Boolean = false): TileReadResult {
        val bounds = Rect(
            tileBounds.left.coerceIn(0, bitmap.width),
            tileBounds.top.coerceIn(0, bitmap.height),
            tileBounds.right.coerceIn(0, bitmap.width),
            tileBounds.bottom.coerceIn(0, bitmap.height)
        )
        if (bounds.width() < 12 || bounds.height() < 12) {
            return TileReadResult(null, null, null, "Area kartu terlalu kecil.")
        }

        val first: Rect
        val second: Rect
        if (vertical) {
            val split = bounds.top + bounds.height() / 2
            first = Rect(bounds.left, bounds.top, bounds.right, split)
            second = Rect(bounds.left, split, bounds.right, bounds.bottom)
        } else {
            val split = bounds.left + bounds.width() / 2
            first = Rect(bounds.left, bounds.top, split, bounds.bottom)
            second = Rect(split, bounds.top, bounds.right, bounds.bottom)
        }

        val a = countPips(bitmap, first)
        val b = countPips(bitmap, second)
        if (a !in 0..6 || b !in 0..6) {
            return TileReadResult(null, a, b, "Titik belum terbaca jelas; periksa area kartu.")
        }
        val tile = DominoTile.of(a, b)
        return TileReadResult(tile, a, b, "Tebakan heuristik awal; wajib cocokkan dengan gambar asli.")
    }

    private fun countPips(bitmap: Bitmap, source: Rect): Int {
        val insetX = max(1, source.width() / 12)
        val insetY = max(1, source.height() / 12)
        val area = Rect(
            source.left + insetX,
            source.top + insetY,
            source.right - insetX,
            source.bottom - insetY
        )
        if (area.width() <= 2 || area.height() <= 2) return -1

        val width = area.width()
        val height = area.height()
        val visited = BooleanArray(width * height)
        val mask = BooleanArray(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val color = bitmap.getPixel(area.left + x, area.top + y)
                val red = (color shr 16) and 0xff
                val green = (color shr 8) and 0xff
                val blue = color and 0xff
                val brightness = (red * 299 + green * 587 + blue * 114) / 1000
                val isRedPip = red > 135 && red > green * 1.35 && red > blue * 1.2
                val isDarkPip = brightness < 105 && max(red, max(green, blue)) < 150
                mask[y * width + x] = isRedPip || isDarkPip
            }
        }

        val minBlob = max(3, (width * height * 0.00045f).toInt())
        val maxBlob = max(minBlob + 1, (width * height * 0.12f).toInt())
        var count = 0
        val queue = IntArray(width * height)

        for (index in mask.indices) {
            if (!mask[index] || visited[index]) continue
            var head = 0
            var tail = 0
            queue[tail++] = index
            visited[index] = true
            var size = 0

            while (head < tail) {
                val current = queue[head++]
                size++
                val x = current % width
                val y = current / width
                val neighbors = intArrayOf(
                    if (x > 0) current - 1 else -1,
                    if (x + 1 < width) current + 1 else -1,
                    if (y > 0) current - width else -1,
                    if (y + 1 < height) current + width else -1
                )
                for (next in neighbors) {
                    if (next >= 0 && mask[next] && !visited[next]) {
                        visited[next] = true
                        queue[tail++] = next
                    }
                }
            }
            if (size in minBlob..maxBlob) count++
        }
        return if (count <= 6) count else -1
    }
}
