package id.aruna.domino

import android.app.Activity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 24, 18, 24)
        }

        page.addView(TextView(this).apply {
            text = "ARUNA DOMINO PRO"
            textSize = 24f
            gravity = Gravity.CENTER
        })
        page.addView(TextView(this).apply {
            text = "Simulator latihan offline • rekomendasi satu kartu"
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 16)
        })

        status = TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 12)
        }
        page.addView(status)
        page.addView(sectionTitle("KARTU YANG SUDAH TERLIHAT"))
        page.addView(TextView(this).apply {
            text = "Masukkan kartu meja yang terlihat pada simulasi latihan ini."
            textSize = 13f
        })
        page.addView(makeTileGrid(observedButtons) { tile ->
            if (tile in tracker.visibleTiles()) {
                tracker.removeVisible(tile)
            } else {
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

        page.addView(sectionTitle("ANALISIS KARTU DI TANGAN"))
        page.addView(TextView(this).apply {
            text = "Pilih hingga 7 kartu untuk tangan simulasi. Isi angka ujung kiri dan kanan meja."
            textSize = 13f
        })
        handStatus = TextView(this).apply {
            textSize = 15f
            setPadding(0, 8, 0, 4)
        }
        page.addView(handStatus)
        page.addView(makeTileGrid(handButtons) { tile ->
            if (tile in selectedHand) {
                selectedHand.remove(tile)
            } else if (tile !in tracker.visibleTiles() && selectedHand.size < 7) {
                selectedHand.add(tile)
            }
            persistState()
            refreshHand()
        })

        val ends = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
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
        val grid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val tiles = DominoTile.fullSet().sortedWith(
            compareBy<DominoTile> { it.low }.thenBy { it.high }
        )
        tiles.chunked(4).forEach { group ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }
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
        status.text = "Terlihat: ${tracker.visibleCount()} / 28  •  Belum diketahui: ${tracker.unknownCount()}"
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
        val recommendationText = if (recommendation == null) {
            "Belum ada kartu yang cocok dengan ujung meja."
        } else {
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
                .filter { it !in tracker.visibleTiles() }
                .take(7)
                .forEach { selectedHand.add(it) }
            if (preferences.contains("left_end")) {
                leftInput.setText(preferences.getInt("left_end", 0).toString())
            }
            if (preferences.contains("right_end")) {
                rightInput.setText(preferences.getInt("right_end", 0).toString())
            }
        }.onFailure {
            tracker.clear()
            selectedHand.clear()
            preferences.edit().clear().apply()
        }
    }

}
