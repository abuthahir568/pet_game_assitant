package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class PetSpecies(
    val displayName: String,
    val emoji: String,
    val defaultFurHex: String,
    val defaultEyeHex: String,
    val voiceSoundLabel: String,
    val earStyle: String,
    val tailStyle: String,
    val specialTrait: String
) {
    DOG("Dog", "🐶", "#F59E0B", "#3B82F6", "Woof-woof! *happy pant*", "FLOPPY", "WAGGY", "Loyal golden buddy"),
    CAT("Cat", "🐱", "#F97316", "#10B981", "Purr~ Meow!", "POINTED", "CURLED", "Cozy purring acrobat"),
    RABBIT("Rabbit", "🐰", "#FCE7F3", "#EC4899", "Boing-boing! *nose twitch*", "LONG_BUNNY", "PUFF", "Cloud-hopping sweetheart"),
    PANDA("Panda", "🐼", "#F8FAFC", "#1E293B", "Munch-munch! *happy squeak*", "ROUND_DARK", "PUFF", "Bamboo cuddle champion"),
    FOX("Fox", "🦊", "#EA580C", "#F59E0B", "Yip-yip! Hehe!", "POINTED", "BUSHY", "Clever woodland trickster"),
    BEAR("Bear", "🐻", "#92400E", "#451A03", "Grrr-awr! Big bear hug!", "ROUND", "PUFF", "Warm honey-loving guardian"),
    TIGER("Tiger", "🐯", "#FB923C", "#CA8A04", "Chuff-roar! Let's go!", "ROUND", "STRIPED", "Striped energetic sprinter"),
    LION("Lion", "🦁", "#FBBF24", "#B45309", "Roooaaar! Majestic high-five!", "MANE", "TUFTED", "Golden-maned cheerleader"),
    MONKEY("Monkey", "🐵", "#B45309", "#1E293B", "Ooh-ooh ahh-ahh! Haha!", "SIDE_ROUND", "SPIRAL", "Playful joke master"),
    PENGUIN("Penguin", "🐧", "#1E293B", "#38BDF8", "Noot-noot! *happy waddle*", "NONE", "FLIPPER", "Tuxedo ice dancer"),
    PARROT("Parrot", "🦜", "#10B981", "#F43F5E", "Squawk! Bestie alert!", "CREST", "FEATHER", "Colorful beatboxer"),
    HAMSTER("Hamster", "🐹", "#FDE68A", "#1E293B", "Squeak-squeak! *stuffed cheeks*", "TINY_ROUND", "PUFF", "Pocket-sized snack collector"),
    UNICORN("Unicorn", "🦄", "#F5D0FE", "#8B5CF6", "Sparkle-neigh! ✨", "HORN_EARS", "RAINBOW", "Starlight wishweaver"),
    DRAGON("Dragon", "🐉", "#7C3AED", "#F59E0B", "Puff-rawr! *tiny smoke heart*", "HORNS_WINGS", "SPIKED", "Friendly ember flyer"),
    DINOSAUR("Dinosaur", "🦕", "#22C55E", "#15803D", "Stomp-rawr! Dino dance!", "CREST", "DINO", "Prehistoric party pal"),
    TURTLE("Turtle", "🐢", "#14B8A6", "#0F766E", "Slow & steady! *happy blink*", "NONE", "TINY", "Zen wisdom keeper"),
    ELEPHANT("Elephant", "🐘", "#94A3B8", "#334155", "Pawoooo! *trunk heart*", "BIG_FLOPPY", "TUFTED", "Never forgets a memory"),
    DEER("Deer", "🦌", "#D97706", "#451A03", "Bleet! *graceful leap*", "ANTLERS", "PUFF", "Forest meadow dreamer"),
    WOLF("Wolf", "🐺", "#64748B", "#0EA5E9", "Awooooo! Pack besties!", "POINTED", "BUSHY", "Moonlight adventure guide"),
    FANTASY("Celestial Kitsune", "✨", "#818CF8", "#F472B6", "Chime-chime! Cosmic hug!", "STAR_EARS", "NINE_TAIL", "Mythical galaxy spirit");

    companion object {
        fun fromId(id: String): PetSpecies = entries.find { it.name == id } ?: DOG
    }
}

enum class PetPersonality(
    val displayName: String,
    val badge: String,
    val description: String,
    val aiSystemStyle: String
) {
    FUNNY_NAUGHTY(
        "Funny & Naughty",
        "😜",
        "Cracks witty jokes, playful harmless teasing, and silly sound effects.",
        "witty, playful, cracks clean funny jokes, gently teases like a goofy best friend while always being warm and respectful"
    ),
    SWEET_CARING(
        "Sweet & Caring",
        "🥰",
        "Gentle, encouraging, celebrates your small wins, and checks in kindly.",
        "extra warm, affectionate, validating, gentle, celebrates every win and offers comforting kindness"
    ),
    ENERGETIC_PLAYFUL(
        "Energetic & Playful",
        "⚡",
        "Loves games, dance challenges, riddles, and hyping up your day!",
        "bouncy, enthusiastic, action-oriented, suggests quick word games or riddles, full of upbeat cheer"
    ),
    CALM_INTELLIGENT(
        "Calm & Intelligent",
        "🧠",
        "Thoughtful storyteller, curious listener, and mindful companion.",
        "calm, insightful, curious, shares fascinating bite-sized stories, helps organize plans clearly"
    ),
    ADVENTUROUS(
        "Adventurous",
        "🧭",
        "Bold explorer turning daily tasks and goals into epic quests!",
        "bold, imaginative, frames everyday goals like fun quests and celebrates bravery"
    ),
    SHY_CUTE(
        "Shy & Cute",
        "🌸",
        "Soft-spoken, bashful giggles, and deeply loyal once warmed up.",
        "adorably polite, slightly bashful at first, super loyal, uses cute expressions"
    );

    companion object {
        fun fromId(id: String): PetPersonality = entries.find { it.name == id } ?: FUNNY_NAUGHTY
    }
}

enum class PetActionState {
    IDLE,
    WALKING,
    RUNNING,
    SLEEPING,
    EATING,
    DRINKING,
    PLAYING,
    LAUGHING,
    SPEAKING,
    BATHING,
    DANCING
}

enum class SupportedPetLanguage(
    val code: String,
    val displayName: String,
    val nativeLabel: String
) {
    ENGLISH("en-US", "English", "English"),
    TAMIL("ta-IN", "Tamil", "தமிழ்"),
    HINDI("hi-IN", "Hindi", "हिन्दी"),
    TELUGU("te-IN", "Telugu", "తెలుగు"),
    MALAYALAM("ml-IN", "Malayalam", "മലയാളം"),
    KANNADA("kn-IN", "Kannada", "ಕನ್ನಡ"),
    ARABIC("ar-SA", "Arabic", "العربية"),
    SPANISH("es-ES", "Spanish", "Español"),
    FRENCH("fr-FR", "French", "Français"),
    JAPANESE("ja-JP", "Japanese", "日本語"),
    CHINESE("zh-CN", "Chinese", "中文");

    companion object {
        fun fromCode(code: String): SupportedPetLanguage =
            entries.find { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
    }
}

@IgnoreExtraProperties
data class PetProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val petName: String = "Mochi",
    val animalType: String = PetSpecies.DOG.name,
    val personality: String = PetPersonality.FUNNY_NAUGHTY.name,
    val furColorHex: String = "#F59E0B",
    val eyeColorHex: String = "#3B82F6",
    val collarColorHex: String = "#F43F5E",
    val equippedHatId: String = "",
    val equippedOutfitId: String = "",
    val equippedToyId: String = "",
    val hunger: Long = 85L,
    val thirst: Long = 85L,
    val happiness: Long = 92L,
    val energy: Long = 88L,
    val cleanliness: Long = 95L,
    val friendshipXp: Long = 20L,
    val friendshipLevel: Long = 1L,
    val coins: Long = 350L,
    val preferredLanguage: String = SupportedPetLanguage.ENGLISH.code,
    val voicePitch: Double = 1.12,
    val voiceSpeed: Double = 1.0,
    val userNickname: String = "Bestie",
    val dailyRewardEpochDay: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PetMemory(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: String = "NOTE",
    val title: String = "",
    val content: String = "",
    val scheduledEpochMillis: Long = 0L,
    val isCompleted: Boolean = false,
    val isSensitiveApproved: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class InventoryItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val catalogItemId: String = "",
    val name: String = "",
    val category: String = "FOOD",
    val quantity: Long = 1L,
    val isEquipped: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CoinTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "EARN",
    val amount: Long = 0L,
    val description: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PlacedFurniture(
    val uid: String = "",
    val itemId: String = "",
    val name: String = "",
    val emoji: String = "🛋️",
    val gridX: Long = 2L,
    val gridY: Long = 2L,
    val rotationDeg: Long = 0L,
    val scaleFactor: Double = 1.0
) {
    fun toMap(): Map<String, Any> = mapOf(
        "uid" to uid,
        "itemId" to itemId,
        "name" to name,
        "emoji" to emoji,
        "gridX" to gridX,
        "gridY" to gridY,
        "rotationDeg" to rotationDeg,
        "scaleFactor" to scaleFactor
    )
}

@IgnoreExtraProperties
data class HomeLayout(
    @DocumentId val id: String = "",
    val userId: String = "",
    val environmentTheme: String = "COZY_HOUSE",
    val wallColorHex: String = "#E0E7FF",
    val floorColorHex: String = "#FDE68A",
    val lightingMode: String = "DAYLIGHT",
    val placedItems: List<Map<String, Any>> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessageRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sender: String = "PET",
    val text: String = "",
    val emotion: String = "HAPPY",
    val language: String = "en-US",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CreatorDesign(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val title: String = "",
    val category: String = "OUTFIT",
    val primaryColorHex: String = "#6366F1",
    val accentColorHex: String = "#F59E0B",
    val priceCoins: Long = 100L,
    val moderationStatus: String = "APPROVED",
    val downloadsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class ShopCatalogItem(
    val id: String,
    val name: String,
    val category: String,
    val emoji: String,
    val priceCoins: Long,
    val description: String,
    val statBoostLabel: String,
    val colorHex: String
)

object PetShopCatalog {
    val items: List<ShopCatalogItem> = listOf(
        ShopCatalogItem("food_berry_cake", "Starberry Shortcake", "FOOD", "🍰", 25L, "Fluffy berry treat baked fresh!", "+25 Hunger, +10 Happy", "#FB7185"),
        ShopCatalogItem("food_golden_sushi", "Golden Bento Box", "FOOD", "🍱", 40L, "Nutritious super-meal for active pets.", "+40 Hunger, +15 Energy", "#F59E0B"),
        ShopCatalogItem("drink_coconut_splash", "Sparkling Coconut Dew", "FOOD", "🥥", 20L, "Refreshing island hydration.", "+35 Thirst, +8 Happy", "#38BDF8"),
        ShopCatalogItem("food_bamboo_smoothie", "Mint Bamboo Smoothie", "FOOD", "🧋", 30L, "Chilled herbal energy boost.", "+30 Thirst, +20 Energy", "#34D399"),
        ShopCatalogItem("outfit_star_cape", "Cosmic Hero Cape", "OUTFIT", "🦸", 120L, "Flowing velvet cape with glowing stars.", "Equip on 3D Pet", "#6366F1"),
        ShopCatalogItem("outfit_cozy_hoodie", "Pastel Cloud Hoodie", "OUTFIT", "🧥", 95L, "Ultra-soft streetwear hoodie.", "Equip on 3D Pet", "#F472B6"),
        ShopCatalogItem("outfit_tuxedo_vest", "Royal Gala Vest", "OUTFIT", "🤵", 150L, "Dapper bow-tie vest for party time.", "Equip on 3D Pet", "#1E293B"),
        ShopCatalogItem("hat_golden_crown", "Sunbeam Royal Crown", "ACCESSORY", "👑", 160L, "Gleaming 3D crown with ruby gems.", "Equip on Pet Head", "#FBBF24"),
        ShopCatalogItem("hat_wizard_cone", "Astral Wizard Hat", "ACCESSORY", "🧙", 130L, "Enchanted pointed hat with moon charm.", "Equip on Pet Head", "#8B5CF6"),
        ShopCatalogItem("hat_party_cap", "Confetti Party Hat", "ACCESSORY", "🥳", 75L, "Celebrates every day with your bestie!", "Equip on Pet Head", "#EC4899"),
        ShopCatalogItem("hat_cool_shades", "Neon Starlight Visor", "ACCESSORY", "🕶️", 90L, "Cool holographic visor.", "Equip on Pet Head", "#06B6D4"),
        ShopCatalogItem("toy_rainbow_ball", "Super Bounce Orb", "TOY", "⚽", 60L, "Interactive toy for Fetch & Play!", "+25 Happiness", "#10B981"),
        ShopCatalogItem("toy_flying_disc", "Aero Star Frisbee", "TOY", "🛸", 85L, "Glides across the garden!", "+30 Happiness", "#3B82F6"),
        ShopCatalogItem("toy_music_boombox", "Groove Beats Box", "TOY", "📻", 140L, "Triggers spontaneous pet dance parties!", "+35 Happiness", "#F43F5E"),
        ShopCatalogItem("furn_cloud_bed", "Velvet Cloud Bed", "FURNITURE", "🛏️", 150L, "Place in your 3D Pet Home for cozy naps.", "Home Builder Item", "#A78BFA"),
        ShopCatalogItem("furn_plush_sofa", "Marshmallow Sofa", "FURNITURE", "🛋️", 180L, "Cozy lounge sofa for the living room.", "Home Builder Item", "#FB7185"),
        ShopCatalogItem("furn_glow_lamp", "Firefly Lantern Lamp", "DECOR", "🏮", 90L, "Warm ambient lighting for night chats.", "Home Builder Item", "#FBBF24"),
        ShopCatalogItem("furn_bonsai_tree", "Sakura Spirit Tree", "DECOR", "🌸", 110L, "Blooming indoor garden centerpiece.", "Home Builder Item", "#F472B6"),
        ShopCatalogItem("furn_splash_pool", "Lagoon Mini Pool", "FURNITURE", "🏊", 220L, "Sparkling backyard splash zone!", "Home Builder Item", "#38BDF8"),
        ShopCatalogItem("furn_slide_park", "Rainbow Playground Slide", "FURNITURE", "🛝", 200L, "Outdoor fun right inside your sanctuary.", "Home Builder Item", "#34D399")
    )
}

fun parseHexColor(hex: String, fallback: Color = Color(0xFFF59E0B)): Color {
    return try {
        val cleaned = hex.trim().removePrefix("#")
        val longVal = when (cleaned.length) {
            6 -> ("FF$cleaned").toLong(16)
            8 -> cleaned.toLong(16)
            else -> return fallback
        }
        Color(longVal)
    } catch (_: Exception) {
        fallback
    }
}
