const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const serverTimestamp = () => new Date(Date.now() - 1000);

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

function validUserSettings(uid) {
  return {
    userId: uid,
    displayName: "Pragathi",
    targetExerciseMinutes: 60,
    targetMorningStudyMinutes: 60,
    targetEveningStudyMinutes: 60,
    targetSleepHours: 7.5,
    targetBedtimeMinutes: 1350,
    targetWakeTimeMinutes: 330,
    targetWaterGlasses: 8,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validDailyLog(uid, dateKey = "2026-11-15") {
  return {
    userId: uid,
    dateKey,
    exerciseMinutes: 60,
    morningStudyMinutes: 60,
    eveningStudyMinutes: 45,
    sleepHours: 7.5,
    bedtimeMinutes: 1350,
    wakeTimeMinutes: 330,
    sleepQuality: 4,
    screenFreeWindDown: true,
    waterGlasses: 7,
    mindfulMealsCount: 3,
    totalMealsCount: 3,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validFocusSession(uid) {
  return {
    userId: uid,
    dateKey: "2026-11-15",
    category: "MORNING_APPSC",
    topic: "Indian Polity - Fundamental Rights & DPSP",
    paperTag: "Paper III",
    durationMinutes: 60,
    notes: "Revised Articles 12-51 and APPSC previous mains questions",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validMealEntry(uid) {
  return {
    userId: uid,
    dateKey: "2026-11-15",
    mealType: "BREAKFAST",
    description: "Ragi malt, sprouts, and almonds",
    distractionFree: true,
    slowPace: true,
    satisfiedStop: true,
    nutritionQuality: "BALANCED",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validReminder(uid) {
  return {
    userId: uid,
    title: "Morning APPSC Group 1 Focus",
    message: "1 hour deep study on Paper III Polity",
    category: "MORNING_APPSC",
    hour: 7,
    minute: 0,
    enabled: true,
    daysOfWeek: [1, 2, 3, 4, 5, 6, 7],
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

test("Unauthenticated user: cannot read or write any user subcollection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("dailyLogs").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("sessions").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("meals").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("reminders").get());
});

test("Authenticated owner: can create, read, and update UserSettings", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(ref.set(validUserSettings(ALICE_UID)));
  await assertSucceeds(ref.get());
  await assertSucceeds(ref.update({ targetExerciseMinutes: 75, updatedAt: serverTimestamp() }));
});

test("Authenticated owner: can create and list DailyLogs, Sessions, Meals, and Reminders", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const userRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(userRef.collection("dailyLogs").doc("2026-11-15").set(validDailyLog(ALICE_UID)));
  await assertSucceeds(userRef.collection("dailyLogs").get());

  await assertSucceeds(userRef.collection("sessions").doc("sess_1").set(validFocusSession(ALICE_UID)));
  await assertSucceeds(userRef.collection("sessions").where("dateKey", "==", "2026-11-15").get());

  await assertSucceeds(userRef.collection("meals").doc("meal_1").set(validMealEntry(ALICE_UID)));
  await assertSucceeds(userRef.collection("meals").get());

  await assertSucceeds(userRef.collection("reminders").doc("rem_1").set(validReminder(ALICE_UID)));
  await assertSucceeds(userRef.collection("reminders").get());
});

test("Cross-user isolation: Bob cannot read, write, or delete Alice's records", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("sessions").doc("sess_alice").set(validFocusSession(ALICE_UID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("sessions").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("sessions").doc("sess_alice").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("sessions").doc("sess_alice").delete());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("reminders").doc("rem_spoof").set(validReminder(BOB_UID))
  );
});

test("Shadow field and schema poisoning: rejects ghost fields, invalid enum, and oversized topic", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const userRef = aliceDb.collection("users").doc(ALICE_UID);

  // Ghost field
  await assertFails(
    userRef.collection("sessions").doc("sess_ghost").set({
      ...validFocusSession(ALICE_UID),
      isAdmin: true,
    })
  );

  // Invalid category enum
  await assertFails(
    userRef.collection("sessions").doc("sess_bad_cat").set({
      ...validFocusSession(ALICE_UID),
      category: "GAMING",
    })
  );

  // Invalid dateKey doc ID
  await assertFails(
    userRef.collection("dailyLogs").doc("not-a-date").set(validDailyLog(ALICE_UID, "not-a-date"))
  );

  // Oversized topic (> 120 chars)
  await assertFails(
    userRef.collection("sessions").doc("sess_huge").set({
      ...validFocusSession(ALICE_UID),
      topic: "A".repeat(121),
    })
  );

  // Immutable createdAt tamper on update
  await assertSucceeds(userRef.collection("reminders").doc("rem_1").set(validReminder(ALICE_UID)));
  await assertFails(
    userRef.collection("reminders").doc("rem_1").update({
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
});
