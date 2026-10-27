package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.ChatMessageRecord
import com.example.data.model.CoinTransaction
import com.example.data.model.CreatorDesign
import com.example.data.model.HomeLayout
import com.example.data.model.InventoryItem
import com.example.data.model.PetMemory
import com.example.data.model.PetProfile
import com.example.data.model.ShopCatalogItem
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class PetPalRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // 1. PetProfile Operations
    fun observePetProfile(userId: String): Flow<PetProfile?> {
        val docRef = db.collection("users").document(userId)
        return docRef.snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(PetProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { err ->
                if (err is Exception) handleFirestoreError(err, OperationType.GET, docRef.path)
                throw err
            }
    }

    suspend fun getPetProfile(userId: String): PetProfile? {
        val docRef = db.collection("users").document(userId)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                snap.toObject(PetProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun createOrAdoptPet(
        petName: String,
        animalType: String,
        personality: String,
        furColorHex: String,
        eyeColorHex: String,
        collarColorHex: String,
        userNickname: String
    ) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val payload = mapOf(
            "userId" to uid,
            "petName" to petName.trim().take(40).ifBlank { "Mochi" },
            "animalType" to animalType,
            "personality" to personality,
            "furColorHex" to furColorHex,
            "eyeColorHex" to eyeColorHex,
            "collarColorHex" to collarColorHex,
            "equippedHatId" to "",
            "equippedOutfitId" to "",
            "equippedToyId" to "",
            "hunger" to 90L,
            "thirst" to 90L,
            "happiness" to 95L,
            "energy" to 92L,
            "cleanliness" to 100L,
            "friendshipXp" to 25L,
            "friendshipLevel" to 1L,
            "coins" to 400L,
            "preferredLanguage" to "en-US",
            "voicePitch" to 1.12,
            "voiceSpeed" to 1.0,
            "userNickname" to userNickname.trim().take(40).ifBlank { "Bestie" },
            "dailyRewardEpochDay" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            logTransaction(uid, "DAILY_REWARD", 400L, "Welcome Adoption Gift for ${petName.trim().ifBlank { "Mochi" }}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updatePetProfileFields(updates: Map<String, Any>) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val finalUpdates = updates + ("updatedAt" to FieldValue.serverTimestamp())
        try {
            docRef.update(finalUpdates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // 2. Smart Pet Memory Operations
    fun observeMemories(userId: String): Flow<List<PetMemory>> {
        val colRef = db.collection("users").document(userId).collection("memories")
        return colRef.whereEqualTo("userId", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(PetMemory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { err ->
                if (err is Exception) handleFirestoreError(err, OperationType.LIST, colRef.path)
                throw err
            }
    }

    suspend fun saveMemory(
        category: String,
        title: String,
        content: String,
        scheduledEpochMillis: Long = 0L,
        isSensitiveApproved: Boolean = true
    ): String {
        val uid = requireUserId()
        val memId = "mem_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("users").document(uid).collection("memories").document(memId)
        val payload = mapOf(
            "userId" to uid,
            "category" to category.take(32),
            "title" to title.trim().take(120).ifBlank { "Saved Memory" },
            "content" to content.trim().take(1000).ifBlank { title.trim().take(120) },
            "scheduledEpochMillis" to scheduledEpochMillis.coerceAtLeast(0L),
            "isCompleted" to false,
            "isSensitiveApproved" to isSensitiveApproved,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return memId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateMemory(memoryId: String, title: String, content: String, isCompleted: Boolean) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("memories").document(memoryId)
        val updates = mapOf(
            "title" to title.trim().take(120).ifBlank { "Saved Memory" },
            "content" to content.trim().take(1000),
            "isCompleted" to isCompleted,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteMemory(memoryId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("memories").document(memoryId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // 3. Inventory & Virtual Coin Shop Operations
    fun observeInventory(userId: String): Flow<List<InventoryItem>> {
        val colRef = db.collection("users").document(userId).collection("inventory")
        return colRef.whereEqualTo("userId", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(InventoryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { err ->
                if (err is Exception) handleFirestoreError(err, OperationType.LIST, colRef.path)
                throw err
            }
    }

    suspend fun purchaseCatalogItem(item: ShopCatalogItem, currentCoins: Long) {
        val uid = requireUserId()
        require(currentCoins >= item.priceCoins) { "Not enough PetCoins! Play mini-games or claim daily rewards." }
        val userRef = db.collection("users").document(uid)
        val invRef = userRef.collection("inventory").document(item.id)
        val txId = "tx_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val txRef = userRef.collection("transactions").document(txId)

        try {
            val invSnap = invRef.get().await()
            val batch = db.batch()
            batch.update(
                userRef,
                mapOf(
                    "coins" to (currentCoins - item.priceCoins).coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (invSnap.exists()) {
                val existingQty = invSnap.getLong("quantity") ?: 1L
                batch.update(
                    invRef,
                    mapOf(
                        "quantity" to (existingQty + 1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    invRef,
                    mapOf(
                        "userId" to uid,
                        "catalogItemId" to item.id,
                        "name" to item.name,
                        "category" to item.category,
                        "quantity" to 1L,
                        "isEquipped" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.set(
                txRef,
                mapOf(
                    "userId" to uid,
                    "type" to "SPEND",
                    "amount" to -item.priceCoins,
                    "description" to "Purchased ${item.name}",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, invRef.path)
            throw e
        }
    }

    suspend fun toggleEquipItem(item: InventoryItem, equip: Boolean) {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val invRef = userRef.collection("inventory").document(item.id)
        val profileField = when (item.category) {
            "ACCESSORY" -> "equippedHatId"
            "OUTFIT" -> "equippedOutfitId"
            "TOY" -> "equippedToyId"
            else -> null
        }
        try {
            val batch = db.batch()
            batch.update(invRef, mapOf("isEquipped" to equip, "updatedAt" to FieldValue.serverTimestamp()))
            if (profileField != null) {
                batch.update(
                    userRef,
                    mapOf(
                        profileField to if (equip) item.catalogItemId else "",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, invRef.path)
            throw e
        }
    }

    // 4. Transactions Ledger
    fun observeTransactions(userId: String): Flow<List<CoinTransaction>> {
        val colRef = db.collection("users").document(userId).collection("transactions")
        return colRef.whereEqualTo("userId", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(CoinTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { err ->
                if (err is Exception) handleFirestoreError(err, OperationType.LIST, colRef.path)
                throw err
            }
    }

    suspend fun logTransaction(userId: String, type: String, amount: Long, description: String) {
        val txId = "tx_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val txRef = db.collection("users").document(userId).collection("transactions").document(txId)
        val payload = mapOf(
            "userId" to userId,
            "type" to type,
            "amount" to amount,
            "description" to description.take(200),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            txRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, txRef.path)
            throw e
        }
    }

    // 5. Home Builder Layouts
    fun observeHomeLayout(userId: String): Flow<HomeLayout?> {
        val docRef = db.collection("users").document(userId).collection("home_layouts").document("primary_home")
        return docRef.snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(HomeLayout::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { err ->
                if (err is Exception) handleFirestoreError(err, OperationType.GET, docRef.path)
                throw err
            }
    }

    suspend fun saveHomeLayout(
        environmentTheme: String,
        wallColorHex: String,
        floorColorHex: String,
        lightingMode: String,
        placedItems: List<Map<String, Any>>
    ) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("home_layouts").document("primary_home")
        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "environmentTheme" to environmentTheme,
                        "wallColorHex" to wallColorHex,
                        "floorColorHex" to floorColorHex,
                        "lightingMode" to lightingMode,
                        "placedItems" to placedItems.take(60),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "environmentTheme" to environmentTheme,
                        "wallColorHex" to wallColorHex,
                        "floorColorHex" to floorColorHex,
                        "lightingMode" to lightingMode,
                        "placedItems" to placedItems.take(60),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // 6. AI Chat Message History
    fun observeChatHistory(userId: String): Flow<List<ChatMessageRecord>> {
        val colRef = db.collection("users").document(userId).collection("messages")
        return colRef.whereEqualTo("userId", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(ChatMessageRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
            }
            .catch { err ->
                if (err is Exception) handleFirestoreError(err, OperationType.LIST, colRef.path)
                throw err
            }
    }

    suspend fun appendChatMessage(sender: String, text: String, emotion: String, language: String) {
        val uid = requireUserId()
        val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("users").document(uid).collection("messages").document(msgId)
        val payload = mapOf(
            "userId" to uid,
            "sender" to sender,
            "text" to text.trim().take(4000).ifBlank { "..." },
            "emotion" to emotion.take(32),
            "language" to language.take(20),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // 7. Creator Studio Community Designs
    fun observeApprovedCreatorDesigns(): Flow<List<CreatorDesign>> {
        val colRef = db.collection("creator_designs")
        return colRef.whereEqualTo("moderationStatus", "APPROVED")
            .snapshots()
            .map { snap ->
                snap.toObjects(CreatorDesign::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.downloadsCount }
            }
            .catch { err ->
                if (err is Exception) handleFirestoreError(err, OperationType.LIST, colRef.path)
                throw err
            }
    }

    suspend fun publishCreatorDesign(
        creatorName: String,
        title: String,
        category: String,
        primaryColorHex: String,
        accentColorHex: String,
        priceCoins: Long
    ): String {
        val uid = requireUserId()
        val designId = "design_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("creator_designs").document(designId)
        val payload = mapOf(
            "creatorId" to uid,
            "creatorName" to creatorName.trim().take(60).ifBlank { "Creator" },
            "title" to title.trim().take(80).ifBlank { "Custom Pet Gear" },
            "category" to category,
            "primaryColorHex" to primaryColorHex,
            "accentColorHex" to accentColorHex,
            "priceCoins" to priceCoins.coerceIn(10L, 50000L),
            "moderationStatus" to "APPROVED",
            "downloadsCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return designId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun adoptCreatorDesign(design: CreatorDesign, currentCoins: Long) {
        val uid = requireUserId()
        require(currentCoins >= design.priceCoins) { "Not enough PetCoins to adopt this creator item!" }
        val designRef = db.collection("creator_designs").document(design.id)
        val userRef = db.collection("users").document(uid)
        val invRef = userRef.collection("inventory").document("creator_${design.id}")
        val txId = "tx_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val txRef = userRef.collection("transactions").document(txId)

        try {
            val batch = db.batch()
            batch.update(
                userRef,
                mapOf(
                    "coins" to (currentCoins - design.priceCoins).coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                invRef,
                mapOf(
                    "userId" to uid,
                    "catalogItemId" to "creator_${design.id}",
                    "name" to design.title,
                    "category" to design.category,
                    "quantity" to 1L,
                    "isEquipped" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                designRef,
                mapOf(
                    "downloadsCount" to (design.downloadsCount + 1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                txRef,
                mapOf(
                    "userId" to uid,
                    "type" to "CREATOR_SALE",
                    "amount" to -design.priceCoins,
                    "description" to "Adopted Creator Item: ${design.title} by ${design.creatorName}",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, designRef.path)
            throw e
        }
    }

    // 8. Account & Data Deletion (Privacy & Play Policy)
    suspend fun deleteAllUserData() {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        try {
            val subcollections = listOf("memories", "inventory", "transactions", "home_layouts", "messages")
            for (sub in subcollections) {
                val docs = userRef.collection(sub).whereEqualTo("userId", uid).get().await()
                for (doc in docs.documents) {
                    doc.reference.delete().await()
                }
            }
            userRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, userRef.path)
            throw e
        }
    }
}
