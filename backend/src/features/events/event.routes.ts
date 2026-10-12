// Declares the backend HTTP routes exposed by the events feature.

import { Router } from "express";

import { getEvents } from "./event.controller";

export const eventRouter = Router();

eventRouter.get("/", getEvents);
