package id.aruna.domino.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import id.aruna.domino.ChainSide
import id.aruna.domino.DominoTile
import id.aruna.domino.GapleSession

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "ARUNA Domino Pro"
    ) {
        MaterialTheme {
            ArunaDesktopApp()
        }
    }
}

@Composable
private fun ArunaDesktopApp() {
    val session = remember { GapleSession() }
    var low by remember { mutableStateOf(0) }
    var high by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("Masukkan kartu tangan, lalu catat kartu yang terlihat di meja.") }

    val selectedTile = DominoTile.of(low, high)
    val hand = session.handTiles().sortedWith(compareBy<DominoTile> { it.low }.thenBy { it.high })
    val legalMoves = session.legalOwnMoves()

    fun update(messageText: String) {
        message = messageText
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("ARUNA DOMINO PRO", style = MaterialTheme.typography.h4)
            Text("Asisten pencatatan dan analisis Gaple double-six")
            Card(modifier = Modifier.fillMaxWidth(), elevation = 4.dp) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("STATUS RONDE", style = MaterialTheme.typography.h6)
                    Text("Giliran: ${session.playerNames[session.currentPlayer]}")
                    Text("Kartu di tangan: ${hand.size}   |   Kartu meja: ${session.chain.size}")
                    Text("Ujung meja: ${session.chain.leftEnd ?: "-"}  •  ${session.chain.rightEnd ?: "-"}")
                    Text("Kartu belum diketahui: ${session.unknownTiles().size}")
                    if (session.roundBlocked) Text("Ronde terkunci: empat pemain berturut-turut pass.")
                    if (session.roundFinished) Text("Ronde selesai: semua kartu tanganmu habis.")
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), elevation = 3.dp) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("KARTU TANGANMU", style = MaterialTheme.typography.h6)
                    Text(if (hand.isEmpty()) "Belum ada kartu yang dimasukkan." else hand.joinToString("     ") { it.toString() })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ValuePicker("Sisi A", low) { low = it }
                        ValuePicker("Sisi B", high) { high = it }
                        Button(onClick = {
                            runCatching { session.addToHand(selectedTile) }
                                .onSuccess { update("Kartu $selectedTile ditambahkan ke tangan.") }
                                .onFailure { update(it.message ?: "Kartu tidak dapat ditambahkan.") }
                        }) { Text("Tambah kartu") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = session.currentPlayer == 0 && !session.roundFinished &&
                                !session.roundBlocked && legalMoves.any { it.tile == selectedTile },
                            onClick = {
                                val move = legalMoves.firstOrNull { it.tile == selectedTile }
                                if (move != null) {
                                    val side = if (ChainSide.LEFT in move.sides) ChainSide.LEFT else ChainSide.RIGHT
                                    runCatching { session.playOwnTile(selectedTile, side) }
                                        .onSuccess { update("Kamu memainkan $selectedTile di sisi $side.") }
                                        .onFailure { update(it.message ?: "Langkah tidak valid.") }
                                }
                            }
                        ) { Text("Mainkan kartu terpilih") }
                        OutlinedButton(
                            enabled = session.currentPlayer == 0 && !session.roundFinished && !session.roundBlocked,
                            onClick = {
                                runCatching { session.passTurn() }
                                    .onSuccess { update("Kamu melakukan pass.") }
                                    .onFailure { update(it.message ?: "Tidak dapat pass.") }
                            }
                        ) { Text("Pass giliranmu") }
                    }
                    Text("Langkah legal: " + (legalMoves.joinToString("   ") { it.tile.toString() }.ifBlank { "Belum ada" }))
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), elevation = 3.dp) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("CATAT KARTU YANG TERLIHAT DI MEJA", style = MaterialTheme.typography.h6)
                    Text("Gunakan ini saat giliran pemain 2, 3, atau 4. Masukkan hanya kartu yang benar-benar terlihat.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ValuePicker("Sisi A", low) { low = it }
                        ValuePicker("Sisi B", high) { high = it }
                        Button(
                            enabled = session.currentPlayer in 1..3 && !session.roundFinished && !session.roundBlocked,
                            onClick = {
                                val side = when {
                                    session.chain.isEmpty -> ChainSide.LEFT
                                    ChainSide.LEFT in session.chain.legalSides(selectedTile) -> ChainSide.LEFT
                                    else -> ChainSide.RIGHT
                                }
                                runCatching { session.recordObservedPlay(selectedTile, side, session.currentPlayer) }
                                    .onSuccess { update("${session.playerNames[(session.currentPlayer + 3) % 4]} memainkan $selectedTile.") }
                                    .onFailure { update(it.message ?: "Kartu yang dicatat tidak valid.") }
                            }
                        ) { Text("Catat kartu meja") }
                    }
                    OutlinedButton(
                        enabled = session.currentPlayer in 1..3 && !session.roundFinished && !session.roundBlocked,
                        onClick = {
                            val player = session.currentPlayer
                            runCatching { session.passTurn() }
                                .onSuccess { update("${session.playerNames[player]} melakukan pass.") }
                                .onFailure { update(it.message ?: "Tidak dapat pass.") }
                        }
                    ) { Text("Catat pemain ini pass") }
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), elevation = 3.dp) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("RIWAYAT RONDE", style = MaterialTheme.typography.h6)
                    val history = session.history()
                    if (history.isEmpty()) Text("Belum ada aktivitas.")
                    history.forEach { event ->
                        val detail = if (event.type.name == "PASS") "pass" else "memainkan ${event.tile}"
                        Text("${session.playerNames[event.playerIndex]}: $detail")
                    }
                    OutlinedButton(onClick = {
                        session.resetRound()
                        update("Ronde baru dimulai. Masukkan kembali kartu tanganmu.")
                    }) { Text("Mulai ronde baru") }
                }
            }

            Text(message, style = MaterialTheme.typography.body2)
            Text(
                "Catatan: ARUNA hanya menghitung kartu tangan yang kamu masukkan dan kartu yang terkonfirmasi terlihat. Kartu tersembunyi lawan tidak ditebak.",
                style = MaterialTheme.typography.caption
            )
        }
    }
}

@Composable
private fun ValuePicker(label: String, value: Int, onSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.caption)
        OutlinedButton(onClick = { expanded = true }) {
            Text(value.toString())
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            (0..6).forEach { number ->
                DropdownMenuItem(onClick = {
                    onSelected(number)
                    expanded = false
                }) {
                    Text(number.toString())
                }
            }
        }
    }
}
