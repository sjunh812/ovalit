/**
 * 실패를 이름만 남깁니다.
 * D1 에러에는 SQL과 PUUID가, FCM 에러에는 기기 토큰이, RSO 콜백에는 Riot 인가 코드가 섞일 수 있어서 에러 객체를 통째로 찍지 않습니다.
 */
export function logFailure(label: string, err: unknown): void {
  console.error(label, err instanceof Error ? err.name : typeof err);
}
