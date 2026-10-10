export interface Env {
  DB: D1Database;
  RIOT_API_KEY?: string;
  RSO_CLIENT_ID?: string;
  RSO_CLIENT_SECRET?: string;
  /**
   * RSO state에 HMAC 서명하는 키입니다. 32자 이상이어야 하고 `openssl rand -base64 32`로 만듭니다.
   * 없거나 짧으면 RSO를 띄우지 않습니다(503 `rso_not_configured`).
   * 바꾸면 그 전에 시작해 아직 Riot 로그인 중인 사람만 다시 시작합니다.
   */
  STATE_SECRET?: string;
  /**
   * `"true"`이고 로컬 주소(localhost, 127.0.0.1, [::1], 10.0.2.2)로 들어온 요청에만 `/auth/dev`가 열립니다.
   * 주소는 Host 헤더로 가려서 꾸밀 수 있으니 배포 환경에는 절대 넣지 않습니다.
   * `RSO_CLIENT_SECRET`이 있으면 이 값과 상관없이 닫힙니다.
   */
  DEV_LOGIN?: string;
  /**
   * 앱 서명 인증서의 SHA-256 지문입니다. 여럿이면 쉼표로 잇습니다(디버그 키, Play 앱 서명 키).
   * 안드로이드가 이 값으로 `/auth/done` App Link를 우리 앱에 이어 줍니다. 비워 두면 `/.well-known/assetlinks.json`이 404입니다.
   */
  ANDROID_CERT_SHA256?: string;
  /**
   * FCM HTTP v1로 알림을 보낼 Google 서비스 계정 JSON 원문입니다(`project_id`, `client_email`, `private_key`).
   * 비워 두거나 읽을 수 없는 값이면 알림을 보내지 않고 넘어갑니다.
   */
  FCM_SERVICE_ACCOUNT?: string;
}

export interface User {
  id: number;
  puuid: string;
  gameName: string;
  tagLine: string;
  statsPublic: boolean;
  /** ㅇㅂㅇ 미리 알림을 시작 몇 분 전에 받을지입니다. 0이면 받지 않습니다. */
  remindBefore: number;
}

export interface AppEnv {
  Bindings: Env;
  Variables: {
    /** Riot API, RSO, FCM을 부를 때 쓰는 `fetch`입니다. 테스트는 가짜로 바꿔 끼워 실제로 밖에 나가지 않습니다. */
    upstream: typeof fetch;
    user: User;
    sessionHash: string;
  };
}
