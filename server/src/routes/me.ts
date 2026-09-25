import { Hono } from "hono";
import type { AppEnv, User } from "../env";
import { ApiError } from "../errors";
import { requireSession } from "../session";
import { jsonBody } from "../validate";

export const me = new Hono<AppEnv>();

me.use(requireSession);

function profile(user: User) {
  return { puuid: user.puuid, gameName: user.gameName, tagLine: user.tagLine, statsPublic: user.statsPublic };
}

me.get("/", (c) => c.json(profile(c.var.user)));

me.patch("/", async (c) => {
  const { statsPublic } = await jsonBody(c);
  if (typeof statsPublic !== "boolean") throw new ApiError(400, "invalid_stats_public");
  await c.env.DB.prepare("UPDATE users SET stats_public = ?, updated_at = ? WHERE id = ?")
    .bind(statsPublic ? 1 : 0, Date.now(), c.var.user.id)
    .run();
  return c.json(profile({ ...c.var.user, statsPublic }));
});

/** 연동 해제입니다. 사용자 한 줄을 지우면 세션, 친구, 요청, 초대가 스키마의 CASCADE로 같이 지워집니다. */
me.delete("/", async (c) => {
  await c.env.DB.prepare("DELETE FROM users WHERE id = ?").bind(c.var.user.id).run();
  return c.body(null, 204);
});
