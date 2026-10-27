package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.PetPersonality
import com.example.data.model.PetShopCatalog
import com.example.data.model.PetSpecies
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PetPalRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun adoptPet_and_saveMemory_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser("alice_pet@test.com")
        val repo = PetPalRepository(firestore)

        withTimeout(5000L) {
            repo.createOrAdoptPet(
                petName = "Biscuit",
                animalType = PetSpecies.FOX.name,
                personality = PetPersonality.FUNNY_NAUGHTY.name,
                furColorHex = "#EA580C",
                eyeColorHex = "#F59E0B",
                collarColorHex = "#6366F1",
                userNickname = "Alice"
            )
        }

        val profile = withTimeout(5000L) { repo.getPetProfile(aliceUid) }
        assertNotNull(profile)
        assertEquals("Biscuit", profile?.petName)

        val memId = withTimeout(5000L) {
            repo.saveMemory(
                category = "PLAN",
                title = "Team Standup",
                content = "Meeting tomorrow at 10 AM",
                scheduledEpochMillis = 1900000000000L,
                isSensitiveApproved = true
            )
        }
        assertTrue(memId.isNotEmpty())

        val memories = withTimeout(4000L) {
            repo.observeMemories(aliceUid).first { list -> list.any { it.id == memId } }
        }
        assertTrue(memories.any { it.title == "Team Standup" })

        // Test shop purchase
        val cake = PetShopCatalog.items.first()
        withTimeout(5000L) {
            repo.purchaseCatalogItem(cake, profile?.coins ?: 400L)
        }
        val inv = withTimeout(4000L) {
            repo.observeInventory(aliceUid).first { list -> list.any { it.catalogItemId == cake.id } }
        }
        assertTrue(inv.isNotEmpty())
    }

    @Test
    fun crossUserAccess_bobCannotReadAlicePetProfile_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_iso@test.com")
        val repo = PetPalRepository(firestore)
        withTimeout(5000L) {
            repo.createOrAdoptPet(
                petName = "Luna",
                animalType = PetSpecies.UNICORN.name,
                personality = PetPersonality.SWEET_CARING.name,
                furColorHex = "#F5D0FE",
                eyeColorHex = "#8B5CF6",
                collarColorHex = "#EC4899",
                userNickname = "Alice"
            )
        }

        signInTestUser("bob_iso@test.com")
        var caught: Exception? = null
        try {
            withTimeout(5000L) { repo.getPetProfile(aliceUid) }
        } catch (e: Exception) {
            caught = e
        }
        assertNotNull(caught)
        val fsEx = caught?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun unauthenticatedCaller_observeMemories_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = PetPalRepository(firestore)

        var caught: Exception? = null
        try {
            withTimeout(4000L) {
                repo.observeMemories("unauth_uid_123").first()
            }
        } catch (e: Exception) {
            caught = e
        }
        assertNotNull(caught)
        val fsEx = checkNotNull(caught?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException inside $caught"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
    }
}
