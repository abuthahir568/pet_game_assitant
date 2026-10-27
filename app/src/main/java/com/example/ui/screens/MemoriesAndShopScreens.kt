package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CoinTransaction
import com.example.data.model.CreatorDesign
import com.example.data.model.InventoryItem
import com.example.data.model.PetMemory
import com.example.data.model.PetProfile
import com.example.data.model.PetShopCatalog
import com.example.data.model.ShopCatalogItem
import com.example.data.model.parseHexColor
import com.example.ui.theme.AmberCoin
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartMemoriesScreen(
    petName: String,
    memories: List<PetMemory>,
    onAddMemory: (category: String, title: String, content: String, reminderMins: Int) -> Unit,
    onEditMemory: (memoryId: String, title: String, content: String, isCompleted: Boolean) -> Unit,
    onForgetMemory: (memoryId: String) -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingMemory by remember { mutableStateOf<PetMemory?>(null) }

    val categories = listOf("ALL", "PLAN", "BIRTHDAY", "FAVORITE", "GOAL", "NAME", "NOTE")

    val filteredMemories = remember(memories, searchQuery, selectedCategory) {
        memories.filter { mem ->
            val matchesCat = selectedCategory == "ALL" || mem.category == selectedCategory
            val matchesQuery = searchQuery.isBlank() ||
                mem.title.contains(searchQuery, ignoreCase = true) ||
                mem.content.contains(searchQuery, ignoreCase = true)
            matchesCat && matchesQuery
        }
    }

    if (showCreateDialog) {
        var cat by remember { mutableStateOf("PLAN") }
        var title by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        var reminderMinsText by remember { mutableStateOf("0") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Teach $petName a Smart Memory") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("PLAN", "BIRTHDAY", "FAVORITE", "GOAL", "NOTE").forEach { c ->
                            FilterChip(
                                selected = cat == c,
                                onClick = { cat = c },
                                label = { Text(c) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it.take(100) },
                        label = { Text("Title (e.g. Mom's Birthday, Standup Meeting)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it.take(500) },
                        label = { Text("Details to remember") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = reminderMinsText,
                        onValueChange = { reminderMinsText = it.filter { ch -> ch.isDigit() }.take(5) },
                        label = { Text("Remind me in minutes (0 = no alarm)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mins = reminderMinsText.toIntOrNull() ?: 0
                        if (mins > 0) onRequestNotificationPermission()
                        onAddMemory(cat, title.ifBlank { "Memory" }, content.ifBlank { title }, mins)
                        showCreateDialog = false
                    },
                    modifier = Modifier.testTag("save_new_memory_confirm_btn")
                ) {
                    Text("Save Memory")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (editingMemory != null) {
        val current = editingMemory!!
        var editTitle by remember(current) { mutableStateOf(current.title) }
        var editContent by remember(current) { mutableStateOf(current.content) }
        var editDone by remember(current) { mutableStateOf(current.isCompleted) }

        AlertDialog(
            onDismissRequest = { editingMemory = null },
            title = { Text("Edit Memory") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it.take(100) },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editContent,
                        onValueChange = { editContent = it.take(500) },
                        label = { Text("Content") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    FilterChip(
                        selected = editDone,
                        onClick = { editDone = !editDone },
                        label = { Text(if (editDone) "Completed ✓" else "Mark as Completed") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onEditMemory(current.id, editTitle, editContent, editDone)
                    editingMemory = null
                }) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingMemory = null }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("$petName's Smart Memory", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Persistent plans, birthdays, favorites & scheduled reminders",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = { showCreateDialog = true },
                modifier = Modifier.testTag("add_memory_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add")
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Search saved memories...") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_memories_input")
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            categories.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        if (filteredMemories.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🧠", style = MaterialTheme.typography.displayMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No memories match yet!", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Tell $petName in Voice Chat \"Remember I have a meeting tomorrow at 10 AM\" or tap + Add above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredMemories, key = { it.id }) { mem ->
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = mem.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Row {
                                    IconButton(
                                        onClick = { editingMemory = mem },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Memory", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { onForgetMemory(mem.id) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("forget_memory_${mem.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Forget Memory",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (mem.isCompleted) "✓ ${mem.title}" else mem.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = mem.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (mem.scheduledEpochMillis > 0L) {
                                val fmt = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Scheduled: ${fmt.format(Date(mem.scheduledEpochMillis))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VirtualShopAndWalletScreen(
    profile: PetProfile,
    inventory: List<InventoryItem>,
    transactions: List<CoinTransaction>,
    creatorDesigns: List<CreatorDesign>,
    onBuyCatalogItem: (ShopCatalogItem) -> Unit,
    onToggleEquip: (InventoryItem, Boolean) -> Unit,
    onPublishCreatorDesign: (creatorName: String, title: String, category: String, primaryHex: String, accentHex: String, priceCoins: Long) -> Unit,
    onAdoptCreatorDesign: (CreatorDesign) -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) }
    var selectedShopCategory by remember { mutableStateOf("ALL") }
    var confirmPurchaseItem by remember { mutableStateOf<ShopCatalogItem?>(null) }
    var showUploadDesignDialog by remember { mutableStateOf(false) }

    if (confirmPurchaseItem != null) {
        val item = confirmPurchaseItem!!
        AlertDialog(
            onDismissRequest = { confirmPurchaseItem = null },
            title = { Text("Confirm PetShop Purchase") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${item.emoji} ${item.name}", style = MaterialTheme.typography.titleLarge)
                    Text(item.description)
                    Text("Price: 🪙 ${item.priceCoins} PetCoins (Your Balance: 🪙 ${profile.coins})")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onBuyCatalogItem(item)
                        confirmPurchaseItem = null
                    },
                    enabled = profile.coins >= item.priceCoins,
                    modifier = Modifier.testTag("confirm_buy_item_button")
                ) {
                    Text("Buy with PetCoins")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmPurchaseItem = null }) { Text("Cancel") }
            }
        )
    }

    if (showUploadDesignDialog) {
        var title by remember { mutableStateOf("") }
        var cat by remember { mutableStateOf("OUTFIT") }
        var priceText by remember { mutableStateOf("80") }

        AlertDialog(
            onDismissRequest = { showUploadDesignDialog = false },
            title = { Text("Creator Studio: Publish Item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Design original virtual pet items for the community! Items use virtual PetCoins only (not withdrawable real money).",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it.take(60) },
                        label = { Text("Original Item Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("OUTFIT", "ACCESSORY", "TOY", "FURNITURE").forEach { c ->
                            FilterChip(selected = cat == c, onClick = { cat = c }, label = { Text(c) })
                        }
                    }
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = { Text("Virtual Coin Price (10 - 5000)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val price = (priceText.toLongOrNull() ?: 80L).coerceIn(10L, 5000L)
                    onPublishCreatorDesign(
                        profile.userNickname.ifBlank { "Creator" },
                        title.ifBlank { "Starlight Custom Gear" },
                        cat,
                        "#6366F1",
                        "#F59E0B",
                        price
                    )
                    showUploadDesignDialog = false
                }) {
                    Text("Publish Design")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUploadDesignDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = activeTab) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("Shop") })
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("Inventory (${inventory.size})") })
            Tab(selected = activeTab == 2, onClick = { activeTab = 2 }, text = { Text("Creators") })
            Tab(selected = activeTab == 3, onClick = { activeTab = 3 }, text = { Text("Wallet") })
        }

        when (activeTab) {
            0 -> {
                val filteredCatalog = remember(selectedShopCategory) {
                    if (selectedShopCategory == "ALL") PetShopCatalog.items
                    else PetShopCatalog.items.filter { it.category == selectedShopCategory }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(135.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_shop_banner_1793118545046),
                                    contentDescription = "Boutique Pet Shop",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "Balance: 🪙 ${profile.coins} PetCoins",
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("ALL", "FOOD", "OUTFIT", "ACCESSORY", "TOY", "FURNITURE", "DECOR").forEach { cat ->
                                FilterChip(
                                    selected = selectedShopCategory == cat,
                                    onClick = { selectedShopCategory = cat },
                                    label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }

                    items(filteredCatalog, key = { it.id }) { item ->
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(parseHexColor(item.colorHex).copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(item.emoji, style = MaterialTheme.typography.headlineMedium)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        item.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        item.statBoostLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                FilledTonalButton(
                                    onClick = { confirmPurchaseItem = item },
                                    modifier = Modifier.testTag("shop_buy_${item.id}")
                                ) {
                                    Text("🪙 ${item.priceCoins}")
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (inventory.isEmpty()) {
                        item {
                            Text(
                                "Your inventory is empty! Visit the Shop tab to pick up treats, crowns, capes, and furniture.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    items(inventory, key = { it.id }) { inv ->
                        val equippable = inv.category in listOf("OUTFIT", "ACCESSORY", "TOY")
                        Card(
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${inv.name} (x${inv.quantity})", style = MaterialTheme.typography.titleMedium)
                                    Text(inv.category, style = MaterialTheme.typography.labelSmall)
                                }
                                if (equippable) {
                                    Button(
                                        onClick = { onToggleEquip(inv, !inv.isEquipped) },
                                        modifier = Modifier.testTag("equip_btn_${inv.id}")
                                    ) {
                                        if (inv.isEquipped) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Unequip")
                                        } else {
                                            Text("Equip on 3D Pet")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Creator Economy Studio", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "Design and share custom pet outfits, toys, and decor with the community using virtual PetCoins!",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { showUploadDesignDialog = true },
                                    modifier = Modifier.testTag("open_creator_upload_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Create & Publish Pet Item")
                                }
                            }
                        }
                    }

                    items(creatorDesigns, key = { it.id }) { design ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(design.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "By ${design.creatorName} • ${design.category} • ${design.downloadsCount} adoptions",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedButton(onClick = { onAdoptCreatorDesign(design) }) {
                                    Text("Adopt 🪙 ${design.priceCoins}")
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AmberCoin.copy(alpha = 0.16f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Virtual PetCoin Wallet", style = MaterialTheme.typography.titleMedium)
                                Text("🪙 ${profile.coins}", style = MaterialTheme.typography.displayMedium)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.virtual_coins_disclaimer),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item {
                        Text("Verified Cloud Transaction Ledger", style = MaterialTheme.typography.titleMedium)
                    }

                    items(transactions, key = { it.id }) { tx ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tx.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(tx.type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = if (tx.amount >= 0) "+${tx.amount} 🪙" else "${tx.amount} 🪙",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (tx.amount >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
