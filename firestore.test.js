const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DATABASE_ID = "ai-studio-android-e547b8f5-ef0e-49a9-96b8-b8956da99d71";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validEntryPayload(uid = ALICE_UID, overrides = {}) {
  const now = new Date();
  return {
    userId: uid,
    dateIso: "2026-10-25",
    monthDay: "10-25",
    moodKey: "BLOOM",
    moodIntensity: 5,
    title: "Теплый осенний день",
    content: "Сегодня я почувствовал спокойствие и благодарность во время прогулки.",
    reflectionPrompt: "Что сегодня дало мне ощущение опоры?",
    reflectionAnswer: "Разговор с близким человеком и тихий вечер.",
    tags: ["природа", "благодарность"],
    photosBase64: [],
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

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

test("Unauthenticated user: cannot read or create diary entries", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore(DATABASE_ID);
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("entries").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("entries")
      .doc("entry_1")
      .set(validEntryPayload(ALICE_UID))
  );
});

test("Authenticated owner: can create, read, list, update, and delete own diary entry", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("entries")
    .doc("2026-10-25");

  const payload = validEntryPayload(ALICE_UID);
  await assertSucceeds(docRef.set(payload));
  await assertSucceeds(docRef.get());
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("entries")
      .where("userId", "==", ALICE_UID)
      .get()
  );
  await assertSucceeds(
    docRef.update({
      title: "Обновленный заголовок",
      moodKey: "CALM_LEAF",
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(docRef.delete());
});

test("Cross-user isolation: Bob cannot read, list, update, or delete Alice's entry", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore(DATABASE_ID)
      .collection("users")
      .doc(ALICE_UID)
      .collection("entries")
      .doc("alice_entry")
      .set(validEntryPayload(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore(DATABASE_ID);
  const aliceDocFromBob = bobDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("entries")
    .doc("alice_entry");

  await assertFails(aliceDocFromBob.get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("entries").get()
  );
  await assertFails(
    aliceDocFromBob.update({
      title: "Hacked",
      updatedAt: new Date(),
    })
  );
  await assertFails(aliceDocFromBob.delete());
});

test("Schema validation: rejects invalid moodKey, out-of-range intensity, or ghost fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const entriesRef = aliceDb.collection("users").doc(ALICE_UID).collection("entries");

  await assertFails(
    entriesRef.doc("bad_mood").set(validEntryPayload(ALICE_UID, { moodKey: "EMOJI_SMILE" }))
  );
  await assertFails(
    entriesRef.doc("bad_intensity").set(validEntryPayload(ALICE_UID, { moodIntensity: 99 }))
  );
  await assertFails(
    entriesRef.doc("ghost_field").set(validEntryPayload(ALICE_UID, { isAdmin: true }))
  );
  await assertFails(
    entriesRef.doc("spoof_uid").set(validEntryPayload(BOB_UID))
  );
});
