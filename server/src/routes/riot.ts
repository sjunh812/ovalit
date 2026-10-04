import { Hono } from "hono";
import type { AppEnv } from "../env";
import { ApiError } from "../errors";
import { relationsTo } from "../relations";
import { rawJson, riotFor } from "../riot";
import { requireSession } from "../session";
import * as validate from "../validate";

export const riot = new Hono<AppEnv>();

riot.use(requireSession);

riot.get("/matchlist", async (c) => rawJson(c, await riotFor(c).matchlist()));

riot.get("/matches/:matchId", async (c) => {
  const match = await riotFor(c).matchWith(validate.matchId(c.req.param("matchId")), c.var.user.puuid);
  if (!match) throw new ApiError(403, "not_in_match");
  return rawJson(c, match.raw);
});

/**
 * 스코어보드에서 누구에게 친구 요청을 보낼 수 있는지 알려줍니다. 내가 뛴 경기의 다른 플레이어 중 앱에 연동한 사람과
 * 나와의 관계만 돌려줍니다. 앱을 쓰는지는 같이 뛴 사람에게만 알려줘서 내가 없는 경기는 403입니다.
 */
riot.get("/matches/:matchId/app-users", async (c) => {
  const players = await riotFor(c).participants(validate.matchId(c.req.param("matchId")));
  if (!players.has(c.var.user.puuid)) throw new ApiError(403, "not_in_match");
  players.delete(c.var.user.puuid);
  const related = await relationsTo(c.env.DB, c.var.user.id, [...players]);
  return c.json([...related].map(([puuid, { relation }]) => ({ puuid, relation })));
});
