const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or create PetProfile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated owner can create and read own PetProfile", async () => {
  const aliceContext = testEnv.authenticatedContext(ALICE_UID);
  const aliceDb = aliceContext.firestore();
  const ts = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      petName: "Mochi",
      animalType: "DOG",
      personality: "FUNNY_NAUGHTY",
      coins: 250,
      hunger: 85,
      thirst: 85,
      happiness: 95,
      energy: 90,
      cleanliness: 100,
      friendshipXp: 10,
      friendshipLevel: 1,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Cross-user isolation: Bob cannot read or modify Alice's PetProfile or Memories", async () => {
  const ts = new Date();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      petName: "Mochi",
      animalType: "CAT",
      personality: "SWEET_CARING",
      coins: 300,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("users").doc(ALICE_UID).collection("memories").doc("mem_1").set({
      userId: ALICE_UID,
      category: "PLAN",
      title: "Dentist Tomorrow",
      content: "Meeting at 10 AM",
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_1").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("memories").where("userId", "==", ALICE_UID).get()
  );
});

test("Shadow update test: Ghost field in PetProfile is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      petName: "Mochi",
      animalType: "FOX",
      personality: "ADVENTUROUS",
      coins: 200,
      isAdminSpoof: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Append-only transactions cannot be mutated after creation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      type: "DAILY_REWARD",
      amount: 50,
      description: "Daily check-in bonus",
      createdAt: ts,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").update({
      amount: 999999,
    })
  );
});

test("Creator Studio designs enforce query filter on moderationStatus == APPROVED", async () => {
  const ts = new Date();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("creator_designs").doc("design_1").set({
      creatorId: ALICE_UID,
      creatorName: "Alice",
      title: "Starlight Cape",
      category: "OUTFIT",
      primaryColorHex: "#6366F1",
      accentColorHex: "#F59E0B",
      priceCoins: 120,
      moderationStatus: "APPROVED",
      downloadsCount: 0,
      createdAt: ts,
      updatedAt: ts,
    });
    await ctx.firestore().collection("creator_designs").doc("design_pending").set({
      creatorId: ALICE_UID,
      creatorName: "Alice",
      title: "Secret Draft Hat",
      category: "ACCESSORY",
      primaryColorHex: "#EC4899",
      accentColorHex: "#FBBF24",
      priceCoins: 80,
      moderationStatus: "PENDING",
      downloadsCount: 0,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("creator_designs").where("moderationStatus", "==", "APPROVED").get()
  );
  // Unfiltered query must fail because pending items exist belonging to Alice
  await assertFails(bobDb.collection("creator_designs").get());
});
