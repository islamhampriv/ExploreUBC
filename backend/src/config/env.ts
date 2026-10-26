import "dotenv/config";

const rawPort = process.env.PORT;
const port =
  rawPort === undefined || rawPort === "" ? 3000 : Number.parseInt(rawPort, 10);

if (Number.isNaN(port) || port < 1 || port > 65535) {
  throw new Error(`Invalid PORT: ${rawPort}`);
}

const mongodbUri = process.env.MONGODB_URI?.trim();
if (mongodbUri === undefined || mongodbUri === "") {
  throw new Error("MONGODB_URI is required.");
}

const googleClientId = process.env.GOOGLE_CLIENT_ID?.trim();
if (googleClientId === undefined || googleClientId === "") {
  throw new Error("GOOGLE_CLIENT_ID is required.");
}

const rawSessionTtlDays = process.env.SESSION_TTL_DAYS?.trim();
if (
  rawSessionTtlDays !== undefined &&
  rawSessionTtlDays !== "" &&
  !/^\d+$/u.test(rawSessionTtlDays)
) {
  throw new Error(`Invalid SESSION_TTL_DAYS: ${rawSessionTtlDays}`);
}

const sessionTtlDays =
  rawSessionTtlDays === undefined || rawSessionTtlDays === ""
    ? 1
    : Number(rawSessionTtlDays);

if (
  !Number.isInteger(sessionTtlDays) ||
  sessionTtlDays < 1 ||
  sessionTtlDays > 365
) {
  throw new Error(`Invalid SESSION_TTL_DAYS: ${rawSessionTtlDays}`);
}

const rawEventSearchPageSize = process.env.EVENT_SEARCH_PAGE_SIZE;
const eventSearchPageSize =
  rawEventSearchPageSize === undefined || rawEventSearchPageSize === ""
    ? 200
    : Number.parseInt(rawEventSearchPageSize, 10);

if (
  Number.isNaN(eventSearchPageSize) ||
  eventSearchPageSize < 1 ||
  eventSearchPageSize > 200
) {
  throw new Error(`Invalid EVENT_SEARCH_PAGE_SIZE: ${rawEventSearchPageSize}`);
}

export const env = {
  port,
  mongodbUri,
  googleClientId,
  sessionTtlDays,
  ticketmaster: {
    apiKey: process.env.TICKETMASTER_API_KEY,
    baseUrl:
      process.env.TICKETMASTER_BASE_URL ??
      "https://app.ticketmaster.com/discovery/v2",
    city: process.env.EVENT_SEARCH_CITY ?? "Vancouver",
    stateCode: process.env.EVENT_SEARCH_STATE_CODE ?? "BC",
    countryCode: process.env.EVENT_SEARCH_COUNTRY_CODE ?? "CA",
    pageSize: eventSearchPageSize,
  },
} as const;
