// Handles event-related HTTP requests and constructs HTTP responses.

import type { Request, Response } from "express";

import { getRecommendedEvents } from "./event.service";

/**
 * Returns events recommended for the supplied preferences and location.
 * @param req Request containing recommendation query parameters.
 * @param res Response containing recommended events.
 */
export async function getEvents(req: Request, res: Response): Promise<void> {
  const categories =
    typeof req.query.categories === "string"
      ? req.query.categories
          .split(",")
          .map((category) => category.trim())
          .filter((category) => category !== "")
      : [];
  const latitude = Number(req.query.latitude);
  const longitude = Number(req.query.longitude);
  const maxDistanceKm = Number(req.query.maxDistanceKm ?? 30);
  const page = Number(req.query.page ?? 1);
  const limit = Number(req.query.limit ?? 20);

  if (
    !Number.isFinite(latitude) ||
    latitude < -90 ||
    latitude > 90 ||
    !Number.isFinite(longitude) ||
    longitude < -180 ||
    longitude > 180 ||
    !Number.isFinite(maxDistanceKm) ||
    maxDistanceKm <= 0 ||
    !Number.isInteger(page) ||
    page < 1 ||
    !Number.isInteger(limit) ||
    limit < 1 ||
    limit > 100
  ) {
    res.status(400).json({ error: "Invalid event query parameters" });
    return;
  }

  try {
    const events = await getRecommendedEvents({
      preferredCategories: categories,
      longitude,
      latitude,
      maxDistanceKm,
      page,
      limit,
    });

    res.json({
      page,
      limit,
      count: events.length,
      events: events.map((event) => ({
        id: event.externalId,
        title: event.title,
        imageUrl: event.imageUrl,
        startDateTime: event.startDateTime,
        venueName: event.venueName,
        city: event.city,
        category: event.category,
        ticketUrl: event.ticketUrl,
        priceMin: event.priceMin,
        priceMax: event.priceMax,
        currency: event.currency,
        distanceKm: event.distanceKm,
        recommendationScore: event.recommendationScore,
      })),
    });
  } catch (error: unknown) {
    console.error("Failed to retrieve events:", error);
    res.status(500).json({ error: "Failed to retrieve events" });
  }
}
