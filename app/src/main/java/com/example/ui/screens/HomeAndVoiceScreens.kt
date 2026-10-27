package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ChatMessageRecord
import com.example.data.model.HomeLayout
import com.example.data.model.PetActionState
import com.example.data.model.PetPersonality
import com.example.data.model.PetProfile
import com.example.data.model.PetSpecies
import com.example.data.model.SupportedPetLanguage
import com.example.data.model.parseHexColor
import com.example.ui.components.Animated3DPetStage
import com.example.ui.theme.AmberCoin
import com.example.ui.theme.EmeraldStat
import com.example.ui.theme.SkyWater
import com.example.ui.theme.VioletSleep
import com.example.viewmodel.AiChatState
import com.example.viewmodel.PendingSensitiveMemory

@Composable
fun MainPetSanctuaryScreen(
    profile: PetProfile,
    homeLayout: HomeLayout?,
    actionState: PetActionState,
    bubbleText: String,
    mouthOpenAmount: Float,
    isListening: Boolean,
    onPetTapped: () -> Unit,
    onCareAction: (PetActionState) -> Unit,
    onQuickVoiceMicClick: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenHomeBuilder: () -> Unit,
    onClaimDailyReward: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val species = PetSpecies.fromId(profile.animalType)
    val personality = PetPersonality.fromId(profile.personality)
    val wallColor = parseHexColor(homeLayout?.wallColorHex ?: "#E0E7FF", MaterialTheme.colorScheme.primaryContainer)
    val floorColor = parseHexColor(homeLayout?.floorColorHex ?: "#FDE68A", MaterialTheme.colorScheme.secondaryContainer)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Header: Pet Identity, Friendship Level, Coin Wallet, Settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${species.emoji} ${profile.petName}",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "Lv.${profile.friendshipLevel} Bestie",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    text = "${personality.badge} ${personality.displayName} • Mood: ${computeMoodLabel(profile)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = AmberCoin.copy(alpha = 0.18f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .clickable { onOpenShop() }
                        .testTag("wallet_coin_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🪙", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${profile.coins}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("open_settings_button")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings & Privacy")
                }
            }
        }

        // Speech Bubble from Pet
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = bubbleText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(14.dp)
            )
        }

        // 3D Pet Sanctuary Room Stage (reflecting saved HomeLayout furniture & colors!)
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to wallColor.copy(alpha = 0.65f),
                            0.68f to wallColor.copy(alpha = 0.35f),
                            0.69f to floorColor.copy(alpha = 0.55f),
                            1.0f to floorColor.copy(alpha = 0.85f)
                        )
                    )
            ) {
                // Placed furniture preview icons in background
                val placed = homeLayout?.placedItems.orEmpty().take(5)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (placed.isEmpty()) {
                        Text("🪴", style = MaterialTheme.typography.headlineMedium)
                        Text("🛋️", style = MaterialTheme.typography.headlineMedium)
                        Text("🏮", style = MaterialTheme.typography.headlineMedium)
                    } else {
                        placed.forEach { itemMap ->
                            val emoji = itemMap["emoji"] as? String ?: "🛋️"
                            Text(emoji, style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }

                // Animated 3D Pet Rig
                Animated3DPetStage(
                    species = species,
                    furColorHex = profile.furColorHex,
                    eyeColorHex = profile.eyeColorHex,
                    collarColorHex = profile.collarColorHex,
                    equippedHatId = profile.equippedHatId,
                    equippedOutfitId = profile.equippedOutfitId,
                    actionState = actionState,
                    mouthOpenAmount = mouthOpenAmount,
                    isListening = isListening,
                    onPetTapped = onPetTapped,
                    modifier = Modifier.fillMaxSize()
                )

                // Floating Quick Microphone Button on Stage
                FloatingActionButton(
                    onClick = onQuickVoiceMicClick,
                    containerColor = if (isListening) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .testTag("home_quick_mic_fab")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Talk with ${profile.petName}"
                    )
                }

                // Quick Daily Gift Chip on Stage
                AssistChip(
                    onClick = onClaimDailyReward,
                    label = { Text("Daily Gift") },
                    leadingIcon = { Icon(Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                        .testTag("claim_daily_reward_chip")
                )
            }
        }

        // Pet Care Needs Meters (Hunger, Thirst, Happiness, Energy, Cleanliness, Friendship)
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Care & Friendship Status", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PetNeedBar("Hunger", profile.hunger, Color(0xFFF97316), Modifier.weight(1f))
                    PetNeedBar("Thirst", profile.thirst, SkyWater, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PetNeedBar("Happy", profile.happiness, Color(0xFFEC4899), Modifier.weight(1f))
                    PetNeedBar("Energy", profile.energy, VioletSleep, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PetNeedBar("Clean", profile.cleanliness, EmeraldStat, Modifier.weight(1f))
                    PetNeedBar("Bond XP", (profile.friendshipXp % 100L), AmberCoin, Modifier.weight(1f))
                }
            }
        }

        // Interactive Care Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CareQuickActionButton("Feed", Icons.Default.Restaurant, Modifier.weight(1f), "care_feed_btn") {
                onCareAction(PetActionState.EATING)
            }
            CareQuickActionButton("Water", Icons.Default.WaterDrop, Modifier.weight(1f), "care_water_btn") {
                onCareAction(PetActionState.DRINKING)
            }
            CareQuickActionButton("Bath", Icons.Default.CleaningServices, Modifier.weight(1f), "care_bath_btn") {
                onCareAction(PetActionState.BATHING)
            }
            CareQuickActionButton("Play", Icons.Default.SportsEsports, Modifier.weight(1f), "care_play_btn") {
                onCareAction(PetActionState.PLAYING)
            }
            CareQuickActionButton("Sleep", Icons.Default.Bedtime, Modifier.weight(1f), "care_sleep_btn") {
                onCareAction(PetActionState.SLEEPING)
            }
        }

        // Quick Shortcuts to Shop & 3D Home Builder
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                onClick = onOpenShop,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("shortcut_pet_shop_button"),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pet Shop")
            }
            FilledTonalButton(
                onClick = onOpenHomeBuilder,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("shortcut_home_builder_button"),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.HomeWork, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("3D Home Builder")
            }
        }
    }
}

@Composable
private fun PetNeedBar(label: String, value: Long, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text("${value.coerceIn(0L, 100L)}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { (value.coerceIn(0L, 100L) / 100f) },
            color = color,
            trackColor = color.copy(alpha = 0.18f),
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(CircleShape)
        )
    }
}

@Composable
private fun CareQuickActionButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .height(64.dp)
            .testTag(tag)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

private fun computeMoodLabel(profile: PetProfile): String = when {
    profile.hunger < 40L -> "Hungry 🍰"
    profile.thirst < 40L -> "Thirsty 💧"
    profile.energy < 35L -> "Sleepy 😴"
    profile.happiness >= 85L -> "Overjoyed ✨"
    else -> "Playful 😊"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceConversationScreen(
    profile: PetProfile,
    chatHistory: List<ChatMessageRecord>,
    aiChatState: AiChatState,
    actionState: PetActionState,
    mouthOpenAmount: Float,
    isListening: Boolean,
    isMuted: Boolean,
    partialTranscript: String,
    voiceError: String?,
    pendingSensitiveMemory: PendingSensitiveMemory?,
    onToggleMute: () -> Unit,
    onMicToggle: () -> Unit,
    onSendText: (String) -> Unit,
    onChangeLanguageAndVoice: (String, Double, Double) -> Unit,
    onDecisionSensitiveMemory: (Boolean) -> Unit
) {
    val species = PetSpecies.fromId(profile.animalType)
    var inputMessage by remember { mutableStateOf("") }
    var showLanguageMenu by remember { mutableStateOf(false) }
    var showVoiceTuningDialog by remember { mutableStateOf(false) }
    val activeLanguage = SupportedPetLanguage.fromCode(profile.preferredLanguage)

    val quickPrompts = listOf(
        "Tell me a funny joke!",
        "Play a quick riddle game with me!",
        "Remember I have a meeting tomorrow at 10 AM",
        "Tell me a 30-second adventure story"
    )

    // Sensitive Memory Permission Dialog
    if (pendingSensitiveMemory != null) {
        AlertDialog(
            onDismissRequest = { onDecisionSensitiveMemory(false) },
            icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
            title = { Text("Save Personal Memory?") },
            text = {
                Text(
                    "${profile.petName} asks permission before saving personal details:\n\n" +
                        "Title: ${pendingSensitiveMemory.title}\n" +
                        "Details: ${pendingSensitiveMemory.content}"
                )
            },
            confirmButton = {
                Button(onClick = { onDecisionSensitiveMemory(true) }) {
                    Text("Approve & Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { onDecisionSensitiveMemory(false) }) {
                    Text("Don't Save")
                }
            }
        )
    }

    if (showVoiceTuningDialog) {
        var tempPitch by remember { mutableFloatStateOf(profile.voicePitch.toFloat()) }
        var tempSpeed by remember { mutableFloatStateOf(profile.voiceSpeed.toFloat()) }
        AlertDialog(
            onDismissRequest = { showVoiceTuningDialog = false },
            title = { Text("Voice Speed & Pitch") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Voice Pitch: ${"%.2f".format(tempPitch)}x")
                    Slider(value = tempPitch, onValueChange = { tempPitch = it }, valueRange = 0.6f..1.8f)
                    Text("Speech Speed: ${"%.2f".format(tempSpeed)}x")
                    Slider(value = tempSpeed, onValueChange = { tempSpeed = it }, valueRange = 0.6f..1.6f)
                }
            },
            confirmButton = {
                Button(onClick = {
                    onChangeLanguageAndVoice(activeLanguage.code, tempPitch.toDouble(), tempSpeed.toDouble())
                    showVoiceTuningDialog = false
                }) {
                    Text("Apply Voice")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVoiceTuningDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Header with Language Switcher, Voice Controls, and Mute
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                OutlinedButton(
                    onClick = { showLanguageMenu = true },
                    modifier = Modifier.testTag("language_selector_button")
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${activeLanguage.displayName} (${activeLanguage.nativeLabel})")
                }
                DropdownMenu(
                    expanded = showLanguageMenu,
                    onDismissRequest = { showLanguageMenu = false }
                ) {
                    SupportedPetLanguage.entries.forEach { lang ->
                        DropdownMenuItem(
                            text = { Text("${lang.displayName} — ${lang.nativeLabel}") },
                            onClick = {
                                showLanguageMenu = false
                                onChangeLanguageAndVoice(lang.code, profile.voicePitch, profile.voiceSpeed)
                            }
                        )
                    }
                }
            }

            Row {
                IconButton(onClick = { showVoiceTuningDialog = true }) {
                    Icon(Icons.Default.MusicNote, contentDescription = "Voice Speed & Pitch")
                }
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier.testTag("toggle_mute_button")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isMuted) "Unmute Pet Voice" else "Mute Pet Voice"
                    )
                }
            }
        }

        // Live Lip-Synced 3D Pet Avatar Stage
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(185.dp),
            contentAlignment = Alignment.Center
        ) {
            Animated3DPetStage(
                species = species,
                furColorHex = profile.furColorHex,
                eyeColorHex = profile.eyeColorHex,
                collarColorHex = profile.collarColorHex,
                equippedHatId = profile.equippedHatId,
                equippedOutfitId = profile.equippedOutfitId,
                actionState = if (mouthOpenAmount > 0.08f) PetActionState.SPEAKING else actionState,
                mouthOpenAmount = mouthOpenAmount,
                isListening = isListening,
                onPetTapped = {},
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isListening || partialTranscript.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Text(
                    text = if (isListening) "🎙️ Listening: ${partialTranscript.ifBlank { "Speak now..." }}"
                    else "Heard: \"$partialTranscript\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        if (voiceError != null) {
            Text(
                text = voiceError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        if (aiChatState is AiChatState.Error) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Text(
                    text = aiChatState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // Quick Conversation Starters
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            quickPrompts.forEach { prompt ->
                AssistChip(
                    onClick = { onSendText(prompt) },
                    label = { Text(prompt, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        // Conversation Transcript List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (chatHistory.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${species.emoji} ${profile.petName}: Hey ${profile.userNickname}! Tap the microphone or type below to tell me about your day, ask for a joke, or tell me something to remember!",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
            items(chatHistory.takeLast(30), key = { it.id.ifBlank { it.text.hashCode().toString() } }) { msg ->
                val isUser = msg.sender == "USER"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth(0.84f)
                    ) {
                        Text(
                            text = if (isUser) msg.text else "${species.emoji} ${msg.text}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isUser) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
            if (aiChatState is AiChatState.Thinking) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${profile.petName} is thinking...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom Voice Mic + Text Chat Input Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = onMicToggle,
                containerColor = if (isListening) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primaryContainer,
                contentColor = if (isListening) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .size(52.dp)
                    .testTag("voice_chat_mic_button")
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = if (isListening) "Stop Listening" else "Speak with Voice"
                )
            }

            OutlinedTextField(
                value = inputMessage,
                onValueChange = { inputMessage = it },
                placeholder = { Text("Chat with ${profile.petName}...") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("voice_chat_text_input")
            )

            IconButton(
                onClick = {
                    if (inputMessage.isNotBlank()) {
                        onSendText(inputMessage)
                        inputMessage = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("voice_chat_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Message",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
