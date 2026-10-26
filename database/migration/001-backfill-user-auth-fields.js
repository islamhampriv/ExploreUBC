// Backfill authentication fields added to existing ExploreUBC users.
// Run this script explicitly; Docker does not run migration scripts automatically.

const migrationDb = db.getSiblingDB("cpen321");
const userCollectionExists = migrationDb.getCollectionNames().includes("users");

if (!userCollectionExists) {
  throw new Error(
    "The users collection does not exist. Use database/init/001-users.js for a fresh database.",
  );
}

// The legacy strict validator does not recognize the new fields. Permit the
// backfill temporarily, then load the current initializer to restore the final
// strict validator and index definition.
migrationDb.runCommand({
  collMod: "users",
  validationLevel: "strict",
  validationAction: "warn",
});

try {
  const profileImages = migrationDb.users.updateMany(
    { profileImageUrl: { $exists: false } },
    { $set: { profileImageUrl: null } },
  );
  const onboarding = migrationDb.users.updateMany(
    { onboardingCompletedAt: { $exists: false } },
    [{ $set: { onboardingCompletedAt: "$updated_at" } }],
  );

  print(
    `Migrated users: ${profileImages.modifiedCount} profile images, ` +
      `${onboarding.modifiedCount} onboarding timestamps.`,
  );
} finally {
  load("/docker-entrypoint-initdb.d/001-users.js");
}
