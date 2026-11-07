package id.aruna.domino

import android.graphics.Bitmap
import android.graphics.Rect
import kotlin.math.max
import kotlin.math.min

enum class ScreenTileZone { HAND, TABLE }

data class ScreenTile(
    val bounds: Rect,
    val tile: DominoTile,
    val zone: ScreenTileZone
)

data class ScreenAnalysis(
    val hand: List<DominoTile>,
    val table: List<DominoTile>,
    val leftEnd: Int?,
    val rightEnd: Int?,
    val recommendation: MoveRecommendation?,
    val note: String
)

/**
 * Conservative on-device detector for bright domino faces.
 * It never guesses concealed opponent tiles. Unreadable regions are omitted.
 * Layouts with touching/overlapping tiles may require a later calibration profile.
 */
object DominoScreenAnalyzer {
    // Jumlah kartu tangan yang didukung: berubah-ubah dari 1 sampai 7.
    const val MIN_HAND_CARDS = 1
    const val MAX_HAND_CARDS = 7

    fun analyze(frame: Bitmap): ScreenAnalysis {
        // Area kartu tangan dipisahkan berdasarkan proyeksi kolom pada pita bawah layar.
        // Ini menghindari satu komponen putih besar menelan beberapa kartu sekaligus.
        val handRegions = findHandTileRegions(frame)
        val tableRegions = findTileRegions(frame).filter {
            (it.top + it.bottom) / 2f / frame.height < 0.68f
        }
        val candidates = (tableRegions + handRegions).distinctBy {
            "${it.left}:${it.top}:${it.right}:${it.bottom}"
        }
        val hand = mutableListOf<ScreenTile>()
        val table = mutableListOf<ScreenTile>()

        candidates.forEach { bounds ->
            val vertical = bounds.height() > bounds.width()
            val read = DominoPipRecognizer.recognize(frame, bounds, vertical)
            val tile = read.tile ?: return@forEach
            val centerY = (bounds.top + bounds.bottom) / 2f / frame.height
            val zone = if (centerY >= 0.68f) ScreenTileZone.HAND else ScreenTileZone.TABLE
            val item = ScreenTile(bounds, tile, zone)
            if (zone == ScreenTileZone.HAND) hand.add(item) else table.add(item)
        }

        // Urutkan kartu tangan dari kiri ke kanan. Jangan gunakan distinct():
        // setiap area kartu harus dipertahankan sebagai kartu tersendiri.
        val detectedHand = hand
            .sortedBy { (it.bounds.left + it.bounds.right) / 2 }
            .take(MAX_HAND_CARDS)
            .map { it.tile }

        val orderedTable = table.sortedBy { (it.bounds.left + it.bounds.right) / 2 }
            .map { it.tile }.distinct()
        val chain = buildChain(orderedTable)
        val recommendation = if (chain != null && detectedHand.size >= MIN_HAND_CARDS) {
            MoveAnalyzer.recommendMove(detectedHand, chain)
        } else null

        val note = when {
            candidates.isEmpty() -> "Belum menemukan area kartu yang jelas."
            hand.isEmpty() -> "Area kartu tangan belum terbaca; periksa tata letak layar."
            table.isEmpty() -> "Kartu tangan terbaca, tetapi susunan meja belum terbaca."
            chain == null -> "Kartu meja terdeteksi, tetapi urutan sambungannya belum dapat dipastikan."
            else -> "Pembacaan visual awal; cocokkan hasil dengan layar permainan."
        }
        return ScreenAnalysis(
            hand = detectedHand,
            table = orderedTable,
            leftEnd = chain?.leftEnd,
            rightEnd = chain?.rightEnd,
            recommendation = recommendation,
            note = note
        )
    }

    private fun buildChain(tiles: List<DominoTile>): DominoChain? {
        if (tiles.isEmpty()) return null
        val chain = DominoChain()
        return runCatching {
            chain.placeFirst(tiles.first())
            tiles.drop(1).forEach { tile ->
                val sides = chain.legalSides(tile)
                if (sides.isEmpty()) error("Disconnected table sequence")
                val side = when {
                    ChainSide.RIGHT in sides -> ChainSide.RIGHT
                    else -> ChainSide.LEFT
                }
                chain.place(tile, side)
            }
            chain
        }.getOrNull()
    }

    /**
     * Kartu tangan berada berjajar mendatar di pita bawah layar.
     * Hitung piksel wajah kartu per kolom, lalu jadikan setiap rentang kolom
     * yang terpisah sebagai satu kandidat kartu.
     */
    private fun findHandTileRegions(bitmap: Bitmap): List<Rect> {
        val scale = min(1f, 640f / bitmap.width)
        val width = max(1, (bitmap.width * scale).toInt())
        val height = max(1, (bitmap.height * scale).toInt())
        val sample = if (width == bitmap.width && height == bitmap.height) bitmap
            else Bitmap.createScaledBitmap(bitmap, width, height, true)
        val pixels = IntArray(width * height)
        sample.getPixels(pixels, 0, width, 0, 0, width, height)
        if (sample !== bitmap) sample.recycle()

        val top = (height * 0.64f).toInt().coerceIn(0, height - 1)
        val bottom = (height * 0.98f).toInt().coerceIn(top + 1, height)
        val roiHeight = bottom - top
        val projection = IntArray(width)
        val mask = BooleanArray(width * roiHeight)

        for (y in 0 until roiHeight) {
            for (x in 0 until width) {
                val c = pixels[(top + y) * width + x]
                val red = (c ushr 16) and 255
                val green = (c ushr 8) and 255
                val blue = c and 255
                val hi = max(red, max(green, blue))
                val lo = min(red, min(green, blue))
                val brightness = (red * 299 + green * 587 + blue * 114) / 1000
                val brightFace = brightness >= 188 && hi - lo <= 82
                mask[y * width + x] = brightFace
                if (brightFace) projection[x]++
            }
        }

        // Satu kolom dianggap bagian kartu bila cukup banyak baris berisi wajah terang.
        val activeThreshold = max(4, (roiHeight * 0.07f).toInt())
        val active = BooleanArray(width) { projection[it] >= activeThreshold }
        val found = mutableListOf<Rect>()
        val sx = bitmap.width.toFloat() / width
        val sy = bitmap.height.toFloat() / height

        var x = 0
        while (x < width) {
            while (x < width && !active[x]) x++
            if (x >= width) break
            val left = x
            var lastActive = x
            var gap = 0
            x++
            while (x < width) {
                if (active[x]) {
                    lastActive = x
                    gap = 0
                } else {
                    gap++
                    if (gap > 2) break
                }
                x++
            }
            val right = lastActive + 1
            if (right - left >= max(8, (width * 0.012f).toInt())) {
                var minY = roiHeight
                var maxY = -1
                for (px in left until right) {
                    for (py in 0 until roiHeight) {
                        if (mask[py * width + px]) {
                            minY = min(minY, py)
                            maxY = max(maxY, py)
                        }
                    }
                }
                if (maxY > minY && maxY - minY >= (roiHeight * 0.18f).toInt()) {
                    found.add(Rect(
                        (left * sx).toInt().coerceIn(0, bitmap.width - 1),
                        ((top + minY) * sy).toInt().coerceIn(0, bitmap.height - 1),
                        (right * sx).toInt().coerceIn(1, bitmap.width),
                        ((top + maxY + 1) * sy).toInt().coerceIn(1, bitmap.height)
                    ))
                }
            }
        }
        return found.sortedBy { (it.left + it.right) / 2 }.take(MAX_HAND_CARDS)
    }

    private fun findTileRegions(bitmap: Bitmap): List<Rect> {
        val scale = min(1f, 640f / bitmap.width)
        val width = max(1, (bitmap.width * scale).toInt())
        val height = max(1, (bitmap.height * scale).toInt())
        val pixels = IntArray(width * height)
        val sample = if (width == bitmap.width && height == bitmap.height) bitmap
            else Bitmap.createScaledBitmap(bitmap, width, height, true)
        sample.getPixels(pixels, 0, width, 0, 0, width, height)
        if (sample !== bitmap) sample.recycle()

        val mask = BooleanArray(pixels.size)
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (c ushr 16) and 255
            val g = (c ushr 8) and 255
            val b = c and 255
            val hi = max(r, max(g, b))
            val lo = min(r, min(g, b))
            val brightness = (r * 299 + g * 587 + b * 114) / 1000
            mask[i] = brightness >= 188 && hi - lo <= 82
        }

        val visited = BooleanArray(mask.size)
        val queue = IntArray(mask.size)
        val found = mutableListOf<Rect>()
        for (start in mask.indices) {
            if (!mask[start] || visited[start]) continue
            var head = 0
            var tail = 0
            queue[tail++] = start
            visited[start] = true
            var left = width
            var top = height
            var right = 0
            var bottom = 0
            var area = 0
            while (head < tail) {
                val index = queue[head++]
                val x = index % width
                val y = index / width
                area++
                left = min(left, x); right = max(right, x)
                top = min(top, y); bottom = max(bottom, y)
                for (dy in -1..1) for (dx in -1..1) {
                    if (dx == 0 && dy == 0) continue
                    val nx = x + dx
                    val ny = y + dy
                    if (nx !in 0 until width || ny !in 0 until height) continue
                    val next = ny * width + nx
                    if (mask[next] && !visited[next]) {
                        visited[next] = true
                        queue[tail++] = next
                    }
                }
            }
            val w = right - left + 1
            val h = bottom - top + 1
            val ratio = w.toFloat() / h.coerceAtLeast(1)
            val plausibleShape = (ratio in 1.15f..3.25f) || (ratio in 0.30f..0.86f)
            val largeEnough = max(w, h) >= 30 && min(w, h) >= 15 && area >= 360
            val notWholeScreen = area < width * height * 0.30f
            if (plausibleShape && largeEnough && notWholeScreen) {
                val sx = bitmap.width.toFloat() / width
                val sy = bitmap.height.toFloat() / height
                found.add(Rect(
                    (left * sx).toInt().coerceIn(0, bitmap.width - 1),
                    (top * sy).toInt().coerceIn(0, bitmap.height - 1),
                    ((right + 1) * sx).toInt().coerceIn(1, bitmap.width),
                    ((bottom + 1) * sy).toInt().coerceIn(1, bitmap.height)
                ))
            }
        }
        return found.sortedByDescending { it.width() * it.height() }.take(32)
    }
}
