package id.aruna.domino

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private val tracker = VisibleTileTracker()
    private val selectedHand = linkedSetOf<DominoTile>()
    private val preferences by lazy {
        getSharedPreferences("aruna_domino_local_session", MODE_PRIVATE)
    }
    private val observedButtons = linkedMapOf<DominoTile, Button>()
    private val handButtons = linkedMapOf<DominoTile, Button>()
    private lateinit var status: TextView
    private lateinit var handStatus: TextView
    private lateinit var leftInput: EditText
    private lateinit var rightInput: EditText
    private lateinit var result: TextView
    private lateinit var scanResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 24, 18, 24)
        }

        page.addView(TextView(this).apply {
            text = "ARUNA DOMINO PRO"
            textSize = 25f
            gravity = Gravity.CENTER
        })
        page.addView(TextView(this).apply {
            text = "Pengamatan visual • pencatatan • simulator offline"
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 16)
        })

        page.addView(sectionTitle("PENGENALAN KARTU DARI SCREENSHOT"))
        page.addView(TextView(this).apply {
            text = "Pilih screenshot yang tersimpan. Profil awal menggunakan tata letak Royal Domino landscape yang kamu kirim. Hasil pembacaan perlu diperiksa."
            textSize = 13f
        })
        page.addView(Button(this).apply {
            text = "Pilih screenshot dan baca kartu tangan"
            setOnClickListener {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
                startActivityForResult(Intent.createChooser(intent, "Pilih screenshot domino"), REQUEST_IMAGE)
            }
        })
        scanResult = TextView(this).apply {
            textSize = 15f
            setPadding(0, 8, 0, 16)
            text = "Belum ada screenshot yang dibaca."
        }
        page.addView(scanResult)

        status = TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 12)
        }
        page.addView(status)
        page.addView(sectionTitle("KARTU YANG SUDAH TERLIHAT"))
        page.addView(TextView(this).apply {
            text = "Untuk pencatatan sesi latihan, tandai kartu yang sudah terlihat."
            textSize = 13f
        })
        page.addView(makeTileGrid(observedButtons) { tile ->
            if (tile in tracker.visibleTiles()) tracker.removeVisible(tile)
            else {
                tracker.markVisible(tile)
                selectedHand.remove(tile)
                refreshHand()
            }
            persistState()
            refreshObserved()
        })
        page.addView(Button(this).apply {
            text = "Bersihkan semua tanda"
            setOnClickListener {
                tracker.clear()
                persistState()
                refreshObserved()
            }
        })

        page.addView(sectionTitle("SIMULATOR LATIHAN OFFLINE"))
        page.addView(TextView(this).apply {
            text = "Pilih hingga 7 kartu tangan simulasi. Masukkan angka ujung meja untuk menghitung satu rekomendasi latihan."
            textSize = 13f
        })
        handStatus = TextView(this).apply {
            textSize = 15f
            setPadding(0, 8, 0, 4)
        }
        page.addView(handStatus)
        page.addView(makeTileGrid(handButtons) { tile ->
            if (tile in selectedHand) selectedHand.remove(tile)
            else if (tile !in tracker.visibleTiles() && selectedHand.size < 7) selectedHand.add(tile)
            persistState()
            refreshHand()
        })

        val ends = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        leftInput = makeNumberInput("Ujung kiri")
        rightInput = makeNumberInput("Ujung kanan")
        ends.addView(leftInput, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        ends.addView(rightInput, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        page.addView(ends)

        page.addView(Button(this).apply {
            text = "Rekomendasikan 1 kartu (simulasi offline)"
            setOnClickListener { analyzeHand() }
        })
        result = TextView(this).apply {
            textSize = 15f
            setPadding(0, 10, 0, 24)
        }
        page.addView(result)

        setContentView(ScrollView(this).apply { addView(page) })
        restoreState()
        refreshObserved()
        refreshHand()
    }

    @Deprecated("Deprecated in Android API; retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_IMAGE || resultCode != RESULT_OK) return
        val uri: Uri = data?.data ?: return
        val bitmap = runCatching {
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
        if (bitmap == null) {
            scanResult.text = "Gambar tidak dapat dibuka."
            return
        }
        scanResult.text = recognizeReferenceLayout(bitmap)
        bitmap.recycle()
    }

    private fun recognizeReferenceLayout(bitmap: Bitmap): String {
        // Calibrated to the two visible hand tiles in the supplied 1536x691 landscape screenshot.
        val regions = listOf(
            Rect((bitmap.width * 0.447f).toInt(), (bitmap.height * 0.672f).toInt(),
                (bitmap.width * 0.495f).toInt(), (bitmap.height * 0.890f).toInt()),
            Rect((bitmap.width * 0.505f).toInt(), (bitmap.height * 0.672f).toInt(),
                (bitmap.width * 0.554f).toInt(), (bitmap.height * 0.890f).toInt())
        )
        val lines = regions.mapIndexed { index, rect ->
            val safe = Rect(rect.left.coerceIn(0, bitmap.width), rect.top.coerceIn(0, bitmap.height),
                rect.right.coerceIn(0, bitmap.width), rect.bottom.coerceIn(0, bitmap.height))
            if (safe.width() < 8 || safe.height() < 8) {
                "Kartu ${index + 1}: area tidak ditemukan."
            } else {
                val read = DominoPipRecognizer.recognize(bitmap, safe, vertical = true)
                if (read.tile == null) "Kartu ${index + 1}: belum terbaca — ${read.message}"
                else "Kartu ${index + 1}: ${read.tile} (keyakinan awal ${(read.confidence * 100).toInt()}%; periksa hasil)"
            }
        }
        return "HASIL PEMBACAAN SCREENSHOT\n" + lines.joinToString("\n") +
            "\n\nCatatan: profil ini baru cocok untuk tata letak screenshot contoh. Belum melacak layar secara otomatis."
    }

    private fun sectionTitle(value: String) = TextView(this).apply {
        text = value
        textSize = 18f
        setPadding(0, 20, 0, 6)
    }

    private fun makeNumberInput(label: String) = EditText(this).apply {
        hint = label
        inputType = InputType.TYPE_CLASS_NUMBER
        setSingleLine(true)
        textSize = 16f
    }

    private fun makeTileGrid(
        buttons: MutableMap<DominoTile, Button>,
        onSelected: (DominoTile) -> Unit
    ): LinearLayout {
        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        DominoTile.fullSet().sortedWith(compareBy<DominoTile> { it.low }.thenBy { it.high })
            .chunked(4).forEach { group ->
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                group.forEach { tile ->
                    val button = Button(this).apply {
                        text = tile.toString()
                        textSize = 13f
                        isAllCaps = false
                        setOnClickListener { onSelected(tile) }
                    }
                    val params = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    params.setMargins(2, 2, 2, 2)
                    row.addView(button, params)
                    buttons[tile] = button
                }
                grid.addView(row)
            }
        return grid
    }

    private fun refreshObserved() {
        val visible = tracker.visibleTiles()
        status.text = "Terlihat: ${tracker.visibleCount()} / 28 • Belum diketahui: ${tracker.unknownCount()}"
        observedButtons.forEach { (tile, button) ->
            button.text = if (tile in visible) "✓ $tile" else tile.toString()
            button.alpha = if (tile in visible) 0.65f else 1.0f
        }
    }

    private fun refreshHand() {
        handStatus.text = "Kartu dipilih: ${selectedHand.size} / 7"
        handButtons.forEach { (tile, button) ->
            button.text = if (tile in selectedHand) "✓ $tile" else tile.toString()
            button.alpha = if (tile in selectedHand) 0.65f else 1.0f
        }
    }

    private fun analyzeHand() {
        if (selectedHand.isEmpty()) {
            result.text = "Pilih kartu di tangan terlebih dahulu."
            return
        }
        val left = leftInput.text.toString().toIntOrNull()
        val right = rightInput.text.toString().toIntOrNull()
        if (left == null || right == null || left !in 0..6 || right !in 0..6) {
            result.text = "Isi kedua ujung meja dengan angka 0 sampai 6."
            return
        }
        persistState(left, right)
        val chain = DominoChain()
        chain.placeFirst(DominoTile.of(left, right))
        val recommendation = MoveAnalyzer.recommendMove(selectedHand, chain)
        val unknown = MoveAnalyzer.remainingUnknown(tracker.visibleTiles(), selectedHand)
        val recommendationText = if (recommendation == null) "Belum ada kartu yang cocok dengan ujung meja."
        else {
            val side = recommendation.move.sides.joinToString(" atau ") {
                if (it == ChainSide.LEFT) "KIRI" else "KANAN"
            }
            "REKOMENDASI SIMULASI\n\n${recommendation.move.tile}\nSisi: $side\nAlasan: ${recommendation.reason}"
        }
        result.text = "$recommendationText\n\nKartu diketahui (meja + tangan simulasi): ${28 - unknown.size} / 28" +
            "\nKartu belum diketahui: ${unknown.size} / 28"
    }

    private fun persistState(left: Int? = null, right: Int? = null) {
        val editor = preferences.edit()
            .putString("visible_tiles", TileSetCodec.encode(tracker.visibleTiles()))
            .putString("own_hand", TileSetCodec.encode(selectedHand))
        if (left != null) editor.putInt("left_end", left)
        if (right != null) editor.putInt("right_end", right)
        editor.apply()
    }

    private fun restoreState() {
        runCatching {
            TileSetCodec.decode(preferences.getString("visible_tiles", "").orEmpty())
                .forEach { tracker.markVisible(it) }
            TileSetCodec.decode(preferences.getString("own_hand", "").orEmpty())
                .filter { it !in tracker.visibleTiles() }.take(7).forEach { selectedHand.add(it) }
            if (preferences.contains("left_end")) leftInput.setText(preferences.getInt("left_end", 0).toString())
            if (preferences.contains("right_end")) rightInput.setText(preferences.getInt("right_end", 0).toString())
        }.onFailure {
            tracker.clear()
            selectedHand.clear()
            preferences.edit().clear().apply()
        }
    }

    companion object {
        private const val REQUEST_IMAGE = 4102
    }
}
