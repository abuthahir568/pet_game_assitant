package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.model.HomeLayout
import com.example.data.model.PetActionState
import com.example.data.model.PetProfile
import com.example.data.model.PetSpecies
import com.example.data.model.PlacedFurniture
import com.example.data.model.parseHexColor
import com.example.ui.components.Animated3DPetStage
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.math.roundToInt

@Composable
fun ArcadeAndHomeBuilderScreen(
    profile: PetProfile,
    savedHomeLayout: HomeLayout?,
    initialTab: Int = 0,
    onWinMiniGame: (gameTitle: String, coins: Long, xp: Long) -> Unit,
    onSaveHomeLayout: (theme: String, wallHex: String, floorHex: String, lighting: String, items: List<PlacedFurniture>) -> Unit
) {
    var activeTab by remember(initialTab) { mutableIntStateOf(initialTab) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = activeTab) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("Mini-Games & Toys") }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("3D Home Builder") }
            )
        }

        if (activeTab == 0) {
            MiniGamesHubView(profile = profile, onWinMiniGame = onWinMiniGame)
        } else {
            Isometric3DHomeBuilderView(
                profile = profile,
                savedLayout = savedHomeLayout,
                onSaveHomeLayout = onSaveHomeLayout
            )
        }
    }
}

@Composable
private fun MiniGamesHubView(
    profile: PetProfile,
    onWinMiniGame: (String, Long, Long) -> Unit
) {
    var selectedGame by remember { mutableIntStateOf(0) } // 0=Menu, 1=Fetch, 2=Memory, 3=StarCatch, 4=Dance
    val species = PetSpecies.fromId(profile.animalType)

    when (selectedGame) {
        0 -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("${profile.petName}'s Toy Box & Arcade", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Play interactive games together to earn virtual PetCoins & level up your Friendship bond!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val games = listOf(
                    Triple(1, "⚽ 3D Fetch & Sprint", "Throw bouncing balls in the meadow and watch ${profile.petName} sprint & catch! (+35 🪙)"),
                    Triple(2, "🧩 Bestie Memory Match", "Flip enchanted pet cards to match all pairs in minimum moves! (+45 🪙)"),
                    Triple(3, "🛸 Catch Flying Treats", "Tap falling starlight treats before they hit the floor! (+40 🪙)"),
                    Triple(4, "💃 Rhythm Dance Challenge", "Hit the glowing dance beats in sequence with ${profile.petName}! (+50 🪙)")
                )

                games.forEach { (id, title, desc) ->
                    item {
                        Card(
                            onClick = { selectedGame = id },
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("minigame_card_$id")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(title, style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(onClick = { selectedGame = id }) {
                                    Text("Play")
                                }
                            }
                        }
                    }
                }
            }
        }

        1 -> {
            // Game 1: Interactive 3D Fetch the Ball
            var catches by remember { mutableIntStateOf(0) }
            var ballX by remember { mutableFloatStateOf(0.5f) }
            var ballY by remember { mutableFloatStateOf(0.25f) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selectedGame = 0 }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Arcade")
                    }
                    Text("⚽ Fetch the Ball ($catches / 5 Catches)", style = MaterialTheme.typography.titleLarge)
                }
                Text("Tap anywhere on the green park lawn to toss the ball for ${profile.petName}!")

                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(Color(0xFFBAE6FD), Color(0xFF86EFAC))))
                            .pointerInput(catches) {
                                detectTapGestures { offset ->
                                    ballX = (offset.x / size.width).coerceIn(0.15f, 0.85f)
                                    ballY = (offset.y / size.height).coerceIn(0.15f, 0.78f)
                                    val next = catches + 1
                                    catches = next
                                    if (next >= 5) {
                                        onWinMiniGame("3D Fetch the Ball", 35L, 25L)
                                        selectedGame = 0
                                    }
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val bx = size.width * ballX
                            val by = size.height * ballY
                            drawCircle(color = Color(0xFFF43F5E), radius = 34f, center = Offset(bx, by))
                            drawCircle(color = Color.White, radius = 11f, center = Offset(bx - 9f, by - 9f))
                        }

                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp)
                        ) {
                            Animated3DPetStage(
                                species = species,
                                furColorHex = profile.furColorHex,
                                eyeColorHex = profile.eyeColorHex,
                                collarColorHex = profile.collarColorHex,
                                equippedHatId = profile.equippedHatId,
                                equippedOutfitId = profile.equippedOutfitId,
                                actionState = PetActionState.PLAYING,
                                mouthOpenAmount = 0.3f,
                                isListening = false,
                                onPetTapped = {}
                            )
                        }
                    }
                }
            }
        }

        2 -> {
            // Game 2: Memory Match Puzzle
            val symbols = remember { listOf("🐶", "🐱", "🦄", "🐉", "🍰", "⚽", "🐶", "🐱", "🦄", "🐉", "🍰", "⚽").shuffled() }
            val revealed = remember { mutableStateListOf<Int>() }
            val matched = remember { mutableStateListOf<Int>() }

            LaunchedEffect(revealed.size) {
                if (revealed.size == 2) {
                    val first = revealed[0]
                    val second = revealed[1]
                    if (symbols[first] == symbols[second]) {
                        matched.add(first)
                        matched.add(second)
                        revealed.clear()
                        if (matched.size == symbols.size) {
                            onWinMiniGame("Memory Puzzle", 45L, 30L)
                            selectedGame = 0
                        }
                    } else {
                        delay(650L)
                        revealed.clear()
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selectedGame = 0 }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text("🧩 Bestie Memory Puzzle", style = MaterialTheme.typography.titleLarge)
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(symbols) { idx, emoji ->
                        val isOpen = idx in revealed || idx in matched
                        Card(
                            onClick = {
                                if (!isOpen && revealed.size < 2) {
                                    revealed.add(idx)
                                }
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isOpen) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.height(88.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (isOpen) emoji else "🐾",
                                    style = MaterialTheme.typography.headlineLarge
                                )
                            }
                        }
                    }
                }
            }
        }

        3 -> {
            // Game 3: Catch Flying Treats
            var caughtCount by remember { mutableIntStateOf(0) }
            var treatX by remember { mutableFloatStateOf(0.45f) }
            var treatY by remember { mutableFloatStateOf(0.15f) }

            LaunchedEffect(caughtCount) {
                while (caughtCount < 6) {
                    delay(850L)
                    treatX = ((15..85).random() / 100f)
                    treatY = ((15..70).random() / 100f)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selectedGame = 0 }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text("🛸 Catch Flying Treats ($caughtCount / 6)", style = MaterialTheme.typography.titleLarge)
                }
                Text("Tap the teleporting starlight cupcake to feed ${profile.petName}!")

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(Brush.verticalGradient(listOf(Color(0xFF312E81), Color(0xFF4F46E5))))
                ) {
                    Surface(
                        onClick = {
                            val next = caughtCount + 1
                            caughtCount = next
                            if (next >= 6) {
                                onWinMiniGame("Catch Flying Treats", 40L, 28L)
                                selectedGame = 0
                            }
                        },
                        shape = CircleShape,
                        color = Color(0xFFFDE047),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(
                                x = (treatX * 240).dp,
                                y = (treatY * 260).dp
                            )
                            .size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🧁", style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
            }
        }

        else -> {
            // Game 4: Rhythm Dance Challenge
            val moves = listOf("⬆️ Jump", "⬅️ Spin Left", "➡️ Spin Right", "✨ Star Pose")
            var stepIndex by remember { mutableIntStateOf(0) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedGame = 0 }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text("💃 Dance Challenge (${stepIndex}/6)", style = MaterialTheme.typography.titleLarge)
                }

                val targetMove = moves[stepIndex % moves.size]
                Text("Hit the beat: $targetMove!", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)

                Box(modifier = Modifier.size(190.dp)) {
                    Animated3DPetStage(
                        species = species,
                        furColorHex = profile.furColorHex,
                        eyeColorHex = profile.eyeColorHex,
                        collarColorHex = profile.collarColorHex,
                        equippedHatId = profile.equippedHatId,
                        equippedOutfitId = profile.equippedOutfitId,
                        actionState = PetActionState.DANCING,
                        mouthOpenAmount = 0.35f,
                        isListening = false,
                        onPetTapped = {}
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    moves.forEach { move ->
                        FilledTonalButton(
                            onClick = {
                                if (move == targetMove) {
                                    val next = stepIndex + 1
                                    stepIndex = next
                                    if (next >= 6) {
                                        onWinMiniGame("Rhythm Dance Challenge", 50L, 35L)
                                        selectedGame = 0
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(move.take(2))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Isometric3DHomeBuilderView(
    profile: PetProfile,
    savedLayout: HomeLayout?,
    onSaveHomeLayout: (String, String, String, String, List<PlacedFurniture>) -> Unit
) {
    var envTheme by remember(savedLayout) { mutableStateOf(savedLayout?.environmentTheme ?: "COZY_HOUSE") }
    var wallHex by remember(savedLayout) { mutableStateOf(savedLayout?.wallColorHex ?: "#E0E7FF") }
    var floorHex by remember(savedLayout) { mutableStateOf(savedLayout?.floorColorHex ?: "#FDE68A") }
    var lightingMode by remember(savedLayout) { mutableStateOf(savedLayout?.lightingMode ?: "DAYLIGHT") }
    var selectedItemUid by remember { mutableStateOf<String?>(null) }

    val placedItems = remember(savedLayout) {
        val list = mutableStateListOf<PlacedFurniture>()
        savedLayout?.placedItems?.forEach { raw ->
            list.add(
                PlacedFurniture(
                    uid = (raw["uid"] as? String) ?: UUID.randomUUID().toString().take(8),
                    itemId = (raw["itemId"] as? String) ?: "furn_sofa",
                    name = (raw["name"] as? String) ?: "Furniture",
                    emoji = (raw["emoji"] as? String) ?: "🛋️",
                    gridX = (raw["gridX"] as? Number)?.toLong() ?: 2L,
                    gridY = (raw["gridY"] as? Number)?.toLong() ?: 2L,
                    rotationDeg = (raw["rotationDeg"] as? Number)?.toLong() ?: 0L,
                    scaleFactor = (raw["scaleFactor"] as? Number)?.toDouble() ?: 1.0
                )
            )
        }
        if (list.isEmpty()) {
            list.add(PlacedFurniture("init_1", "furn_bed", "Cloud Bed", "🛏️", 1L, 1L, 0L, 1.0))
            list.add(PlacedFurniture("init_2", "furn_sofa", "Plush Sofa", "🛋️", 4L, 1L, 0L, 1.0))
            list.add(PlacedFurniture("init_3", "furn_plant", "Sakura Tree", "🌸", 0L, 3L, 0L, 1.0))
        }
        list
    }

    val furniturePalette = listOf(
        Triple("furn_bed", "Bed", "🛏️"),
        Triple("furn_sofa", "Sofa", "🛋️"),
        Triple("furn_lamp", "Lamp", "🏮"),
        Triple("furn_plant", "Tree", "🌸"),
        Triple("furn_pool", "Pool", "🏊"),
        Triple("furn_slide", "Slide", "🛝"),
        Triple("furn_table", "Kitchen", "🍽️")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("3D Pet Sanctuary Builder", style = MaterialTheme.typography.titleLarge)
            Row {
                IconButton(
                    onClick = {
                        if (placedItems.isNotEmpty()) {
                            placedItems.removeAt(placedItems.lastIndex)
                            selectedItemUid = null
                        }
                    },
                    modifier = Modifier.testTag("home_builder_undo_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo Placement")
                }
                Button(
                    onClick = {
                        onSaveHomeLayout(envTheme, wallHex, floorHex, lightingMode, placedItems.toList())
                    },
                    modifier = Modifier.testTag("home_builder_save_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save")
                }
            }
        }

        // Environment Theme Chooser (House, Apartment, Garden, Fantasy Castle)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                Triple("COZY_HOUSE", "🏠 House", "#E0E7FF" to "#FDE68A"),
                Triple("SKY_APARTMENT", "🏙️ Apartment", "#DBEAFE" to "#CBD5E1"),
                Triple("SUNLIT_GARDEN", "🌿 Garden", "#BAE6FD" to "#86EFAC"),
                Triple("FANTASY_CASTLE", "🏰 Castle", "#F3E8FF" to "#DDD6FE")
            ).forEach { (id, label, colors) ->
                FilterChip(
                    selected = envTheme == id,
                    onClick = {
                        envTheme = id
                        wallHex = colors.first
                        floorHex = colors.second
                    },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        // Interactive 6x4 Grid-Snapped 3D Isometric Room Stage
        val wallColor = parseHexColor(wallHex)
        val floorColor = parseHexColor(floorHex)
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to wallColor,
                            0.42f to wallColor.copy(alpha = 0.7f),
                            0.43f to floorColor.copy(alpha = 0.75f),
                            1.0f to floorColor
                        )
                    )
                    .pointerInput(selectedItemUid) {
                        detectTapGestures { offset ->
                            // Snap selected furniture to tapped 6x4 grid cell!
                            val targetUid = selectedItemUid ?: return@detectTapGestures
                            val gx = ((offset.x / size.width) * 6f).toInt().coerceIn(0, 5).toLong()
                            val gy = ((offset.y / size.height) * 4f).toInt().coerceIn(0, 3).toLong()
                            val idx = placedItems.indexOfFirst { it.uid == targetUid }
                            if (idx >= 0) {
                                placedItems[idx] = placedItems[idx].copy(gridX = gx, gridY = gy)
                            }
                        }
                    }
            ) {
                // Placed furniture items
                placedItems.forEach { item ->
                    val isSel = item.uid == selectedItemUid
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = (item.gridX * 52).toInt().dp.roundToPx(),
                                    y = (item.gridY * 52).toInt().dp.roundToPx()
                                )
                            }
                            .size(50.dp)
                            .rotate(item.rotationDeg.toFloat())
                            .scale(item.scaleFactor.toFloat())
                            .clip(CircleShape)
                            .background(if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f) else Color.Transparent)
                            .clickable { selectedItemUid = item.uid },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(item.emoji, style = MaterialTheme.typography.headlineMedium)
                    }
                }

                // Pet walking happily around the custom home!
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    Animated3DPetStage(
                        species = PetSpecies.fromId(profile.animalType),
                        furColorHex = profile.furColorHex,
                        eyeColorHex = profile.eyeColorHex,
                        collarColorHex = profile.collarColorHex,
                        equippedHatId = profile.equippedHatId,
                        equippedOutfitId = profile.equippedOutfitId,
                        actionState = PetActionState.WALKING,
                        mouthOpenAmount = 0.1f,
                        isListening = false,
                        onPetTapped = {}
                    )
                }
            }
        }

        Text(
            "Tap an item below to place it, then tap it in the room to select, move on the grid, rotate, or resize:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Selected item transform toolbar (Rotate, Resize, Remove)
        val activeIdx = placedItems.indexOfFirst { it.uid == selectedItemUid }
        if (activeIdx >= 0) {
            val currentItem = placedItems[activeIdx]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        placedItems[activeIdx] = currentItem.copy(rotationDeg = (currentItem.rotationDeg + 45L) % 360L)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rotate")
                }
                OutlinedButton(
                    onClick = {
                        val nextScale = if (currentItem.scaleFactor >= 1.35) 0.85 else currentItem.scaleFactor + 0.18
                        placedItems[activeIdx] = currentItem.copy(scaleFactor = nextScale)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ZoomIn, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Resize")
                }
                OutlinedButton(
                    onClick = {
                        placedItems.removeAt(activeIdx)
                        selectedItemUid = null
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Remove")
                }
            }
        }

        // Furniture Add Palette
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            furniturePalette.forEach { (id, label, emoji) ->
                FilledTonalButton(
                    onClick = {
                        if (placedItems.size < 25) {
                            val newUid = UUID.randomUUID().toString().take(8)
                            placedItems.add(
                                PlacedFurniture(
                                    uid = newUid,
                                    itemId = id,
                                    name = label,
                                    emoji = emoji,
                                    gridX = (1..4).random().toLong(),
                                    gridY = (1..2).random().toLong()
                                )
                            )
                            selectedItemUid = newUid
                        }
                    }
                ) {
                    Text("$emoji $label")
                }
            }
        }
    }
}

@Composable
fun SettingsAndPrivacyScreen(
    profile: PetProfile,
    userEmail: String?,
    darkModeEnabled: Boolean,
    wellnessHydrationEnabled: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onToggleWellnessHydration: (Boolean) -> Unit,
    onSwitchOrAdoptNewPet: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteCloudAccountData: () -> Unit,
    onClose: () -> Unit
) {
    var confirmDeleteDialog by remember { mutableStateOf(false) }

    if (confirmDeleteDialog) {
        AlertDialog(
            onDismissRequest = { confirmDeleteDialog = false },
            title = { Text("Delete All Cloud Pet & Memory Data?") },
            text = {
                Text(
                    "This permanently deletes your PetProfile, Smart Memories, Chat History, Inventory, and Home Layouts from Cloud Firestore."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDeleteDialog = false
                        onDeleteCloudAccountData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Settings, Wellness & Privacy", style = MaterialTheme.typography.headlineSmall)
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Connected Google Account", style = MaterialTheme.typography.titleMedium)
                Text(userEmail ?: "Signed in with Google", style = MaterialTheme.typography.bodyMedium)
                Text("Active Pet: ${profile.petName} (${profile.animalType}) • Nickname: ${profile.userNickname}", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(onClick = onSwitchOrAdoptNewPet) {
                    Text("Customize / Adopt Another Animal")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Display, Accessibility & Self-Care", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cozy Night Dark Mode", fontWeight = FontWeight.SemiBold)
                        Text("High-contrast night palette for evening chats", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = darkModeEnabled,
                        onCheckedChange = onToggleDarkMode,
                        modifier = Modifier.testTag("dark_mode_switch")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Real-World Hydration & Break Check-Ins", fontWeight = FontWeight.SemiBold)
                        Text("${profile.petName} gently reminds you to drink water & rest", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = wellnessHydrationEnabled,
                        onCheckedChange = onToggleWellnessHydration
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Privacy & Data Controls", style = MaterialTheme.typography.titleMedium)
                Text(
                    "• Microphone is only activated when you explicitly tap the Mic button.\n" +
                        "• Sensitive memories always ask your approval before saving.\n" +
                        "• PetCoins are virtual gameplay tokens only—never simulated as real money.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sign Out")
                    }
                    OutlinedButton(
                        onClick = { confirmDeleteDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("delete_account_data_button")
                    ) {
                        Icon(
                            Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Data", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
