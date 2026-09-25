import { Hono } from "hono";
import type { AppEnv } from "../env";
import { ApiError } from "../errors";
import { relationsTo } from "../relations";
import { playersOf, rawJson, riotFor } from "../riot";
import { requireSession } from "../session";
import * as validate from "../validate";

export const riot = new Hono<AppEnv>();

riot.use(requireSession);

riot.get("/matchlist", async (c) => rawJson(c, await riotFor(c).matchlist(c.var.user.puuid)));

riot.get("/matches/:matchId", async (c) => {
  const match = await riotFor(c).match(validate.matchId(c.req.param("matchId")));
  if (!playersOf(match.data).has(c.var.user.puuid)) throw new ApiError(403, "not_in_match");
  return rawJson(c, match.raw);
});

/**
 * 스코어보드에서 누구에게 친구 요청을 보낼 수 있는지 알려줍니다. 내가 뛴 경기의 다른 플레이어 중 앱에
 * 연동한 사람만 돌려주고, 연동하지 않은 사람은 목록에서 뺍니다.
 */
riot.get("/matches/:matchId/app-users", async (c) => {
  const match = await riotFor(c).match(validate.matchId(c.req.param("matchId")));
  const players = playersOf(match.data);
  if (!players.has(c.var.user.puuid)) throw new ApiError(403, "not_in_match");
  players.delete(c.var.user.puuid);
  const related = await relationsTo(c.env.DB, c.var.user.id, [...players]);
  return c.json([...related].map(([puuid, { relation }]) => ({ puuid, relation })));
});
