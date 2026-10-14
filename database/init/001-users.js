// MongoDB initialization for the ExploreUBC users collection.
// This file intentionally creates no user documents.

const exploreUbcDb = db.getSiblingDB("cpen321");

const userValidator = {
  $jsonSchema: {
    bsonType: "object",
    title: "ExploreUBC user",
    additionalProperties: false,
    required: [
      "googleSubject",
      "email",
      "displayName",
      "start_location",
      "preferences",
      "create_at",
      "updated_at",
    ],
    properties: {
      _id: {
        bsonType: "objectId",
        description: "MongoDB user identifier",
      },
      googleSubject: {
        bsonType: "string",
        description: "Stable Google sub claim; must be a string",
      },
      email: {
        bsonType: "string",
        description: "User email; must be a string",
      },
      displayName: {
        bsonType: "string",
        description: "User display name; must be a string",
      },
      start_location: {
        bsonType: "string",
        description: "User's default starting location; must be a string",
      },
      preferences: {
        bsonType: "object",
        additionalProperties: false,
        required: ["transportation", "budget", "interest"],
        properties: {
          transportation: {
            bsonType: "object",
            additionalProperties: false,
            required: ["mode"],
            properties: {
              mode: {
                enum: ["walking", "cycling", "transit", "driving", "other"],
              },
            },
          },
          budget: {
            enum: ["economical", "balanced", "premium"],
          },
          interest: {
            enum: [
              "food",
              "art",
              "outdoors",
              "markets",
              "live_music",
              "gaming",
            ],
          },
        },
      },
      create_at: { bsonType: "date" },
      updated_at: { bsonType: "date" },
    },
  },
};

if (!exploreUbcDb.getCollectionNames().includes("users")) {
  exploreUbcDb.createCollection("users", {
    validator: userValidator,
    validationLevel: "strict",
    validationAction: "error",
  });
} else {
  exploreUbcDb.runCommand({
    collMod: "users",
    validator: userValidator,
    validationLevel: "strict",
    validationAction: "error",
  });
}

exploreUbcDb.users.createIndex(
  { googleSubject: 1 },
  { unique: true, name: "unique_google_subject" },
);
