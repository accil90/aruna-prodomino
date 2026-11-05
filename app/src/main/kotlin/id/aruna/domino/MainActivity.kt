package id.aruna.domino

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private val tracker = VisibleTileTracker()
    private val chain = DominoChain()
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 36, 28, 28)
        }

        val title = TextView(this).apply {
            text = "ARUNA DOMINO PRO"
            textSize = 24f
            gravity = Gravity.CENTER
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "Fondasi analisis Gaple • Double-six • 4 pemain"
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 24)
        }
        root.addView(subtitle)

        status = TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 20)
        }
        root.addView(status)

        root.addView(Button(this).apply {
            text = "Catat contoh keping 6|6"
            setOnClickListener {
                tracker.markVisible(DominoTile.of(6, 6))
                updateStatus()
            }
        })

        root.addView(Button(this).apply {
            text = "Bersihkan sesi"
            setOnClickListener {
                tracker.clear()
                chain.clear()
                updateStatus()
            }
        })

        root.addView(TextView(this).apply {
            text = "Tahap ini adalah kerangka awal. Input keping, papan permainan, OCR, dan pengamatan layar akan ditambahkan bertahap."
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 0)
        })

        setContentView(root)
        updateStatus()
    }

    private fun updateStatus() {
        status.text = "Keping terlihat: ${tracker.visibleCount()} dari 28\n" +
            "Belum diketahui: ${tracker.unknownCount()}\n" +
            "Keping di rantai meja: ${chain.size}"
    }
}
