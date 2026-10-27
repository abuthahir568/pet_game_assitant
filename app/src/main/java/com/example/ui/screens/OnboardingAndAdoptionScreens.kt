package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.auth.GoogleSignInButton
import com.example.data.model.PetActionState
import com.example.data.model.PetPersonality
import com.example.data.model.PetSpecies
import com.example.data.model.parseHexColor
import com.example.ui.components.Animated3DPetStage
import kotlinx.coroutines.delay

@Composable
fun SplashAndOnboardingScreen(
    showSplashInitially: Boolean,
    onAuthSuccess: () -> Unit
) {
    var inSplash by remember { mutableStateOf(showSplashInitially) }
    var onboardingPage by remember { mutableIntStateOf(0) }
    var authError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(showSplashInitially) {
        if (showSplashInitially) {
            delay(1400L)
            inSplash = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        if (inSplash) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Box(modifier = Modifier.size(220.dp)) {
                    Animated3DPetStage(
                        species = PetSpecies.DOG,
                        furColorHex = "#F59E0B",
                        eyeColorHex = "#3B82F6",
                        collarColorHex = "#F43F5E",
                        equippedHatId = "hat_golden_crown",
                        equippedOutfitId = "",
                        actionState = PetActionState.LAUGHING,
                        mouthOpenAmount = 0.4f,
                        isListening = false,
                        onPetTapped = {}
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "PetPal AI",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "My Virtual 3D Best Friend",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_pet_sanctuary_1793118545024),
                        contentDescription = "PetPal AI 3D Pet Sanctuary",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                val titles = listOf(
                    stringResource(R.string.welcome_title),
                    "Real-Time Multilingual Voice & Lip-Sync",
                    "Smart Memory, 3D Home Builder & Mini-Games"
                )
                val descriptions = listOf(
                    "Choose from 20 expressive 3D pets—from Golden Puppies & Red Pandas to Unicorns & Dragons—each with unique personalities and moods.",
                    "Chat naturally in English, Tamil, Hindi, Telugu, Malayalam, Kannada, Arabic, Spanish, French, Japanese, or Chinese with live lip-sync!",
                    "Your bestie remembers your plans and birthdays, sends helpful reminders, and lives in a 3D home you decorate together."
                )

                Text(
                    text = titles[onboardingPage],
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = descriptions[onboardingPage],
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Page indicators
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (onboardingPage == index) 28.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (onboardingPage == index) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                )
                                .clickable { onboardingPage = index }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (onboardingPage < 2) {
                    Button(
                        onClick = { onboardingPage++ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("onboarding_next_button"),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text("Next Feature", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Sign in with Google via Jetpack Credential Manager
                GoogleSignInButton(
                    onAuthSuccess = onAuthSuccess,
                    onAuthError = { err -> authError = err }
                )

                AnimatedVisibility(visible = authError != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = authError.orEmpty(),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Protected by Google Sign-In & Cloud Firestore sync so your pet, memories, and home are safe across devices.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PetAdoptionCreatorScreen(
    defaultNickname: String,
    onAdoptConfirmed: (
        petName: String,
        species: PetSpecies,
        personality: PetPersonality,
        furHex: String,
        eyeHex: String,
        collarHex: String,
        userNickname: String
    ) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var selectedSpecies by remember { mutableStateOf(PetSpecies.DOG) }
    var selectedPersonality by remember { mutableStateOf(PetPersonality.FUNNY_NAUGHTY) }
    var petName by remember { mutableStateOf("Mochi") }
    var userNickname by remember { mutableStateOf(defaultNickname.ifBlank { "Bestie" }) }
    var furHex by remember { mutableStateOf(PetSpecies.DOG.defaultFurHex) }
    var eyeHex by remember { mutableStateOf(PetSpecies.DOG.defaultEyeHex) }
    var collarHex by remember { mutableStateOf("#F43F5E") }

    val furPalette = listOf("#F59E0B", "#EA580C", "#FCE7F3", "#F8FAFC", "#92400E", "#7C3AED", "#10B981", "#64748B", "#818CF8")
    val collarPalette = listOf("#F43F5E", "#6366F1", "#10B981", "#F59E0B", "#EC4899", "#0EA5E9")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (step == 1) "1. Choose Your 3D Animal" else "2. Customize & Adopt",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (step == 1) "20 animated companions with unique voices & traits"
                    else "Give ${selectedSpecies.displayName} a name, personality & look!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "Step $step/2",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Live 3D Interactive Pet Preview Card
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(160.dp)) {
                    Animated3DPetStage(
                        species = selectedSpecies,
                        furColorHex = furHex,
                        eyeColorHex = eyeHex,
                        collarColorHex = collarHex,
                        equippedHatId = "",
                        equippedOutfitId = "",
                        actionState = PetActionState.PLAYING,
                        mouthOpenAmount = 0.25f,
                        isListening = false,
                        onPetTapped = {}
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${selectedSpecies.emoji} ${selectedSpecies.displayName}",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = selectedSpecies.specialTrait,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "\"${selectedSpecies.voiceSoundLabel}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (step == 1) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 104.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(PetSpecies.entries) { species ->
                    val isSelected = species == selectedSpecies
                    Card(
                        onClick = {
                            selectedSpecies = species
                            furHex = species.defaultFurHex
                            eyeHex = species.defaultEyeHex
                        },
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                shape = MaterialTheme.shapes.medium
                            )
                            .testTag("species_card_${species.name.lowercase()}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = species.emoji, style = MaterialTheme.typography.headlineMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = species.displayName,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Button(
                onClick = { step = 2 },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("continue_customize_button"),
                shape = MaterialTheme.shapes.large
            ) {
                Text("Customize ${selectedSpecies.displayName}", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = petName,
                    onValueChange = { petName = it.take(30) },
                    label = { Text("Pet's Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pet_name_input")
                )

                OutlinedTextField(
                    value = userNickname,
                    onValueChange = { userNickname = it.take(30) },
                    label = { Text("What should your pet call you? (Nickname)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("user_nickname_input")
                )

                Text("Select Personality", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PetPersonality.entries.forEach { p ->
                        FilterChip(
                            selected = selectedPersonality == p,
                            onClick = { selectedPersonality = p },
                            label = { Text("${p.badge} ${p.displayName}") },
                            modifier = Modifier.testTag("personality_chip_${p.name.lowercase()}")
                        )
                    }
                }
                Text(
                    text = selectedPersonality.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text("Coat / Fur Shade", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    furPalette.forEach { hex ->
                        val c = parseHexColor(hex)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (furHex == hex) 3.dp else 1.dp,
                                    color = if (furHex == hex) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable { furHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (furHex == hex) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected fur color",
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Text("Collar Accent", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    collarPalette.forEach { hex ->
                        val c = parseHexColor(hex)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (collarHex == hex) 3.dp else 1.dp,
                                    color = if (collarHex == hex) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { collarHex = hex }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { step = 1 },
                    modifier = Modifier
                        .weight(0.4f)
                        .height(54.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("Back")
                }
                Button(
                    onClick = {
                        onAdoptConfirmed(
                            petName.ifBlank { "Mochi" },
                            selectedSpecies,
                            selectedPersonality,
                            furHex,
                            eyeHex,
                            collarHex,
                            userNickname.ifBlank { "Bestie" }
                        )
                    },
                    modifier = Modifier
                        .weight(0.6f)
                        .height(54.dp)
                        .testTag("adopt_pet_confirm_button"),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Default.Pets, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Adopt ${petName.ifBlank { "Mochi" }}!", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
