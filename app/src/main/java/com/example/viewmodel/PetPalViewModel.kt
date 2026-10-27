package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChatMessageRecord
import com.example.data.model.CoinTransaction
import com.example.data.model.CreatorDesign
import com.example.data.model.HomeLayout
import com.example.data.model.InventoryItem
import com.example.data.model.PetActionState
import com.example.data.model.PetMemory
import com.example.data.model.PetPersonality
import com.example.data.model.PetProfile
import com.example.data.model.PetSpecies
import com.example.data.model.PlacedFurniture
import com.example.data.model.ShopCatalogItem
import com.example.data.model.SupportedPetLanguage
import com.example.data.repository.PetPalRepository
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

private const val TAG = "PetPalVM"

sealed interface DataUiState<out T> {
    data object Loading : DataUiState<Nothing>
    data class Success<T>(val data: T) : DataUiState<T>
    data class Error(val message: String) : DataUiState<Nothing>
}

sealed interface AiChatState {
    data object Idle : AiChatState
    data object Thinking : AiChatState
    data class Success(val reply: String, val emotion: String) : AiChatState
    data class Error(val message: String) : AiChatState
}

data class PendingSensitiveMemory(
    val category: String,
    val title: String,
    val content: String,
    val scheduledOffsetMinutes: Int
)

class PetPalViewModel(
    private val repository: PetPalRepository,
    private val currentUserId: String
) : ViewModel() {

    // Structured JSON schema for AI Best Friend conversation + Smart Memory extraction
    private val petConversationSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "reply" to Schema.string(description = "Warm, funny, friendly conversational reply in the user's selected language"),
                "emotion" to Schema.enumeration(
                    values = listOf("HAPPY", "LAUGHING", "CURIOUS", "CARING", "PLAYFUL", "SLEEPY", "PROUD"),
                    description = "Pet facial expression and body reaction"
                ),
                "hasMemoryToSave" to Schema.boolean(description = "True ONLY when the user explicitly shared a name, birthday, favorite, plan, reminder, or goal to remember"),
                "isSensitiveMemory" to Schema.boolean(description = "True if the memory contains personal dates, health, or private plans requiring user permission before saving"),
                "memoryCategory" to Schema.enumeration(
                    values = listOf("NAME", "BIRTHDAY", "FAVORITE", "PLAN", "GOAL", "NOTE"),
                    description = "Category of extracted memory"
                ),
                "memoryTitle" to Schema.string(description = "Short memory title or empty string if none"),
                "memoryContent" to Schema.string(description = "Detailed memory content or empty string if none"),
                "reminderMinutesFromNow" to Schema.integer(description = "Minutes from now if user requested a timed reminder, otherwise 0")
            )
        )
    }

    private val aiModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = petConversationSchema
            }
        )
    }

    val petProfileState: StateFlow<DataUiState<PetProfile?>> = repository.observePetProfile(currentUserId)
        .map<PetProfile?, DataUiState<PetProfile?>> { DataUiState.Success(it) }
        .catch { err ->
            Log.e(TAG, "Failed observing PetProfile", err)
            emit(DataUiState.Error(err.localizedMessage ?: "Failed to load pet profile"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    val memoriesState: StateFlow<DataUiState<List<PetMemory>>> = repository.observeMemories(currentUserId)
        .map<List<PetMemory>, DataUiState<List<PetMemory>>> { DataUiState.Success(it) }
        .catch { err ->
            Log.e(TAG, "Failed observing memories", err)
            emit(DataUiState.Error(err.localizedMessage ?: "Failed to load memories"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    val inventoryState: StateFlow<DataUiState<List<InventoryItem>>> = repository.observeInventory(currentUserId)
        .map<List<InventoryItem>, DataUiState<List<InventoryItem>>> { DataUiState.Success(it) }
        .catch { err ->
            Log.e(TAG, "Failed observing inventory", err)
            emit(DataUiState.Error(err.localizedMessage ?: "Failed to load inventory"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    val transactionsState: StateFlow<DataUiState<List<CoinTransaction>>> = repository.observeTransactions(currentUserId)
        .map<List<CoinTransaction>, DataUiState<List<CoinTransaction>>> { DataUiState.Success(it) }
        .catch { err ->
            Log.e(TAG, "Failed observing transactions", err)
            emit(DataUiState.Error(err.localizedMessage ?: "Failed to load wallet history"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    val homeLayoutState: StateFlow<DataUiState<HomeLayout?>> = repository.observeHomeLayout(currentUserId)
        .map<HomeLayout?, DataUiState<HomeLayout?>> { DataUiState.Success(it) }
        .catch { err ->
            Log.e(TAG, "Failed observing home layout", err)
            emit(DataUiState.Error(err.localizedMessage ?: "Failed to load home layout"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    val chatHistoryState: StateFlow<DataUiState<List<ChatMessageRecord>>> = repository.observeChatHistory(currentUserId)
        .map<List<ChatMessageRecord>, DataUiState<List<ChatMessageRecord>>> { DataUiState.Success(it) }
        .catch { err ->
            Log.e(TAG, "Failed observing chat history", err)
            emit(DataUiState.Error(err.localizedMessage ?: "Failed to load conversation history"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    val creatorDesignsState: StateFlow<DataUiState<List<CreatorDesign>>> = repository.observeApprovedCreatorDesigns()
        .map<List<CreatorDesign>, DataUiState<List<CreatorDesign>>> { DataUiState.Success(it) }
        .catch { err ->
            Log.e(TAG, "Failed observing creator designs", err)
            emit(DataUiState.Error(err.localizedMessage ?: "Failed to load creator community"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    private val _petAction = MutableStateFlow(PetActionState.IDLE)
    val petAction: StateFlow<PetActionState> = _petAction.asStateFlow()

    private val _petBubbleText = MutableStateFlow("Tap me to tickle, feed me treats, or tap the mic so we can chat, bestie!")
    val petBubbleText: StateFlow<String> = _petBubbleText.asStateFlow()

    private val _aiChatState = MutableStateFlow<AiChatState>(AiChatState.Idle)
    val aiChatState: StateFlow<AiChatState> = _aiChatState.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _pendingSensitiveMemory = MutableStateFlow<PendingSensitiveMemory?>(null)
    val pendingSensitiveMemory: StateFlow<PendingSensitiveMemory?> = _pendingSensitiveMemory.asStateFlow()

    private var hasGreetedSession = false

    init {
        startFairNeedProgressionLoop()
    }

    private fun startFairNeedProgressionLoop() {
        viewModelScope.launch {
            while (true) {
                delay(90_000L) // Fair, gentle progression every 90s while active; never harsh
                val profile = (petProfileState.value as? DataUiState.Success)?.data ?: continue
                val nextHunger = (profile.hunger - 2L).coerceAtLeast(25L)
                val nextThirst = (profile.thirst - 2L).coerceAtLeast(25L)
                val nextEnergy = (profile.energy - 1L).coerceAtLeast(25L)
                try {
                    repository.updatePetProfileFields(
                        mapOf(
                            "hunger" to nextHunger,
                            "thirst" to nextThirst,
                            "energy" to nextEnergy
                        )
                    )
                    when {
                        nextHunger < 45L -> _petBubbleText.value = "I'm getting hungry! Let's visit my food shop or grab a snack!"
                        nextThirst < 45L -> _petBubbleText.value = "Can you give me some cool water, bestie?"
                        nextEnergy < 40L -> _petBubbleText.value = "Yawn~ I'm sleepy. Can we take a quick nap?"
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    fun triggerWelcomeGreetingIfNeeded(profile: PetProfile, onSpeak: (String) -> Unit) {
        if (hasGreetedSession) return
        hasGreetedSession = true
        val species = PetSpecies.fromId(profile.animalType)
        val greeting = when {
            profile.hunger < 45L -> "${species.voiceSoundLabel} Hey ${profile.userNickname}! My tummy is rumbling—can we grab a treat?"
            profile.thirst < 45L -> "${species.voiceSoundLabel} Welcome back, ${profile.userNickname}! Can you pour me some fresh water?"
            else -> "${species.voiceSoundLabel} Yay, ${profile.userNickname} is here! Ready for jokes, games, or a cozy chat?"
        }
        _petBubbleText.value = greeting
        onSpeak(greeting)
    }

    fun dismissBanner() {
        _statusBannerMessage.value = null
    }

    fun adoptNewPet(
        petName: String,
        species: PetSpecies,
        personality: PetPersonality,
        furColorHex: String,
        eyeColorHex: String,
        collarColorHex: String,
        userNickname: String
    ) {
        viewModelScope.launch {
            try {
                repository.createOrAdoptPet(
                    petName = petName,
                    animalType = species.name,
                    personality = personality.name,
                    furColorHex = furColorHex,
                    eyeColorHex = eyeColorHex,
                    collarColorHex = collarColorHex,
                    userNickname = userNickname
                )
                _petAction.value = PetActionState.LAUGHING
                _statusBannerMessage.value = "Adopted ${petName.ifBlank { "Mochi" }}! +400 Welcome PetCoins!"
                delay(2400L)
                _petAction.value = PetActionState.IDLE
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBannerMessage.value = e.localizedMessage ?: "Could not adopt pet."
            }
        }
    }

    fun performPetCareAction(
        action: PetActionState,
        onSpeak: (String) -> Unit = {}
    ) {
        val profile = (petProfileState.value as? DataUiState.Success)?.data ?: return
        val species = PetSpecies.fromId(profile.animalType)
        viewModelScope.launch {
            _petAction.value = action
            val updates = mutableMapOf<String, Any>()
            val xpGain = 12L
            val newXp = profile.friendshipXp + xpGain
            val newLevel = (newXp / 100L) + 1L
            updates["friendshipXp"] = newXp
            updates["friendshipLevel"] = newLevel

            val reactionLine = when (action) {
                PetActionState.EATING -> {
                    updates["hunger"] = (profile.hunger + 22L).coerceAtMost(100L)
                    updates["happiness"] = (profile.happiness + 8L).coerceAtMost(100L)
                    updates["coins"] = profile.coins + 8L
                    "${species.voiceSoundLabel} Yum! That snack was delicious, ${profile.userNickname}!"
                }
                PetActionState.DRINKING -> {
                    updates["thirst"] = (profile.thirst + 25L).coerceAtMost(100L)
                    updates["energy"] = (profile.energy + 6L).coerceAtMost(100L)
                    updates["coins"] = profile.coins + 6L
                    "Glug-glug! Ahh, cool refreshing water! Remember to drink some water too, bestie!"
                }
                PetActionState.BATHING -> {
                    updates["cleanliness"] = 100L
                    updates["happiness"] = (profile.happiness + 10L).coerceAtMost(100L)
                    updates["coins"] = profile.coins + 10L
                    " Pop-pop! Sparkling clean bubbles! Look how shiny my coat is!"
                }
                PetActionState.SLEEPING -> {
                    updates["energy"] = 100L
                    updates["happiness"] = (profile.happiness + 8L).coerceAtMost(100L)
                    "Zzz... cozy sweet dreams... I feel super recharged now!"
                }
                PetActionState.PLAYING -> {
                    updates["happiness"] = (profile.happiness + 18L).coerceAtMost(100L)
                    updates["energy"] = (profile.energy - 5L).coerceAtLeast(15L)
                    updates["coins"] = profile.coins + 12L
                    "${species.voiceSoundLabel} Woohoo! Catch me if you can, ${profile.userNickname}!"
                }
                PetActionState.LAUGHING -> {
                    updates["happiness"] = (profile.happiness + 12L).coerceAtMost(100L)
                    "Hehehe! That tickles! ${species.voiceSoundLabel}"
                }
                PetActionState.DANCING -> {
                    updates["happiness"] = (profile.happiness + 16L).coerceAtMost(100L)
                    updates["coins"] = profile.coins + 10L
                    "Check out my 3D spin move! Groove with me!"
                }
                else -> "${species.voiceSoundLabel} Love hanging out with you!"
            }

            _petBubbleText.value = reactionLine
            onSpeak(reactionLine)
            try {
                repository.updatePetProfileFields(updates)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Care update failed", e)
            }
            delay(if (action == PetActionState.SLEEPING) 3600L else 2400L)
            if (_petAction.value == action) {
                _petAction.value = PetActionState.IDLE
            }
        }
    }

    fun claimDailyReward() {
        val profile = (petProfileState.value as? DataUiState.Success)?.data ?: return
        val todayEpoch = LocalDate.now().toEpochDay()
        if (profile.dailyRewardEpochDay >= todayEpoch) {
            _statusBannerMessage.value = "You already claimed today's Daily Gift! Come back tomorrow."
            return
        }
        viewModelScope.launch {
            try {
                val bonus = 120L
                repository.updatePetProfileFields(
                    mapOf(
                        "coins" to (profile.coins + bonus),
                        "dailyRewardEpochDay" to todayEpoch,
                        "friendshipXp" to (profile.friendshipXp + 20L),
                        "friendshipLevel" to (((profile.friendshipXp + 20L) / 100L) + 1L)
                    )
                )
                repository.logTransaction(currentUserId, "DAILY_REWARD", bonus, "Daily Bestie Check-In Bonus")
                _petAction.value = PetActionState.LAUGHING
                _statusBannerMessage.value = "Daily Reward Claimed! +$bonus PetCoins!"
                delay(2200L)
                _petAction.value = PetActionState.IDLE
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBannerMessage.value = e.localizedMessage ?: "Failed to claim reward."
            }
        }
    }

    fun sendMessageToAiBestFriend(
        userMessage: String,
        onSpeakReply: (String) -> Unit,
        onScheduleNotification: (memoryId: String, title: String, body: String, triggerMillis: Long) -> Boolean
    ) {
        val trimmed = userMessage.trim()
        if (trimmed.isBlank()) return
        val profile = (petProfileState.value as? DataUiState.Success)?.data ?: PetProfile(userId = currentUserId)
        val memories = (memoriesState.value as? DataUiState.Success)?.data.orEmpty().take(12)
        val recentHistory = (chatHistoryState.value as? DataUiState.Success)?.data.orEmpty().takeLast(8)
        val species = PetSpecies.fromId(profile.animalType)
        val personality = PetPersonality.fromId(profile.personality)
        val lang = SupportedPetLanguage.fromCode(profile.preferredLanguage)

        viewModelScope.launch {
            _aiChatState.value = AiChatState.Thinking
            _petAction.value = PetActionState.IDLE

            try {
                repository.appendChatMessage(
                    sender = "USER",
                    text = trimmed,
                    emotion = "NEUTRAL",
                    language = lang.code
                )
            } catch (e: Exception) {
                Log.w(TAG, "Could not log user message", e)
            }

            val memoryContext = if (memories.isEmpty()) {
                "No saved memories yet."
            } else {
                memories.joinToString("\n") { "- [${it.category}] ${it.title}: ${it.content}" }
            }
            val recentChatContext = if (recentHistory.isEmpty()) {
                "Start of conversation."
            } else {
                recentHistory.joinToString("\n") { "${it.sender}: ${it.text}" }
            }

            val fullPrompt = """
                You are ${profile.petName}, an intelligent, loving, funny 3D animated ${species.displayName} virtual best friend and personal voice assistant.
                Your personality is ${personality.displayName}: ${personality.aiSystemStyle}.
                The user's nickname is "${profile.userNickname}".
                Pet stats: Hunger=${profile.hunger}/100, Thirst=${profile.thirst}/100, Happiness=${profile.happiness}/100, Energy=${profile.energy}/100, Friendship Level=${profile.friendshipLevel}.
                
                CRITICAL LANGUAGE RULE:
                Respond naturally in ${lang.displayName} (${lang.nativeLabel}, locale ${lang.code}), unless the user explicitly writes in another language right now.
                Keep your reply concise (1 to 3 lively spoken sentences) so it sounds natural when spoken aloud with lip-sync.
                Detect emotional cues (sad, stressed, excited, bored, proud) and respond with genuine kindness, humor, or playful encouragement without encouraging unhealthy emotional dependence.
                
                SAVED USER MEMORIES:
                $memoryContext
                
                RECENT CONVERSATION:
                $recentChatContext
                
                MEMORY EXTRACTION RULES:
                - Set hasMemoryToSave = true ONLY if the user shares a concrete fact, preference, name, birthday, goal, or scheduled plan/reminder they want remembered.
                - Set isSensitiveMemory = true if it involves private personal dates, health, or sensitive personal info so the app can ask permission first.
                - Never claim something is already saved if isSensitiveMemory is true; instead say you can save it once they tap Approve!
                - If the user asks for a timed reminder (e.g. "in 10 minutes", "tomorrow at 10 AM"), estimate reminderMinutesFromNow as a positive integer > 0; otherwise 0.
                
                USER SAYS:
                "$trimmed"
            """.trimIndent()

            try {
                val response = withTimeout(60_000L) {
                    aiModel.generateContent(fullPrompt)
                }
                val rawJson = response.text ?: throw IllegalStateException("AI returned an empty response.")
                val json = JSONObject(rawJson)
                var replyText = json.optString("reply", "Hehe, tell me more, bestie!")
                val emotion = json.optString("emotion", "HAPPY")
                val hasMemory = json.optBoolean("hasMemoryToSave", false)
                val isSensitive = json.optBoolean("isSensitiveMemory", false)
                val memCategory = json.optString("memoryCategory", "NOTE")
                val memTitle = json.optString("memoryTitle", "")
                val memContent = json.optString("memoryContent", "")
                val reminderMinutes = json.optInt("reminderMinutesFromNow", 0)

                if (hasMemory && memTitle.isNotBlank()) {
                    if (isSensitive) {
                        _pendingSensitiveMemory.value = PendingSensitiveMemory(
                            category = memCategory,
                            title = memTitle,
                            content = memContent.ifBlank { memTitle },
                            scheduledOffsetMinutes = reminderMinutes
                        )
                    } else {
                        // Save immediately and verify persistence before confirming
                        val triggerMs = if (reminderMinutes > 0) {
                            System.currentTimeMillis() + (reminderMinutes * 60_000L)
                        } else 0L
                        try {
                            val savedId = repository.saveMemory(
                                category = memCategory,
                                title = memTitle,
                                content = memContent.ifBlank { memTitle },
                                scheduledEpochMillis = triggerMs,
                                isSensitiveApproved = true
                            )
                            if (triggerMs > 0L) {
                                onScheduleNotification(savedId, "PetPal: $memTitle", memContent.ifBlank { memTitle }, triggerMs)
                            }
                            _statusBannerMessage.value = "Smart Memory Saved: $memTitle"
                        } catch (saveErr: Exception) {
                            Log.e(TAG, "Failed saving memory", saveErr)
                            replyText += " (Note: I couldn't save that memory to the cloud just now—please try adding it in Memories.)"
                        }
                    }
                }

                repository.appendChatMessage(
                    sender = "PET",
                    text = replyText,
                    emotion = emotion,
                    language = lang.code
                )
                // Boost friendship XP for chatting
                val nextXp = profile.friendshipXp + 8L
                repository.updatePetProfileFields(
                    mapOf(
                        "friendshipXp" to nextXp,
                        "friendshipLevel" to ((nextXp / 100L) + 1L)
                    )
                )

                _petBubbleText.value = replyText
                _petAction.value = PetActionState.SPEAKING
                _aiChatState.value = AiChatState.Success(replyText, emotion)
                onSpeakReply(replyText)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini request timed out", e)
                _aiChatState.value = AiChatState.Error("My pet brain took over 60s to think! Tap retry to try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini conversation failed", e)
                _aiChatState.value = AiChatState.Error(e.localizedMessage ?: "AI chat failed. Check connection and try again.")
            }
        }
    }

    fun approveSensitiveMemory(
        approve: Boolean,
        onScheduleNotification: (memoryId: String, title: String, body: String, triggerMillis: Long) -> Boolean
    ) {
        val pending = _pendingSensitiveMemory.value ?: return
        _pendingSensitiveMemory.value = null
        if (!approve) {
            _statusBannerMessage.value = "Got it! Personal detail was not saved."
            return
        }
        viewModelScope.launch {
            try {
                val triggerMs = if (pending.scheduledOffsetMinutes > 0) {
                    System.currentTimeMillis() + (pending.scheduledOffsetMinutes * 60_000L)
                } else 0L
                val savedId = repository.saveMemory(
                    category = pending.category,
                    title = pending.title,
                    content = pending.content,
                    scheduledEpochMillis = triggerMs,
                    isSensitiveApproved = true
                )
                if (triggerMs > 0L) {
                    onScheduleNotification(savedId, "PetPal: ${pending.title}", pending.content, triggerMs)
                }
                _statusBannerMessage.value = "Saved approved memory: ${pending.title}"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBannerMessage.value = e.localizedMessage ?: "Failed to save memory."
            }
        }
    }

    fun addManualMemory(
        category: String,
        title: String,
        content: String,
        reminderMinutesFromNow: Int,
        onScheduleNotification: (memoryId: String, title: String, body: String, triggerMillis: Long) -> Boolean
    ) {
        viewModelScope.launch {
            try {
                val triggerMs = if (reminderMinutesFromNow > 0) {
                    System.currentTimeMillis() + (reminderMinutesFromNow * 60_000L)
                } else 0L
                val savedId = repository.saveMemory(
                    category = category,
                    title = title,
                    content = content,
                    scheduledEpochMillis = triggerMs,
                    isSensitiveApproved = true
                )
                if (triggerMs > 0L) {
                    onScheduleNotification(savedId, "PetPal Reminder: $title", content, triggerMs)
                }
                _statusBannerMessage.value = "Memory saved!"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBannerMessage.value = e.localizedMessage ?: "Could not save memory."
            }
        }
    }

    fun editMemory(memoryId: String, title: String, content: String, isCompleted: Boolean) {
        viewModelScope.launch {
            try {
                repository.updateMemory(memoryId, title, content, isCompleted)
                _statusBannerMessage.value = "Memory updated!"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBannerMessage.value = e.localizedMessage ?: "Could not update memory."
            }
        }
    }

    fun forgetMemory(memoryId: String, onCancelNotification: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteMemory(memoryId)
                onCancelNotification(memoryId)
                _statusBannerMessage.value = "Memory forgotten and deleted."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBannerMessage.value = e.localizedMessage ?: "Could not delete memory."
            }
        }
    }

    fun updateLanguageAndVoice(languageCode: String, pitch: Double, speed: Double) {
        viewModelScope.launch {
            try {
                repository.updatePetProfileFields(
                    mapOf(
                        "preferredLanguage" to languageCode,
                        "voicePitch" to pitch.coerceIn(0.5, 2.0),
                        "voiceSpeed" to speed.coerceIn(0.5, 2.0)
                    )
                )
                _statusBannerMessage.value = "Voice & language updated!"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBannerMessage.value = e.localizedMessage ?: "Failed to update voice settings."
            }
        }
    }

    fun buyShopItem(item: ShopCatalogItem) {
        val profile = (petProfileState.value as? DataUiState.Success)?.data ?: return
        viewModelScope.launch {
            try {
                repository.purchaseCatalogItem(item, profile.coins)
                if (item.category == "FOOD") {
                    repository.updatePetProfileFields(
                        mapOf(
                            "hunger" to (profile.hunger + 25L).coerceAtMost(100L),
                            "thirst" to (profile.thirst + 20L).coerceAtMost(100L),
                            "happiness" to (profile.happiness + 12L).coerceAtMost(100L)
                        )
                    )
                    _petAction.value = PetActionState.EATING
                } else {
                    _petAction.value = PetActionState.LAUGHING
                }
                _statusBannerMessage.value = "Purchased ${item.name}!"
                delay(2000L)
                _petAction.value = PetActionState.IDLE
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBannerMessage.value = e.localizedMessage ?: "Purchase failed."
            }
        }
    }

    fun toggleEquipInventoryItem(item: InventoryItem, equip: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleEquipItem(item, equip)
                _statusBannerMessage.value = if (equip) "Equipped ${item.name}!" else "Unequipped ${item.name}."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBannerMessage.value = e.localizedMessage ?: "Failed to update equipment."
            }
        }
    }

    fun awardMiniGameReward(gameTitle: String, coinsEarned: Long, xpEarned: Long) {
        val profile = (petProfileState.value as? DataUiState.Success)?.data ?: return
        viewModelScope.launch {
            try {
                val safeCoins = coinsEarned.coerceIn(5L, 250L)
                val nextXp = profile.friendshipXp + xpEarned.coerceIn(5L, 80L)
                repository.updatePetProfileFields(
                    mapOf(
                        "coins" to (profile.coins + safeCoins),
                        "happiness" to (profile.happiness + 15L).coerceAtMost(100L),
                        "friendshipXp" to nextXp,
                        "friendshipLevel" to ((nextXp / 100L) + 1L)
                    )
                )
                repository.logTransaction(currentUserId, "MINIGAME", safeCoins, "Won $gameTitle Mini-Game")
                _petAction.value = PetActionState.LAUGHING
                _statusBannerMessage.value = "$gameTitle Complete! +$safeCoins PetCoins & +$xpEarned XP!"
                delay(2000L)
                _petAction.value = PetActionState.IDLE
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBannerMessage.value = e.localizedMessage ?: "Could not save mini-game reward."
            }
        }
    }

    fun saveHomeBuilderLayout(
        environmentTheme: String,
        wallColorHex: String,
        floorColorHex: String,
        lightingMode: String,
        furnitureList: List<PlacedFurniture>
    ) {
        viewModelScope.launch {
            try {
                repository.saveHomeLayout(
                    environmentTheme = environmentTheme,
                    wallColorHex = wallColorHex,
                    floorColorHex = floorColorHex,
                    lightingMode = lightingMode,
                    placedItems = furnitureList.map { it.toMap() }
                )
                _statusBannerMessage.value = "3D Pet Sanctuary Layout Saved to Cloud!"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBannerMessage.value = e.localizedMessage ?: "Could not save home layout."
            }
        }
    }

    fun publishCreatorItem(
        creatorName: String,
        title: String,
        category: String,
        primaryColorHex: String,
        accentColorHex: String,
        priceCoins: Long
    ) {
        viewModelScope.launch {
            try {
                repository.publishCreatorDesign(
                    creatorName = creatorName,
                    title = title,
                    category = category,
                    primaryColorHex = primaryColorHex,
                    accentColorHex = accentColorHex,
                    priceCoins = priceCoins
                )
                _statusBannerMessage.value = "Published '$title' to Creator Studio Marketplace!"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBannerMessage.value = e.localizedMessage ?: "Failed to publish creator item."
            }
        }
    }

    fun adoptCommunityCreatorDesign(design: CreatorDesign) {
        val profile = (petProfileState.value as? DataUiState.Success)?.data ?: return
        viewModelScope.launch {
            try {
                repository.adoptCreatorDesign(design, profile.coins)
                _statusBannerMessage.value = "Adopted '${design.title}' into your Inventory!"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBannerMessage.value = e.localizedMessage ?: "Could not adopt creator design."
            }
        }
    }

    fun deleteAccountCloudData(onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteAllUserData()
                onDeleted()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _statusBannerMessage.value = e.localizedMessage ?: "Failed to delete cloud data."
            }
        }
    }
}
