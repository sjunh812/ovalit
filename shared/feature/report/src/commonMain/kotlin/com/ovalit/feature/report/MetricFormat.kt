package com.ovalit.feature.report

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 지표 숫자를 몇 자리까지 보여줄지 정합니다.
 *
 * 변화량은 원래 값이 아니라 화면에 보이는 자릿수로 반올림한 값끼리 뺍니다. 74%와 69%를
 * 나란히 띄워 놓고 변화량에 +6을 쓰면 틀려 보입니다.
 */
internal enum class MetricFormat(private val scale: Int) {
    INTEGER(scale = 1),
    ONE_DECIMAL(scale = 10),
    TWO_DECIMALS(scale = 100),
    PERCENT(scale = 100),
    ;

    fun format(value: Double): String = digits(steps(value))

    fun formatChange(current: Double, baseline: Double): String {
        val change = steps(current) - steps(baseline)
        val sign = when {
            change > 0 -> "+"
            change < 0 -> "−"
            else -> ""
        }
        return sign + digits(abs(change))
    }

    fun direction(current: Double, baseline: Double): Int = (steps(current) - steps(baseline)).coerceIn(-1, 1)

    private fun steps(value: Double): Int = (value * scale).roundToInt()

    private fun digits(steps: Int): String = when (this) {
        INTEGER, PERCENT -> steps.toString()
        ONE_DECIMAL -> "${steps / 10}.${steps % 10}"
        TWO_DECIMALS -> "${steps / 100}.${(steps % 100).toString().padStart(2, '0')}"
    }
}

/** 계산식에 쓰는 큰 수입니다. `80352`보다 `80,352`가 한눈에 읽힙니다. */
internal fun Int.withThousands(): String = toString().reversed().chunked(3).joinToString(",").reversed()
