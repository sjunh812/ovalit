package com.ovalit.core.model

/**
 * 저장소가 실패한 까닭입니다. 화면은 이것으로 안내 문구를 고르고, 실패를 삼키지 않습니다. 실제 저장소가 HTTP 응답과 네트워크
 * 예외를 이것으로 바꿔 [OvalitException]으로 던집니다.
 */
sealed interface OvalitError {
    /** 인터넷에 연결되어 있지 않습니다. */
    data object Offline : OvalitError

    /** 서버 세션이 끝났습니다(401). 다시 로그인해야 합니다. */
    data object SessionExpired : OvalitError

    /** Riot이 레이트 리밋에 걸렸습니다(503 `riot_rate_limited`). 잠시 뒤 다시 하면 됩니다. */
    data object RiotBusy : OvalitError

    /** Riot 서버가 응답하지 않습니다(502 `riot_unavailable`). */
    data object RiotDown : OvalitError

    /** 그 밖의 실패입니다. */
    data object Unknown : OvalitError
}

class OvalitException(val error: OvalitError, cause: Throwable? = null) : Exception(error.toString(), cause)

/** 저장소가 던진 예외의 까닭입니다. [OvalitException]이 아니면 [OvalitError.Unknown]입니다. */
val Throwable.ovalitError: OvalitError get() = (this as? OvalitException)?.error ?: OvalitError.Unknown
