import { Hono } from "hono";
import { INVITE_CODE, inviteCode } from "../crypto";
import type { AppEnv } from "../env";
import { ApiError } from "../errors";
import { relationsTo, sendRequest } from "../relations";
import { requireSession } from "../session";

const DAY_MS = 24 * 60 * 60 * 1000;
const INVITE_TTL_MS = 7 * DAY_MS;
// 하루도 안 남은 링크를 단톡방에 올리면 받은 사람이 열기 전에 끝날 수 있어 새로 만든다.
const REUSE_MIN_REMAINING_MS = DAY_MS;
// 크론을 두지 않아서 누가 링크를 만들든 만료된 초대를 같이 치운다. 쌓인 게 많아도 요청 하나가 지우는 줄은 50개로 묶는다.
const CLEANUP_LIMIT = 50;

export const invites = new Hono<AppEnv>();

invites.use(requireSession);

/**
 * 단톡방에 한 번 올린 링크를 여럿이 누를 수 있게 만료 전까지 몇 번이든 씁니다. 기한이 하루 넘게 남은 링크가 있으면
 * 새로 만들지 않고 그 링크를 200으로 돌려주고, 새로 만들면 201입니다.
 */
invites.post("/", async (c) => {
  const db = c.env.DB;
  const origin = new URL(c.req.url).origin;
  const now = Date.now();
  const live = await db
    .prepare("SELECT code, expires_at FROM invites WHERE user_id = ? AND expires_at > ? ORDER BY expires_at DESC LIMIT 1")
    .bind(c.var.user.id, now + REUSE_MIN_REMAINING_MS)
    .first<{ code: string; expires_at: number }>();
  if (live) return c.json({ code: live.code, url: `${origin}/i/${live.code}`, expiresAt: live.expires_at });

  const code = inviteCode();
  const expiresAt = now + INVITE_TTL_MS;
  await db.batch([
    db
      .prepare("DELETE FROM invites WHERE rowid IN (SELECT rowid FROM invites WHERE expires_at <= ? LIMIT ?)")
      .bind(now, CLEANUP_LIMIT),
    db
      .prepare("INSERT INTO invites (code, user_id, created_at, expires_at) VALUES (?, ?, ?, ?)")
      .bind(code, c.var.user.id, now, expiresAt),
  ]);
  return c.json({ code, url: `${origin}/i/${code}`, expiresAt }, 201);
});

/** 링크를 받은 사람이 링크를 만든 사람에게 친구 요청을 보냅니다. 만든 사람이 수락해야 친구가 됩니다. */
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
  // 단톡방에 올린 링크는 이미 친구인 사람도 누른다. 몇 번을 눌러도 에러 없이 같은 결과가 나오게 200으로 답한다.
  // 스코어보드 요청은 친구가 아닌 사람에게만 버튼이 뜨니 거기서는 409로 앱의 관계가 낡았다고 알린다.
  if (related?.relation === "friend") return c.json({ status: "already_friends" });
  return sendRequest(c, invite.user_id, related?.relation ?? "app_user", "invite_link");
});
