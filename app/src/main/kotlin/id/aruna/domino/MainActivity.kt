package id.aruna.domino

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private val tracker = VisibleTileTracker()
    private lateinit var status: TextView
    private val tileButtons = linkedMapOf<DominoTile, Button>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 28, 20, 20)
        }

        page.addView(TextView(this).apply {
            text = "ARUNA DOMINO PRO"
            textSize = 24f
            gravity = Gravity.CENTER
        })

        page.addView(TextView(this).apply {
            text = "Pencatat keping Gaple • Set double-six"
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 18)
        })

        status = TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 16)
        }
        page.addView(status)

        page.addView(TextView(this).apply {
            text = "Sentuh keping untuk menandai bahwa keping itu sudah terlihat. Sentuh lagi untuk membatalkan."
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 12)
        })

        val tileGrid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val allTiles = DominoTile.fullSet().sortedWith(
            compareBy<DominoTile> { it.low }.thenBy { it.high }
        )

        allTiles.chunked(4).forEach { rowTiles ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            rowTiles.forEach { tile ->
                val tileButton = Button(this).apply {
                    text = tile.toString()
                    textSize = 14f
                    isAllCaps = false
                    setOnClickListener {
                        if (tracker.visibleTiles().contains(tile)) {
                            tracker.removeVisible(tile)
                        } else {
                            tracker.markVisible(tile)
                        }
                        refreshTiles()
                    }
                }
                val params = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
                params.setMargins(3, 3, 3, 3)
                row.addView(tileButton, params)
                tileButtons[tile] = tileButton
            }
            tileGrid.addView(row)
        }

        val scroll = ScrollView(this).apply {
            addView(tileGrid)
        }
        page.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        page.addView(Button(this).apply {
            text = "Bersihkan semua tanda"
            setOnClickListener {
                tracker.clear()
                refreshTiles()
            }
        })

        setContentView(page)
        refreshTiles()
    }

    private fun refreshTiles() {
        val visible = tracker.visibleTiles()
        tileButtons.forEach { (tile, button) ->
            button.text = if (tile in visible) "✓ $tile" else tile.toString()
            button.alpha = if (tile in visible) 0.65f else 1.0f
        }
        status.text = "Terlihat: ${tracker.visibleCount()} / 28    •    Belum diketahui: ${tracker.unknownCount()}"
    }
}
