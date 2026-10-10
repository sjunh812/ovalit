package com.ovalit.core.ui

import androidx.compose.runtime.Composable
import com.ovalit.core.ui.resources.Res
import com.ovalit.core.ui.resources.josa_enabled
import org.jetbrains.compose.resources.stringResource

/**
 * 이름 뒤에 붙는 조사입니다.
 * 받침이 있으면 [withBatchim], 없으면 [withoutBatchim]을 붙입니다.
 * 요원, 맵, 무기 이름은 카탈로그에서 오니 문장 틀에 조사를 박아 두면 "레이즈으로", "오퍼레이터을"처럼 틀립니다.
 * 닉네임에는 붙이지 않습니다.
 */
enum class Josa(internal val withBatchim: String, internal val withoutBatchim: String) {
    I_GA("이", "가"),
    EUN_NEUN("은", "는"),
    EUL_REUL("을", "를"),

    /** ㄹ 받침 뒤에도 "로"입니다("밴달로"). */
    EURO_RO("으로", "로"),
}

/** 이름 뒤에 조사를 붙입니다. 끝 글자가 한글이 아니면 받침이 없다고 봅니다. */
fun String.withJosa(josa: Josa): String {
    val last = lastOrNull() ?: return this
    if (last !in '가'..'힣') return this + josa.withoutBatchim
    val batchim = (last - '가') % 28
    val takesWithout = batchim == 0 || (josa == Josa.EURO_RO && batchim == RIEUL)
    return this + if (takesWithout) josa.withoutBatchim else josa.withBatchim
}

// 받침 순서에서 ㄹ의 자리다(ㄱ ㄲ ㄳ ㄴ ㄵ ㄶ ㄷ ㄹ)
private const val RIEUL = 8

/**
 * 화면 언어가 한국어일 때만 [withJosa]로 조사를 붙입니다.
 * 다른 언어는 문장 틀에 조사를 직접 써 두고 이름만 받습니다.
 * 일본어 이름에 붙이면 "レイズ로"가 됩니다.
 */
@Composable
fun String.withLocalJosa(josa: Josa): String = if (stringResource(Res.string.josa_enabled) == "true") withJosa(josa) else this
