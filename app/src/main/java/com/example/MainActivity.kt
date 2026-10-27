package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.UserPreferencesRepository
import com.example.data.model.PetActionState
import com.example.data.repository.PetPalRepository
import com.example.notification.PetReminderScheduler
import com.example.security.configureAppCheck
import com.example.ui.screens.ArcadeAndHomeBuilderScreen
import com.example.ui.screens.MainPetSanctuaryScreen
import com.example.ui.screens.PetAdoptionCreatorScreen
import com.example.ui.screens.SettingsAndPrivacyScreen
import com.example.ui.screens.SmartMemoriesScreen
import com.example.ui.screens.SplashAndOnboardingScreen
import com.example.ui.screens.VirtualShopAndWalletScreen
import com.example.ui.screens.VoiceConversationScreen
import com.example.ui.theme.PetPalTheme
import com.example.viewmodel.DataUiState
import com.example.viewmodel.PetPalViewModel
import com.example.voice.PetVoiceEngine
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check configuration BEFORE UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            val appContext = applicationContext
            val prefsRepo = remember { UserPreferencesRepository(appContext) }
            val darkMode by prefsRepo.darkModeFlow.collectAsStateWithLifecycle(initialValue = false)

            PetPalTheme(darkTheme = darkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PetPalRootNavigation(prefsRepo = prefsRepo, darkMode = darkMode)
                }
            }
        }
    }
}

private enum class TopDestination(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    HOME(R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home, "nav_tab_home"),
    VOICE(R.string.nav_talk, Icons.Filled.Mic, Icons.Outlined.MicNone, "nav_tab_voice"),
    MEMORIES(R.string.nav_memory, Icons.Filled.Memory, Icons.Outlined.Memory, "nav_tab_memories"),
    SHOP(R.string.nav_shop, Icons.Filled.ShoppingBag, Icons.Outlined.ShoppingBag, "nav_tab_shop"),
    ARCADE(R.string.nav_arcade, Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports, "nav_tab_arcade")
}

@Composable
private fun PetPalRootNavigation(
    prefsRepo: UserPreferencesRepository,
    darkMode: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    // Attempt low-friction silent auto sign-in once on launch
    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            val cm = CredentialManager.create(context)
            attemptAutoSignIn(
                context = context,
                credentialManager = cm,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        SplashAndOnboardingScreen(
            showSplashInitially = true,
            onAuthSuccess = {}
        )
    } else {
        AuthenticatedPetPalApp(
            currentUserId = user.uid,
            userEmail = user.email,
            userDisplayName = user.displayName ?: "Bestie",
            prefsRepo = prefsRepo,
            darkMode = darkMode
        )
    }
}

@Composable
private fun AuthenticatedPetPalApp(
    currentUserId: String,
    userEmail: String?,
    userDisplayName: String,
    prefsRepo: UserPreferencesRepository,
    darkMode: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val viewModel: PetPalViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                PetPalViewModel(PetPalRepository(db), currentUserId)
            }
        }
    )

    val voiceEngine = remember(currentUserId) { PetVoiceEngine(context, scope) }
    DisposableEffect(voiceEngine) {
        onDispose { voiceEngine.shutdown() }
    }

    val isMuted by prefsRepo.muteVoiceFlow.collectAsStateWithLifecycle(initialValue = false)
    val wellnessHydration by prefsRepo.wellnessHydrationFlow.collectAsStateWithLifecycle(initialValue = false)
    LaunchedEffect(isMuted) {
        voiceEngine.isMuted = isMuted
    }

    val profileState by viewModel.petProfileState.collectAsStateWithLifecycle()
    val memoriesState by viewModel.memoriesState.collectAsStateWithLifecycle()
    val inventoryState by viewModel.inventoryState.collectAsStateWithLifecycle()
    val transactionsState by viewModel.transactionsState.collectAsStateWithLifecycle()
    val homeLayoutState by viewModel.homeLayoutState.collectAsStateWithLifecycle()
    val chatHistoryState by viewModel.chatHistoryState.collectAsStateWithLifecycle()
    val creatorDesignsState by viewModel.creatorDesignsState.collectAsStateWithLifecycle()

    val petAction by viewModel.petAction.collectAsStateWithLifecycle()
    val bubbleText by viewModel.petBubbleText.collectAsStateWithLifecycle()
    val aiChatState by viewModel.aiChatState.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val pendingSensitiveMemory by viewModel.pendingSensitiveMemory.collectAsStateWithLifecycle()

    val isListening by voiceEngine.isListening.collectAsStateWithLifecycle()
    val mouthOpenAmount by voiceEngine.mouthOpenAmount.collectAsStateWithLifecycle()
    val partialTranscript by voiceEngine.partialTranscript.collectAsStateWithLifecycle()
    val voiceError by voiceEngine.voiceError.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(TopDestination.HOME) }
    var arcadeInitialSubTab by remember { mutableIntStateOf(0) }
    var showSettingsOverlay by remember { mutableStateOf(false) }
    var showAdoptionCreatorOverlay by remember { mutableStateOf(false) }

    // Runtime permission launchers (requested only when needed)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val requestNotificationsIfNeeded: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val scheduleMemoryAlarm: (String, String, String, Long) -> Boolean = { id, title, body, triggerMs ->
        requestNotificationsIfNeeded()
        PetReminderScheduler.scheduleReminder(context, id, title, body, triggerMs)
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val lang = (profileState as? DataUiState.Success)?.data?.preferredLanguage ?: "en-US"
            voiceEngine.startListening(lang) { recognizedText ->
                viewModel.sendMessageToAiBestFriend(
                    userMessage = recognizedText,
                    onSpeakReply = { voiceEngine.speak(it) },
                    onScheduleNotification = scheduleMemoryAlarm
                )
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Microphone permission is required for live voice chat.")
            }
        }
    }

    val toggleVoiceListening: () -> Unit = {
        if (isListening) {
            voiceEngine.stopListening()
        } else {
            val hasMic = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (hasMic) {
                val lang = (profileState as? DataUiState.Success)?.data?.preferredLanguage ?: "en-US"
                voiceEngine.startListening(lang) { recognizedText ->
                    viewModel.sendMessageToAiBestFriend(
                        userMessage = recognizedText,
                        onSpeakReply = { voiceEngine.speak(it) },
                        onScheduleNotification = scheduleMemoryAlarm
                    )
                }
            } else {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.dismissBanner()
    }

    // Back navigation handler for sub-screens / non-home tabs
    BackHandler(enabled = showSettingsOverlay || showAdoptionCreatorOverlay || currentTab != TopDestination.HOME) {
        when {
            showSettingsOverlay -> showSettingsOverlay = false
            showAdoptionCreatorOverlay -> showAdoptionCreatorOverlay = false
            else -> currentTab = TopDestination.HOME
        }
    }

    when (val pState = profileState) {
        is DataUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is DataUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = pState.message)
            }
        }

        is DataUiState.Success -> {
            val profile = pState.data
            if (profile == null || showAdoptionCreatorOverlay) {
                PetAdoptionCreatorScreen(
                    defaultNickname = userDisplayName.substringBefore(" "),
                    onAdoptConfirmed = { petName, species, personality, furHex, eyeHex, collarHex, nick ->
                        viewModel.adoptNewPet(petName, species, personality, furHex, eyeHex, collarHex, nick)
                        showAdoptionCreatorOverlay = false
                        voiceEngine.playChimeEffect(true)
                    }
                )
            } else {
                // Sync TTS language & pitch with user's PetProfile
                LaunchedEffect(profile.preferredLanguage, profile.voicePitch, profile.voiceSpeed) {
                    voiceEngine.applyLanguageAndVoice(
                        profile.preferredLanguage,
                        profile.voicePitch.toFloat(),
                        profile.voiceSpeed.toFloat()
                    )
                }

                // Trigger warm pet welcome greeting once per session
                LaunchedEffect(profile.id) {
                    viewModel.triggerWelcomeGreetingIfNeeded(profile) { greeting ->
                        voiceEngine.speak(greeting)
                    }
                }

                if (showSettingsOverlay) {
                    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            SettingsAndPrivacyScreen(
                                profile = profile,
                                userEmail = userEmail,
                                darkModeEnabled = darkMode,
                                wellnessHydrationEnabled = wellnessHydration,
                                onToggleDarkMode = { enabled ->
                                    scope.launch { prefsRepo.setDarkMode(enabled) }
                                },
                                onToggleWellnessHydration = { enabled ->
                                    scope.launch {
                                        prefsRepo.setWellnessHydration(enabled)
                                        if (enabled) {
                                            requestNotificationsIfNeeded()
                                            PetReminderScheduler.scheduleReminder(
                                                context = context,
                                                reminderKey = "wellness_hydration",
                                                title = "${profile.petName}: Hydration Check! 💧",
                                                body = "Hey ${profile.userNickname}, let's both drink a glass of water right now!",
                                                triggerAtEpochMillis = System.currentTimeMillis() + 60 * 60_000L
                                            )
                                        } else {
                                            PetReminderScheduler.cancelReminder(context, "wellness_hydration")
                                        }
                                    }
                                },
                                onSwitchOrAdoptNewPet = {
                                    showSettingsOverlay = false
                                    showAdoptionCreatorOverlay = true
                                },
                                onSignOut = {
                                    showSettingsOverlay = false
                                    val cm = CredentialManager.create(context)
                                    signOutUser(cm, {}, scope)
                                },
                                onDeleteCloudAccountData = {
                                    viewModel.deleteAccountCloudData {
                                        showSettingsOverlay = false
                                        val cm = CredentialManager.create(context)
                                        signOutUser(cm, {}, scope)
                                    }
                                },
                                onClose = { showSettingsOverlay = false }
                            )
                        }
                    }
                } else {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val isWideScreen = maxWidth >= 640.dp

                        Scaffold(
                            contentWindowInsets = WindowInsets.safeDrawing,
                            snackbarHost = { SnackbarHost(snackbarHostState) },
                            bottomBar = {
                                if (!isWideScreen) {
                                    NavigationBar {
                                        TopDestination.entries.forEach { dest ->
                                            val selected = currentTab == dest
                                            NavigationBarItem(
                                                selected = selected,
                                                onClick = {
                                                    if (dest == TopDestination.ARCADE) arcadeInitialSubTab = 0
                                                    currentTab = dest
                                                },
                                                icon = {
                                                    Icon(
                                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                        contentDescription = stringResource(dest.labelRes)
                                                    )
                                                },
                                                label = { Text(stringResource(dest.labelRes)) },
                                                modifier = Modifier.testTag(dest.tag)
                                            )
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                if (isWideScreen) {
                                    NavigationRail {
                                        TopDestination.entries.forEach { dest ->
                                            val selected = currentTab == dest
                                            NavigationRailItem(
                                                selected = selected,
                                                onClick = {
                                                    if (dest == TopDestination.ARCADE) arcadeInitialSubTab = 0
                                                    currentTab = dest
                                                },
                                                icon = {
                                                    Icon(
                                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                        contentDescription = stringResource(dest.labelRes)
                                                    )
                                                },
                                                label = { Text(stringResource(dest.labelRes)) },
                                                modifier = Modifier.testTag(dest.tag)
                                            )
                                        }
                                    }
                                }

                                Box(modifier = Modifier.weight(1f)) {
                                    val memories = (memoriesState as? DataUiState.Success)?.data.orEmpty()
                                    val inventory = (inventoryState as? DataUiState.Success)?.data.orEmpty()
                                    val transactions = (transactionsState as? DataUiState.Success)?.data.orEmpty()
                                    val homeLayout = (homeLayoutState as? DataUiState.Success)?.data
                                    val chatHistory = (chatHistoryState as? DataUiState.Success)?.data.orEmpty()
                                    val creatorDesigns = (creatorDesignsState as? DataUiState.Success)?.data.orEmpty()

                                    when (currentTab) {
                                        TopDestination.HOME -> MainPetSanctuaryScreen(
                                            profile = profile,
                                            homeLayout = homeLayout,
                                            actionState = petAction,
                                            bubbleText = bubbleText,
                                            mouthOpenAmount = mouthOpenAmount,
                                            isListening = isListening,
                                            onPetTapped = {
                                                voiceEngine.playChimeEffect(true)
                                                viewModel.performPetCareAction(PetActionState.LAUGHING) {
                                                    voiceEngine.speak(it)
                                                }
                                            },
                                            onCareAction = { action ->
                                                voiceEngine.playChimeEffect(true)
                                                viewModel.performPetCareAction(action) {
                                                    voiceEngine.speak(it)
                                                }
                                            },
                                            onQuickVoiceMicClick = {
                                                currentTab = TopDestination.VOICE
                                                toggleVoiceListening()
                                            },
                                            onOpenShop = { currentTab = TopDestination.SHOP },
                                            onOpenHomeBuilder = {
                                                arcadeInitialSubTab = 1
                                                currentTab = TopDestination.ARCADE
                                            },
                                            onClaimDailyReward = {
                                                voiceEngine.playChimeEffect(true)
                                                viewModel.claimDailyReward()
                                            },
                                            onOpenSettings = { showSettingsOverlay = true }
                                        )

                                        TopDestination.VOICE -> VoiceConversationScreen(
                                            profile = profile,
                                            chatHistory = chatHistory,
                                            aiChatState = aiChatState,
                                            actionState = petAction,
                                            mouthOpenAmount = mouthOpenAmount,
                                            isListening = isListening,
                                            isMuted = isMuted,
                                            partialTranscript = partialTranscript,
                                            voiceError = voiceError,
                                            pendingSensitiveMemory = pendingSensitiveMemory,
                                            onToggleMute = {
                                                scope.launch {
                                                    val next = !isMuted
                                                    prefsRepo.setMuteVoice(next)
                                                    if (next) voiceEngine.stopSpeaking()
                                                }
                                            },
                                            onMicToggle = toggleVoiceListening,
                                            onSendText = { msg ->
                                                voiceEngine.clearVoiceError()
                                                viewModel.sendMessageToAiBestFriend(
                                                    userMessage = msg,
                                                    onSpeakReply = { voiceEngine.speak(it) },
                                                    onScheduleNotification = scheduleMemoryAlarm
                                                )
                                            },
                                            onChangeLanguageAndVoice = { langCode, pitch, speed ->
                                                viewModel.updateLanguageAndVoice(langCode, pitch, speed)
                                            },
                                            onDecisionSensitiveMemory = { approved ->
                                                viewModel.approveSensitiveMemory(approved, scheduleMemoryAlarm)
                                            }
                                        )

                                        TopDestination.MEMORIES -> SmartMemoriesScreen(
                                            petName = profile.petName,
                                            memories = memories,
                                            onAddMemory = { cat, title, content, mins ->
                                                viewModel.addManualMemory(cat, title, content, mins, scheduleMemoryAlarm)
                                            },
                                            onEditMemory = { id, title, content, done ->
                                                viewModel.editMemory(id, title, content, done)
                                            },
                                            onForgetMemory = { id ->
                                                viewModel.forgetMemory(id) {
                                                    PetReminderScheduler.cancelReminder(context, it)
                                                }
                                            },
                                            onRequestNotificationPermission = requestNotificationsIfNeeded
                                        )

                                        TopDestination.SHOP -> VirtualShopAndWalletScreen(
                                            profile = profile,
                                            inventory = inventory,
                                            transactions = transactions,
                                            creatorDesigns = creatorDesigns,
                                            onBuyCatalogItem = { item ->
                                                voiceEngine.playChimeEffect(true)
                                                viewModel.buyShopItem(item)
                                            },
                                            onToggleEquip = { inv, equip ->
                                                viewModel.toggleEquipInventoryItem(inv, equip)
                                            },
                                            onPublishCreatorDesign = { cName, title, cat, pHex, aHex, price ->
                                                viewModel.publishCreatorItem(cName, title, cat, pHex, aHex, price)
                                            },
                                            onAdoptCreatorDesign = { design ->
                                                viewModel.adoptCommunityCreatorDesign(design)
                                            }
                                        )

                                        TopDestination.ARCADE -> ArcadeAndHomeBuilderScreen(
                                            profile = profile,
                                            savedHomeLayout = homeLayout,
                                            initialTab = arcadeInitialSubTab,
                                            onWinMiniGame = { title, coins, xp ->
                                                voiceEngine.playChimeEffect(true)
                                                viewModel.awardMiniGameReward(title, coins, xp)
                                            },
                                            onSaveHomeLayout = { theme, wall, floor, light, items ->
                                                viewModel.saveHomeBuilderLayout(theme, wall, floor, light, items)
                                            }
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
}
