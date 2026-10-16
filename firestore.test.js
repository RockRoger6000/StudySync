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

test("Unauthenticated user: cannot read tasks", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("tasks").get());
});

test("Unauthenticated user: cannot read schedules", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("schedules").get());
});

test("Authenticated user: cannot read another user's task", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("tasks").doc("bob_task_1").set({
      userId: BOB_UID,
      title: "Bob Assignment",
      subject: "Math",
      dueDate: "2026-10-20",
      status: "PENDING",
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("tasks").doc("bob_task_1").get());
});

test("Authenticated user: can query their own tasks", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("tasks").doc("alice_task_1").set({
      userId: ALICE_UID,
      title: "Alice Assignment",
      subject: "Physics",
      dueDate: "2026-10-20",
      status: "PENDING",
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("tasks").where("userId", "==", ALICE_UID).get()
  );
});

test("Authenticated user: can create their own schedule block", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("schedules").doc("sched_1").set({
      userId: ALICE_UID,
      title: "Kalkulus II",
      category: "STUDY",
      dayOfWeek: 1,
      startTime: "08:00",
      endTime: "10:00",
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: cannot create schedule block for another user", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("schedules").doc("sched_2").set({
      userId: BOB_UID,
      title: "Spoofed Block",
      category: "STUDY",
      dayOfWeek: 1,
      startTime: "08:00",
      endTime: "10:00",
      createdAt: new Date(),
    })
  );
});
