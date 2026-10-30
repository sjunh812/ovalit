package com.ovalit.feature.match

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import kotlin.test.Test
import kotlin.test.assertEquals

class EmphasizeArgumentTest {

    private val bold = SpanStyle(fontWeight = FontWeight.SemiBold)

    // 문장 앞의 "1,000"에 있는 1을 칠하던 버그가 있었다
    @Test
    fun `이긴 횟수가 1이어도 앞의 1000이 아니라 횟수를 굵게 한다`() {
        val template = "장비가 상대보다 1,000 넘게 적었는데도 %1\$d번 이겼어요."
        val sentence = "장비가 상대보다 1,000 넘게 적었는데도 1번 이겼어요."

        val text = emphasizeArgument(template, sentence, "1", bold)

        val span = text.spanStyles.single()
        assertEquals(sentence.indexOf("1번"), span.start)
        assertEquals("1", text.substring(span.start, span.end))
    }

    @Test
    fun `일본어 문장도 자리 표시가 있던 곳을 굵게 한다`() {
        val template = "装備が相手より1,000を超えて少なかったのに、%1\$d回勝ちました。"
        val sentence = "装備が相手より1,000を超えて少なかったのに、10回勝ちました。"

        val text = emphasizeArgument(template, sentence, "10", bold)

        val span = text.spanStyles.single()
        assertEquals("10回", text.substring(span.start, span.end + 1))
    }
}
