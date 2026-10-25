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

const rawSoclaasMaxOutputTokens = process.env.SOCLAAS_MAX_OUTPUT_TOKENS;
const soclaasMaxOutputTokens =
  rawSoclaasMaxOutputTokens === undefined || rawSoclaasMaxOutputTokens === ""
    ? 2_000
    : Number.parseInt(rawSoclaasMaxOutputTokens, 10);

if (
  !Number.isInteger(soclaasMaxOutputTokens) ||
  soclaasMaxOutputTokens < 1 ||
  soclaasMaxOutputTokens > 16_384
) {
  throw new Error(
    `Invalid SOCLAAS_MAX_OUTPUT_TOKENS: ${rawSoclaasMaxOutputTokens}`,
  );
}

export const env = {
  port,
  mongodbUri,
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
  soclaas: {
    endpoint: process.env.SOCLAAS_ENDPOINT?.trim(),
    apiKey: process.env.SOCLAAS_API_KEY?.trim(),
    model: process.env.SOCLAAS_MODEL?.trim(),
    maxOutputTokens: soclaasMaxOutputTokens,
  },
} as const;
