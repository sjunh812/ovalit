import { ApiError } from "./errors";

/**
 * 요약을 두는 주 수입니다.
 * 앱은 8주 안의 경기만 보니 경기 참가자처럼 10주가 지나면 크론이 지웁니다.
 */
export const SUMMARY_KEEP_WEEKS = 10;

// 경쟁전과 일반전만 친구 비교와 라이벌 대결에 넣는다(CLAUDE.md 큐)
const QUEUES = ["competitive", "unrated"] as const;
// 한 주가 액트 경계에 걸쳐도 액트 둘, 큐 둘이라 넷이면 된다
const MAX_ROWS = 4;
const WEEK = /^\d{4}-\d{2}-\d{2}$/;
const ACT = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
const DAY_MS = 24 * 60 * 60 * 1000;

// 한 주에 나올 수 없는 값을 거른다.
// 넘치면 앱이 잘못 센 것이라 저장하지 않고 400이다.
const LIMITS = {
  matches: 300,
  rounds: 12_000,
  kills: 30_000,
  deaths: 30_000,
  assists: 30_000,
  score: 10_000_000,
  damage: 5_000_000,
  head: 200_000,
  body: 200_000,
  leg: 200_000,
} as const;

export type SummaryCounts = Record<keyof typeof LIMITS, number>;

export interface SummaryRow extends SummaryCounts {
  act: string;
  queue: (typeof QUEUES)[number];
}

export const SUMMARY_COLUMNS = Object.keys(LIMITS) as (keyof typeof LIMITS)[];

/** 같은 주에 같은 값을 다시 올렸는지입니다. 같으면 D1에 쓰지 않습니다. */
export function sameRows(stored: SummaryRow[], incoming: SummaryRow[]): boolean {
  const key = (row: SummaryRow) => [row.act, row.queue, ...SUMMARY_COLUMNS.map((column) => row[column])].join(":");
  const left = new Set(stored.map(key));
  return stored.length === incoming.length && incoming.every((row) => left.has(key(row)));
}

/**
 * 그 주 월요일입니다.
 * 기기 시간대로 자른 주라 서버의 UTC 날짜보다 하루 앞설 수 있어 하루까지 받습니다.
 * [SUMMARY_KEEP_WEEKS]보다 오래된 주는 크론이 지울 주라 받지 않습니다.
 */
export function week(value: string | undefined, now = Date.now()): string {
  if (value === undefined || !WEEK.test(value)) throw new ApiError(400, "invalid_week");
  const day = new Date(`${value}T00:00:00Z`);
  if (Number.isNaN(day.getTime()) || day.toISOString().slice(0, 10) !== value || day.getUTCDay() !== 1) {
    throw new ApiError(400, "invalid_week");
  }
  if (day.getTime() > now + DAY_MS || day.getTime() < oldestWeekStart(now)) throw new ApiError(400, "invalid_week");
  return value;
}

/** 지금 둘 수 있는 가장 오래된 주의 시작 시각입니다. 크론도 이보다 앞선 주를 지웁니다. */
export function oldestWeekStart(now = Date.now()): number {
  return now - SUMMARY_KEEP_WEEKS * 7 * DAY_MS;
}

/** 크론과 받기가 쓰는 날짜 문자열입니다. 사전순이 날짜순이라 문자열로 견줍니다. */
export function weekCutoff(now = Date.now()): string {
  return new Date(oldestWeekStart(now)).toISOString().slice(0, 10);
}

/**
 * 올린 요약 줄입니다.
 * 비율이나 평균은 받지 않습니다.
 * 합계만 받아야 친구 앱이 기간을 넓힐 때 더한 뒤 나눌 수 있습니다(평균의 평균 금지).
 */
export function summaryRows(value: unknown): SummaryRow[] {
  if (!Array.isArray(value) || value.length > MAX_ROWS) throw new ApiError(400, "invalid_summary");
  const rows = value.map(summaryRow);
  const keys = new Set(rows.map((row) => `${row.act}:${row.queue}`));
  if (keys.size !== rows.length) throw new ApiError(400, "invalid_summary");
  return rows;
}

function summaryRow(value: unknown): SummaryRow {
  if (typeof value !== "object" || value === null || Array.isArray(value)) throw new ApiError(400, "invalid_summary");
  const row = value as Record<string, unknown>;
  const allowed = new Set<string>(["act", "queue", ...SUMMARY_COLUMNS]);
  if (Object.keys(row).some((key) => !allowed.has(key))) throw new ApiError(400, "invalid_summary");
  if (typeof row.act !== "string" || !ACT.test(row.act)) throw new ApiError(400, "invalid_summary");
  const queue = QUEUES.find((it) => it === row.queue);
  if (queue === undefined) throw new ApiError(400, "invalid_summary");
  const counts = {} as SummaryCounts;
  for (const column of SUMMARY_COLUMNS) {
    const count = row[column];
    if (typeof count !== "number" || !Number.isInteger(count) || count < 0 || count > LIMITS[column]) {
      throw new ApiError(400, "invalid_summary");
    }
    counts[column] = count;
  }
  return { act: row.act, queue, ...counts };
}
