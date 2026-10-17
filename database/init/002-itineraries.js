// MongoDB initialization for the ExploreUBC itineraries collection.
// API IDs and top-level timestamps are converted by ItineraryDao to ObjectId
// and Date values. Embedded item IDs and timestamps remain strings.

const exploreUbcDb = db.getSiblingDB("cpen321");

const transportationModes = [
  "walking",
  "cycling",
  "transit",
  "driving",
  "other",
];

const numberTypes = ["double", "int", "long", "decimal"];

const activityItemSchema = {
  bsonType: "object",
  additionalProperties: false,
  required: [
    "id",
    "type",
    "name",
    "image",
    "location",
    "startTime",
    "endTime",
  ],
  properties: {
    id: { bsonType: "string" },
    type: { enum: ["activity"] },
    name: { bsonType: "string" },
    image: { bsonType: "string" },
    description: { bsonType: "string" },
    category: { bsonType: "string" },
    location: { bsonType: "string" },
    startTime: { bsonType: "string" },
    endTime: { bsonType: "string" },
  },
};

const transportationItemSchema = {
  bsonType: "object",
  additionalProperties: false,
  required: [
    "id",
    "type",
    "mode",
    "startLocation",
    "endLocation",
    "distanceMeters",
    "startTime",
    "endTime",
  ],
  properties: {
    id: { bsonType: "string" },
    type: { enum: ["transportation"] },
    mode: { enum: transportationModes },
    startLocation: { bsonType: "string" },
    endLocation: { bsonType: "string" },
    distanceMeters: { bsonType: numberTypes, minimum: 0 },
    startTime: { bsonType: "string" },
    endTime: { bsonType: "string" },
  },
};

const itineraryValidator = {
  $jsonSchema: {
    bsonType: "object",
    title: "ExploreUBC itinerary",
    additionalProperties: false,
    required: [
      "_id",
      "ownerId",
      "name",
      "items",
      "createdAt",
      "updatedAt",
      "saved",
      "inRecommendation",
      "score",
    ],
    properties: {
      _id: { bsonType: "objectId" },
      ownerId: { bsonType: "objectId" },
      name: { bsonType: "string" },
      description: { bsonType: "string" },
      startingLocation: { bsonType: "string" },
      items: {
        bsonType: "array",
        items: {
          oneOf: [activityItemSchema, transportationItemSchema],
        },
      },
      createdAt: { bsonType: "date" },
      updatedAt: { bsonType: "date" },
      saved: { bsonType: "bool" },
      inRecommendation: { bsonType: "bool" },
      score: {
        bsonType: "object",
        additionalProperties: { bsonType: numberTypes },
      },
    },
  },
};

if (!exploreUbcDb.getCollectionNames().includes("itineraries")) {
  exploreUbcDb.createCollection("itineraries", {
    validator: itineraryValidator,
    validationLevel: "strict",
    validationAction: "error",
  });
} else {
  exploreUbcDb.runCommand({
    collMod: "itineraries",
    validator: itineraryValidator,
    validationLevel: "strict",
    validationAction: "error",
  });
}

exploreUbcDb.itineraries.createIndex(
  { ownerId: 1, updatedAt: -1 },
  { name: "owner_updated_at" },
);
