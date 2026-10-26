// MongoDB initialization for opaque ExploreUBC login sessions.

const exploreUbcDb = db.getSiblingDB("cpen321");

const sessionValidator = {
  $jsonSchema: {
    bsonType: "object",
    title: "ExploreUBC session",
    additionalProperties: false,
    required: ["_id", "userId", "tokenHash", "createdAt", "expiresAt"],
    properties: {
      _id: {
        bsonType: "objectId",
        description: "MongoDB session identifier",
      },
      userId: {
        bsonType: "objectId",
        description: "ExploreUBC user authenticated by this session",
      },
      tokenHash: {
        bsonType: "string",
        description: "SHA-256 hash of the opaque session token",
      },
      createdAt: {
        bsonType: "date",
        description: "Session creation time",
      },
      expiresAt: {
        bsonType: "date",
        description: "Absolute session expiry time",
      },
    },
  },
};

if (!exploreUbcDb.getCollectionNames().includes("sessions")) {
  exploreUbcDb.createCollection("sessions", {
    validator: sessionValidator,
    validationLevel: "strict",
    validationAction: "error",
  });
} else {
  exploreUbcDb.runCommand({
    collMod: "sessions",
    validator: sessionValidator,
    validationLevel: "strict",
    validationAction: "error",
  });
}

exploreUbcDb.sessions.createIndex(
  { tokenHash: 1 },
  { unique: true, name: "unique_session_token_hash" },
);

exploreUbcDb.sessions.createIndex(
  { expiresAt: 1 },
  { expireAfterSeconds: 0, name: "expire_sessions" },
);

exploreUbcDb.sessions.createIndex(
  { userId: 1 },
  { name: "sessions_by_user" },
);
