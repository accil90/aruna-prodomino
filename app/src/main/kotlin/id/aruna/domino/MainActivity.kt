package id.aruna.domino

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Intent
import android.content.IntentFilter
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Build
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

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
    private lateinit var observerStatus: TextView
    private lateinit var observerReport: TextView
    private var observerReceiverRegistered = false
    private var observerActive = false
    private var observerFrames = 0L
    private var observerWidth = 0
    private var observerHeight = 0
    private var observerDensityDpi = 0
    private var observerStartedAt = 0L
    private var observerLastFrameAt = 0L

    private val observerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ScreenObserverService.ACTION_STATUS) return
            val active = intent.getBooleanExtra(ScreenObserverService.EXTRA_ACTIVE, false)
            observerActive = active
            observerFrames = intent.getLongExtra(ScreenObserverService.EXTRA_FRAMES, 0L)
            observerWidth = intent.getIntExtra(ScreenObserverService.EXTRA_WIDTH, 0)
            observerHeight = intent.getIntExtra(ScreenObserverService.EXTRA_HEIGHT, 0)
            observerDensityDpi = intent.getIntExtra(ScreenObserverService.EXTRA_DENSITY_DPI, 0)
            observerStartedAt = intent.getLongExtra(ScreenObserverService.EXTRA_STARTED_AT, 0L)
            observerLastFrameAt = intent.getLongExtra(ScreenObserverService.EXTRA_LAST_FRAME_AT, 0L)
            observerStatus.text = if (active) {
                "Pengamat aktif • $observerFrames frame diterima\\nPengenalan kartu belum diaktifkan."
            } else {
                "Pengamat tidak aktif."
            }
            refreshObserverReport()
        }
    }

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
            text = "Pencatat keping dan analisis langkah Gaple"
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 16)
        })

        page.addView(sectionTitle("PENGAMAT LAYAR"))
        observerStatus = TextView(this).apply {
            text = "Pengamat tidak aktif. Mulai untuk meminta izin tangkap layar Android."
            textSize = 14f
            setPadding(0, 6, 0, 8)
        }
        page.addView(observerStatus)
        val observerControls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        observerControls.addView(Button(this).apply {
            text = "Mulai pengamat"
            setOnClickListener { requestObserverPermission() }
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        observerControls.addView(Button(this).apply {
            text = "Hentikan"
            setOnClickListener {
                startService(Intent(this@MainActivity, ScreenObserverService::class.java).apply {
                    action = ScreenObserverService.ACTION_STOP
                })
                observerStatus.text = "Pengamat dihentikan."
            }
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        page.addView(observerControls)
        page.addView(sectionTitle("HASIL PENGAMATAN"))
        observerReport = TextView(this).apply {
            textSize = 13f
            setPadding(10, 10, 10, 10)
            text = "Belum ada laporan. Mulai pengamat untuk mengumpulkan data layar."
            setBackgroundColor(0xFFEFEFEF.toInt())
        }
        page.addView(observerReport)
        page.addView(Button(this).apply {
            text = "Salin laporan untuk dikirim"
            setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Laporan ARUNA DOMINO", buildObserverReport()))
                Toast.makeText(this@MainActivity, "Laporan disalin. Tempelkan ke chat.", Toast.LENGTH_SHORT).show()
            }
        })
        page.addView(TextView(this).apply {
            text = "Tahap ini hanya menangkap frame layar setelah izin diberikan. Gambar tidak disimpan atau dikirim; pengenalan kartu dan giliran akan ditambahkan pada tahap berikutnya."
            textSize = 12f
            setPadding(0, 4, 0, 12)
        })

        status = TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 12)
        }
        page.addView(status)
        page.addView(sectionTitle("KARTU YANG SUDAH TERLIHAT"))
        page.addView(TextView(this).apply {
            text = "Tandai hanya kartu yang benar-benar terlihat di meja atau sudah terbuka."
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
            text = "Pilih kartu milikmu (maksimal 7). Masukkan angka ujung kiri dan kanan rantai di meja."
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
            text = "Analisis langkah"
            setOnClickListener { analyzeHand() }
        })
        result = TextView(this).apply {
            textSize = 15f
            setPadding(0, 10, 0, 24)
        }
        page.addView(result)

        setContentView(ScrollView(this).apply { addView(page) })
        registerObserverReceiver()
        restoreState()
        refreshObserved()
        refreshHand()
    }

    private fun buildObserverReport(): String {
        val started = if (observerStartedAt > 0L) {
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date(observerStartedAt))
        } else "Belum dimulai"
        val lastFrame = if (observerLastFrameAt > 0L) {
            java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date(observerLastFrameAt))
        } else "Belum ada"
        return """
            LAPORAN PENGAMAT ARUNA DOMINO
            Status: ${if (observerActive) "AKTIF" else "TIDAK AKTIF"}
            Mulai pengamatan: $started
            Frame diterima: $observerFrames
            Resolusi layar: ${if (observerWidth > 0 && observerHeight > 0) "${observerWidth}x${observerHeight}" else "Belum tersedia"}
            Kepadatan layar: ${if (observerDensityDpi > 0) "$observerDensityDpi dpi" else "Belum tersedia"}
            Frame terakhir: $lastFrame

            PENGENALAN DOMINO: BELUM TERPASANG
            Kartu tangan: Belum dibaca otomatis
            Kartu di meja: Belum dibaca otomatis
            Ujung kiri/kanan: Belum dibaca otomatis
            Giliran pemain: Belum dideteksi

            Privasi: Frame hanya diterima lalu langsung ditutup; gambar tidak disimpan atau diunggah.
        """.trimIndent()
    }

    private fun refreshObserverReport() {
        observerReport.text = buildObserverReport()
    }

    private fun registerObserverReceiver() {
        val filter = IntentFilter(ScreenObserverService.ACTION_STATUS)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(observerReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(observerReceiver, filter)
        }
        observerReceiverRegistered = true
    }

    private fun requestObserverPermission() {
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_SCREEN_CAPTURE)
    }

    @Deprecated("Uses the compatible Activity result callback for the current minSdk.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_SCREEN_CAPTURE) return
        if (resultCode != RESULT_OK || data == null) {
            observerStatus.text = "Izin tangkap layar tidak diberikan."
            return
        }
        val serviceIntent = Intent(this, ScreenObserverService::class.java).apply {
            action = ScreenObserverService.ACTION_START
            putExtra(ScreenObserverService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenObserverService.EXTRA_PERMISSION_DATA, data)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        observerStatus.text = "Memulai pengamat layar…"
    }

    override fun onDestroy() {
        if (observerReceiverRegistered) {
            runCatching { unregisterReceiver(observerReceiver) }
            observerReceiverRegistered = false
        }
        super.onDestroy()
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
        val moves = MoveAnalyzer.legalMoves(selectedHand, chain)
        val unknown = MoveAnalyzer.remainingUnknown(tracker.visibleTiles(), selectedHand)
        val moveSummary = if (moves.isEmpty()) {
            "Tidak ada kartu pilihan yang cocok dengan ujung meja."
        } else {
            "Kartu yang sah dimainkan:\n" + moves.joinToString("\n") { move ->
                val side = move.sides.joinToString(" dan ") {
                    if (it == ChainSide.LEFT) "kiri" else "kanan"
                }
                "• ${move.tile} — $side"
            }
        }
        result.text = "$moveSummary\n\nKartu yang sudah diketahui (meja + tanganmu): ${28 - unknown.size} / 28" +
            "\nKartu yang belum diketahui: ${unknown.size} / 28"
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

    companion object {
        private const val REQUEST_SCREEN_CAPTURE = 7302
    }
}
