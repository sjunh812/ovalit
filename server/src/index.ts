import { createApp } from "./app";
import type { Env } from "./env";
import { createScheduled } from "./scheduled";

const app = createApp();

export default {
  fetch: app.fetch,
  scheduled: createScheduled(),
} satisfies ExportedHandler<Env>;
