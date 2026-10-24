// Explicit local test data loaded when Docker creates a fresh Mongo volume.

const exploreUbcDb = db.getSiblingDB("cpen321");
const testUserId = ObjectId("6acd519c76376cefc61e97f1");
const now = new Date();

exploreUbcDb.users.replaceOne(
  { _id: testUserId },
  {
    _id: testUserId,
    googleSubject: "dev-seed-user-google-subject",
    email: "test.user@student.ubc.ca",
    displayName: "Test User",
    profileImageUrl: null,
    start_location: "UBC Bus Exchange",
    preferences: {
      transportation: {
        mode: "transit",
      },
      budget: "balanced",
      interest: "outdoors",
    },
    onboardingCompletedAt: now,
    create_at: now,
    updated_at: now,
  },
  { upsert: true },
);

const testItineraryId = ObjectId("6705f1000000000000000001");

exploreUbcDb.itineraries.replaceOne(
  { _id: testItineraryId },
  {
    _id: testItineraryId,
    ownerId: testUserId,
    name: "UBC Afternoon",
    description: "A local test itinerary with an activity and transit segment.",
    startingLocation: "UBC Bus Exchange",
    items: [
      {
        id: "11111111-1111-4111-8111-111111111111",
        type: "activity",
        name: "Visit the Museum of Anthropology",
        image: "https://example.com/moa.jpg",
        description: "Explore the museum collections.",
        category: "art",
        location: "Museum of Anthropology at UBC",
        startTime: "2026-10-12T20:00:00.000Z",
        endTime: "2026-10-12T21:30:00.000Z",
      },
      {
        id: "22222222-2222-4222-8222-222222222222",
        type: "transportation",
        mode: "transit",
        startLocation: "Museum of Anthropology at UBC",
        endLocation: "UBC Bus Exchange",
        distanceMeters: 1800,
        startTime: "2026-10-12T21:35:00.000Z",
        endTime: "2026-10-12T21:50:00.000Z",
      },
    ],
    createdAt: now,
    updatedAt: now,
    saved: true,
    inRecommendation: false,
    score: {
      overall: 0.9,
      preferenceMatch: 0.85,
    },
  },
  { upsert: true },
);
