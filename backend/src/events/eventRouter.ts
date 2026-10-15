// Declares the backend HTTP routes exposed by the events feature.

import { Router } from 'express';

import type { EventService } from './eventService';

export function createEventRouter(eventService: EventService): Router {
  const router = Router();

  router.get('/', async (request, response) => {
    const categories =
      typeof request.query.categories === 'string'
        ? request.query.categories
            .split(',')
            .map((category) => category.trim())
            .filter((category) => category !== '')
        : [];
    const latitude = Number(request.query.latitude);
    const longitude = Number(request.query.longitude);
    const maxDistanceKm = Number(request.query.maxDistanceKm ?? 30);
    const page = Number(request.query.page ?? 1);
    const limit = Number(request.query.limit ?? 20);

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
      response.status(400).json({ error: 'Invalid event query parameters' });
      return;
    }

    try {
      const events = await eventService.getRecommendedEvents({
        preferredCategories: categories,
        longitude,
        latitude,
        maxDistanceKm,
        page,
        limit,
      });

      response.json({
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
      console.error('Failed to retrieve events:', error);
      response.status(500).json({ error: 'Failed to retrieve events' });
    }
  });

  return router;
}
