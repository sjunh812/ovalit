import { Hono } from "hono";
import { INVITE_CODE, inviteCode } from "../crypto";
import type { AppEnv } from "../env";
import { ApiError } from "../errors";
import { relationsTo } from "../relations";
import { requireSession } from "../session";
import { sendRequest } from "./friends";

const INVITE_TTL_MS = 7 * 24 * 60 * 60 * 1000;

export const invites = new Hono<AppEnv>();

invites.use(requireSession);

/** 단톡방에 한 번 올린 링크를 여럿이 누를 수 있게 만료 전까지 몇 번이든 씁니다. */
invites.post("/", async (c) => {
  const code = inviteCode();
  const now = Date.now();
  const expiresAt = now + INVITE_TTL_MS;
  await c.env.DB.batch([
    c.env.DB.prepare("DELETE FROM invites WHERE user_id = ? AND expires_at <= ?").bind(c.var.user.id, now),
    c.env.DB.prepare("INSERT INTO invites (code, user_id, created_at, expires_at) VALUES (?, ?, ?, ?)").bind(
      code,
      c.var.user.id,
      now,
      expiresAt,
    ),
  ]);
  return c.json({ code, url: `${new URL(c.req.url).origin}/i/${code}`, expiresAt }, 201);
});

/** 링크를 받은 사람이 연 사람에게 친구 요청을 보냅니다. 초대한 사람이 수락해야 친구가 됩니다. */
invites.post("/:code/redeem", async (c) => {
  const code = c.req.param("code").toUpperCase();
  if (!INVITE_CODE.test(code)) throw new ApiError(404, "invite_not_found");
  const invite = await c.env.DB.prepare(
    "SELECT i.user_id, i.expires_at, u.puuid FROM invites i JOIN users u ON u.id = i.user_id WHERE i.code = ?",
  )
    .bind(code)
    .first<{ user_id: number; expires_at: number; puuid: string }>();
  if (!invite) throw new ApiError(404, "invite_not_found");
  if (invite.user_id === c.var.user.id) throw new ApiError(400, "own_invite");
  if (invite.expires_at <= Date.now()) throw new ApiError(410, "invite_expired");

  const related = (await relationsTo(c.env.DB, c.var.user.id, [invite.puuid])).get(invite.puuid);
  if (related?.relation === "friend") return c.json({ status: "already_friends" });
  return sendRequest(c, invite.user_id, related?.relation ?? "app_user", "invite_link");
});
