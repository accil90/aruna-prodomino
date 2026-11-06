package id.aruna.domino

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * A self-contained offline practice table. It uses only generated local tiles;
 * it never reads another app's screen or exposes any opponent's hidden hand.
 */
class OfflineSimulationActivity : Activity() {
    private val chain = DominoChain()
    private val deck = mutableListOf<DominoTile>()
    private val hand = linkedSetOf<DominoTile>()
    private var selectedTile: DominoTile? = null
    private var currentTurn = "Giliran latihan"
    private lateinit var boardView: TextView
    private lateinit var handView: LinearLayout
    private lateinit var analysisView: TextView
    private lateinit var recommendationView: TextView
    private lateinit var turnView: TextView
    private lateinit var leftButton: Button
    private lateinit var rightButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 24)
        }
        page.addView(TextView(this).apply {
            text = "ARUNA DOMINO PRO • SIMULASI OFFLINE"
            textSize = 20f
            gravity = Gravity.CENTER
        })
        page.addView(TextView(this).apply {
            text = "Meja latihan lokal — semua kartu dibuat di perangkat"
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, 6, 0, 14)
        })
        turnView = TextView(this).apply { textSize = 15f; setPadding(0, 8, 0, 8) }
        page.addView(turnView)
        page.addView(section("RANTAI KARTU DI MEJA"))
        boardView = TextView(this).apply {
            textSize = 18f
            setPadding(12, 12, 12, 12)
            setBackgroundColor(0xFFE8F0E8.toInt())
        }
        page.addView(boardView)
        page.addView(section("KARTU LATIHAN (7 KEPING)"))
        handView = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(handView)
        page.addView(section("REKOMENDASI OTOMATIS • SIMULASI"))
        recommendationView = TextView(this).apply {
            textSize = 20f
            setPadding(12, 14, 12, 14)
            setBackgroundColor(0xFFE7F0FF.toInt())
            text = "Menyiapkan rekomendasi..."
        }
        page.addView(recommendationView)
        page.addView(section("HASIL ANALISIS SIMULASI"))
        analysisView = TextView(this).apply {
            textSize = 15f
            setPadding(10, 10, 10, 10)
            setBackgroundColor(0xFFF1F1F1.toInt())
            text = "Tekan ANALISIS untuk melihat kartu yang cocok dengan ujung meja."
        }
        page.addView(analysisView)
        val sideRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        leftButton = Button(this).apply {
            text = "Mainkan KIRI"
            setOnClickListener { playSelected(ChainSide.LEFT) }
        }
        rightButton = Button(this).apply {
            text = "Mainkan KANAN"
            setOnClickListener { playSelected(ChainSide.RIGHT) }
        }
        sideRow.addView(leftButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        sideRow.addView(rightButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        page.addView(sideRow)
        page.addView(Button(this).apply {
            text = "ANALISIS"
            setOnClickListener { analyze() }
        })
        page.addView(Button(this).apply {
            text = "Lewati giliran simulasi"
            setOnClickListener {
                if (currentTurn == "Giliran simulasi") {
                    currentTurn = "Giliran latihan"
                    refresh()
                } else {
                    Toast.makeText(this@OfflineSimulationActivity, "Selesaikan langkah latihan terlebih dahulu.", Toast.LENGTH_SHORT).show()
                }
            }
        })
        page.addView(Button(this).apply {
            text = "Buat sesi latihan baru"
            setOnClickListener { newRound() }
        })
        page.addView(Button(this).apply {
            text = "Kembali"
            setOnClickListener { finish() }
        })
        setContentView(ScrollView(this).apply { addView(page) })
        newRound()
    }

    private fun section(label: String) = TextView(this).apply {
        text = label
        textSize = 16f
        setPadding(0, 18, 0, 6)
    }

    private fun newRound() {
        chain.clear()
        hand.clear()
        deck.clear()
        selectedTile = null
        currentTurn = "Giliran latihan"
        deck.addAll(DominoTile.fullSet().shuffled())
        val opening = deck.removeAt(0)
        chain.placeFirst(opening)
        repeat(7) { hand.add(deck.removeAt(0)) }
        // Ensure the sample hand has at least one legal practice move.
        if (MoveAnalyzer.legalMoves(hand, chain).isEmpty()) {
            val replacement = deck.firstOrNull { it.matches(chain.leftEnd!!) || it.matches(chain.rightEnd!!) }
            if (replacement != null) {
                val removed = hand.last()
                hand.remove(removed)
                deck.add(removed)
                deck.remove(replacement)
                hand.add(replacement)
            }
        }
        analysisView.text = "Sesi baru siap. Pilih salah satu kartu, lalu tekan ANALISIS."
        refresh()
    }

    private fun analyze() {
        if (currentTurn != "Giliran latihan") {
            analysisView.text = "Giliran latihan belum aktif."
            return
        }
        val moves = MoveAnalyzer.legalMoves(hand, chain)
        val selected = selectedTile
        val relevant = if (selected == null) moves else moves.filter { it.tile == selected }
        analysisView.text = if (relevant.isEmpty()) {
            if (selected == null) "Tidak ada langkah yang cocok pada sesi ini."
            else "Kartu $selected tidak cocok dengan ujung meja."
        } else {
            "Kartu yang cocok dalam simulasi:\n" + relevant.joinToString("\n") { move ->
                val sides = move.sides.joinToString(" / ") {
                    if (it == ChainSide.LEFT) "KIRI" else "KANAN"
                }
                "• ${move.tile} — $sides"
            } + "\n\nPilih kartu dan sisi untuk menguji perubahan rantai."
        }
        updateSideButtons()
    }

    private fun playSelected(side: ChainSide) {
        if (currentTurn != "Giliran latihan") {
            Toast.makeText(this, "Sekarang giliran simulasi. Tekan lewati giliran untuk kembali.", Toast.LENGTH_SHORT).show()
            return
        }
        val tile = selectedTile
        if (tile == null || tile !in hand) {
            Toast.makeText(this, "Pilih kartu latihan terlebih dahulu.", Toast.LENGTH_SHORT).show()
            return
        }
        val legalSides = chain.legalSides(tile)
        if (side !in legalSides) {
            Toast.makeText(this, "Kartu $tile tidak cocok di sisi tersebut.", Toast.LENGTH_SHORT).show()
            return
        }
        chain.place(tile, side)
        hand.remove(tile)
        selectedTile = null
        currentTurn = "Giliran simulasi"
        analysisView.text = "Langkah simulasi tercatat: $tile di sisi ${if (side == ChainSide.LEFT) "KIRI" else "KANAN"}.\nTidak ada kartu lawan yang dibaca atau ditebak."
        refresh()
    }

    private fun refresh() {
        turnView.text = "STATUS: $currentTurn"
        updateAutomaticRecommendation()
        boardView.text = "Rantai: " + chain.snapshot().joinToString("  —  ") +
            "\nUjung kiri: ${chain.leftEnd}     Ujung kanan: ${chain.rightEnd}"
        handView.removeAllViews()
        hand.toList().chunked(4).forEach { rowTiles ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            rowTiles.forEach { tile ->
                val button = Button(this).apply {
                    text = if (tile == selectedTile) "✓ $tile" else tile.toString()
                    isAllCaps = false
                    textSize = 13f
                    setOnClickListener {
                        selectedTile = if (selectedTile == tile) null else tile
                        refresh()
                        analyze()
                    }
                }
                row.addView(button, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            }
            handView.addView(row)
        }
        updateSideButtons()
    }

    /**
     * Shows exactly one legal move in the offline practice simulator.
     * The choice is derived only from the locally generated practice hand
     * and the public simulated chain.
     */
    private fun updateAutomaticRecommendation() {
        if (currentTurn != "Giliran latihan") {
            recommendationView.text = "Menunggu giliran latihan berikutnya."
            return
        }
        val move = MoveAnalyzer.legalMoves(hand, chain).firstOrNull()
        if (move == null) {
            recommendationView.text = "Belum ada kartu yang cocok."
            return
        }
        val sides = move.sides.joinToString(" / ") {
            if (it == ChainSide.LEFT) "KIRI" else "KANAN"
        }
        recommendationView.text = "KARTU: ${move.tile}\nPOSISI: $sides"
    }

    private fun updateSideButtons() {
        val tile = selectedTile
        val sides = if (tile == null || currentTurn != "Giliran latihan") emptySet() else chain.legalSides(tile)
        leftButton.isEnabled = ChainSide.LEFT in sides
        rightButton.isEnabled = ChainSide.RIGHT in sides
    }
}
