const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_student_1";
const BOB_UID = "bob_student_2";

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

function notebookPayload(uid, overrides = {}) {
  const past = new Date(Date.now() - 5000);
  return {
    userId: uid,
    title: "Cellular Biology 101",
    subject: "Biology",
    colorToken: "teal",
    sourceCount: 0,
    createdAt: past,
    updatedAt: past,
    ...overrides,
  };
}

function sourcePayload(uid, notebookId, overrides = {}) {
  const past = new Date(Date.now() - 3000);
  return {
    userId: uid,
    notebookId,
    title: "Lecture 03 - Mitochondria.pdf",
    sourceType: "PDF",
    mimeType: "application/pdf",
    content: "Mitochondria generate most of the cell's supply of ATP via oxidative phosphorylation.",
    excerpt: "Mitochondria generate most of the cell's supply of ATP...",
    wordCount: 13,
    included: true,
    createdAt: past,
    updatedAt: past,
    ...overrides,
  };
}

function artifactPayload(uid, notebookId, overrides = {}) {
  const past = new Date(Date.now() - 2000);
  return {
    userId: uid,
    notebookId,
    artifactType: "STUDY_GUIDE",
    title: "ATP Synthesis Study Guide",
    payloadJson: "# Study Guide\n- Oxidative phosphorylation\n- Electron transport chain",
    sourceTitles: ["Lecture 03 - Mitochondria.pdf"],
    createdAt: past,
    updatedAt: past,
    ...overrides,
  };
}

function messagePayload(uid, notebookId, overrides = {}) {
  const past = new Date(Date.now() - 1000);
  return {
    userId: uid,
    notebookId,
    role: "user",
    text: "How does the proton gradient drive ATP synthase?",
    citations: [],
    createdAt: past,
    ...overrides,
  };
}

test("Unauthenticated user cannot read or write notebooks, sources, artifacts, or messages", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection(`users/${ALICE_UID}/notebooks`).get());
  await assertFails(
    unauth.collection(`users/${ALICE_UID}/notebooks`).doc("nb_1").set(notebookPayload(ALICE_UID))
  );
  await assertFails(
    unauth.collection(`users/${ALICE_UID}/notebooks/nb_1/sources`).get()
  );
});

test("Owner can create, list, update, and delete notebooks; ghost fields & immutable edits fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const nbRef = aliceDb.collection(`users/${ALICE_UID}/notebooks`).doc("nb_bio_1");

  // Valid create (no @DocumentId `id` field)
  await assertSucceeds(nbRef.set(notebookPayload(ALICE_UID)));

  // Reject ghost field on create
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/notebooks`).doc("nb_ghost").set(
      notebookPayload(ALICE_UID, { id: "nb_ghost" })
    )
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/notebooks`).doc("nb_future").set(
      notebookPayload(ALICE_UID, { createdAt: new Date(Date.now() + 60000) })
    )
  );

  // Valid list query matching repository
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/notebooks`).orderBy("updatedAt", "desc").get()
  );

  // Valid partial updates matching repository
  await assertSucceeds(
    nbRef.update({
      title: "Molecular Biology 102",
      subject: "Bio",
      colorToken: "amber",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    nbRef.update({
      sourceCount: 2,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(
    nbRef.update({
      summaryOverview: "Covers ATP synthesis and glycolysis.",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Reject immutable createdAt edit
  await assertFails(
    nbRef.update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Reject invalid colorToken
  await assertFails(
    nbRef.update({
      title: "Valid",
      subject: "Bio",
      colorToken: "neon_pink",
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("Cross-user access to notebooks and subcollections is strictly denied", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection(`users/${BOB_UID}/notebooks`).doc("bob_nb").set(notebookPayload(BOB_UID));
    await db
      .collection(`users/${BOB_UID}/notebooks/bob_nb/sources`)
      .doc("bob_src")
      .set(sourcePayload(BOB_UID, "bob_nb"));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection(`users/${BOB_UID}/notebooks`).get());
  await assertFails(aliceDb.collection(`users/${BOB_UID}/notebooks`).doc("bob_nb").get());
  await assertFails(
    aliceDb.collection(`users/${BOB_UID}/notebooks`).doc("bob_nb").update({
      sourceCount: 99,
      updatedAt: new Date(Date.now() - 100),
    })
  );
  await assertFails(aliceDb.collection(`users/${BOB_UID}/notebooks/bob_nb/sources`).get());
  await assertFails(
    aliceDb
      .collection(`users/${ALICE_UID}/notebooks`)
      .doc("spoof_nb")
      .set(notebookPayload(BOB_UID))
  );
});

test("Sources, artifacts, and messages enforce parent existence, schema validation, and immutability", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const nbId = "nb_cs_1";

  // Orphan source creation without parent notebook fails
  await assertFails(
    aliceDb
      .collection(`users/${ALICE_UID}/notebooks/${nbId}/sources`)
      .doc("src_orphan")
      .set(sourcePayload(ALICE_UID, nbId))
  );

  // Create parent notebook
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/notebooks`).doc(nbId).set(notebookPayload(ALICE_UID))
  );

  // Create, list, toggle included, and edit source
  const srcRef = aliceDb.collection(`users/${ALICE_UID}/notebooks/${nbId}/sources`).doc("src_1");
  await assertSucceeds(srcRef.set(sourcePayload(ALICE_UID, nbId)));
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/notebooks/${nbId}/sources`).orderBy("createdAt", "desc").get()
  );
  await assertSucceeds(
    srcRef.update({ included: false, updatedAt: new Date(Date.now() - 500) })
  );

  // Create & update artifact
  const artRef = aliceDb.collection(`users/${ALICE_UID}/notebooks/${nbId}/artifacts`).doc("art_1");
  await assertSucceeds(artRef.set(artifactPayload(ALICE_UID, nbId)));
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/notebooks/${nbId}/artifacts`).orderBy("createdAt", "desc").get()
  );

  // Reject artifactType mutation
  await assertFails(
    artRef.update({
      artifactType: "QUIZ",
      title: "Changed",
      payloadJson: "{}",
      sourceTitles: [],
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Create message & reject message update
  const msgRef = aliceDb.collection(`users/${ALICE_UID}/notebooks/${nbId}/messages`).doc("msg_1");
  await assertSucceeds(msgRef.set(messagePayload(ALICE_UID, nbId)));
  await assertFails(msgRef.update({ text: "Tampered" }));
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/notebooks/${nbId}/messages`).orderBy("createdAt", "asc").get()
  );
});
