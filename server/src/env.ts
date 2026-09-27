export interface Env {
  DB: D1Database;
  RIOT_API_KEY?: string;
  RSO_CLIENT_ID?: string;
  RSO_CLIENT_SECRET?: string;
  /**
   * `"true"`이고 로컬 주소(localhost, 127.0.0.1, [::1], 10.0.2.2)로 들어온 요청에만 `/auth/dev`가 열립니다. 주소는
   * Host 헤더로 가려서 꾸밀 수 있으니 배포 환경에는 절대 넣지 않습니다.
   */
  DEV_LOGIN?: string;
  /**
   * 앱 서명 인증서의 SHA-256 지문입니다. 여럿이면 쉼표로 잇습니다(디버그 키, Play 앱 서명 키). 안드로이드가 이 값으로
   * `/auth/done` App Link를 우리 앱에 이어 줍니다. 비워 두면 `/.well-known/assetlinks.json`이 404입니다.
   */
  ANDROID_CERT_SHA256?: string;
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
    /** Riot API와 RSO를 부를 때 쓰는 `fetch`입니다. 테스트는 가짜로 바꿔 끼워 실제 Riot을 부르지 않습니다. */
    upstream: typeof fetch;
    user: User;
    sessionHash: string;
  };
}
