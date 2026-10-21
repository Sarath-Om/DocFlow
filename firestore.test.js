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

// --- SECURITY BOUNDS TESTS ---

test("Unauthenticated user: cannot read documents", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("documents").get());
});

test("Unauthenticated user: cannot create documents", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("documents").doc("doc1").set({
      userId: ALICE_UID,
      title: "Test",
      folderId: "root",
      category: "Work",
      fileType: "pdf",
      fileName: "test.pdf",
      fileSize: 1024,
      content: "Hello",
      isEncrypted: false,
      isOfflineCached: true,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: can create and read their own document and folder", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Create folder
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("folders").doc("f1").set({
      userId: ALICE_UID,
      name: "Invoices",
      color: "#4285F4",
      icon: "folder",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  // Read folder
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("folders").doc("f1").get()
  );

  // Create document
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("documents").doc("doc1").set({
      userId: ALICE_UID,
      title: "Invoice #101",
      folderId: "f1",
      category: "Invoices",
      fileType: "pdf",
      fileName: "inv101.pdf",
      fileSize: 2048,
      content: "Amount: $500",
      isEncrypted: false,
      isOfflineCached: true,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  // Read document
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("documents").doc("doc1").get()
  );
});

test("Cross-user isolation: Alice cannot access Bob's documents", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("documents").doc("bob_doc").set({
      userId: BOB_UID,
      title: "Secret",
      folderId: "root",
      category: "Personal",
      fileType: "pdf",
      fileName: "secret.pdf",
      fileSize: 1024,
      content: "Secret details",
      isEncrypted: true,
      isOfflineCached: false,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("documents").doc("bob_doc").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("documents").get()
  );
});
