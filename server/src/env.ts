export interface Env {
  DB: D1Database;
  RIOT_API_KEY?: string;
  RSO_CLIENT_ID?: string;
  RSO_CLIENT_SECRET?: string;
  /** `"true"`일 때만 `/auth/dev`가 열립니다. 배포 환경에는 절대 넣지 않습니다. */
  DEV_LOGIN?: string;
}

export interface User {
  id: number;
  puuid: string;
  gameName: string;
  tagLine: string;
  statsPublic: boolean;
}

export interface AppEnv {
  Bindings: Env;
  Variables: {
    /** Riot으로 나가는 요청입니다. 테스트는 가짜를 넣어 실제 Riot을 부르지 않습니다. */
    upstream: typeof fetch;
    user: User;
    sessionHash: string;
  };
}
