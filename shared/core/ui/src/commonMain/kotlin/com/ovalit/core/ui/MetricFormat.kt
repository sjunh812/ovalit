package com.ovalit.core.ui

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 지표 숫자를 몇 자리까지 보여줄지 정합니다.
 *
 * 변화량은 원래 값이 아니라 화면에 보이는 자릿수로 반올림한 값끼리 뺍니다. 74%와 69%를
 * 나란히 띄워 놓고 변화량에 +6을 쓰면 틀려 보입니다.
 */
enum class MetricFormat(private val scale: Int) {
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

    /** 보이는 자릿수로 반올림한 두 값의 차이를 부호 없이 적습니다. 74%와 69%면 "5"이고, 1.42와 1.29면 "0.13"입니다. */
    fun formatGap(a: Double, b: Double): String = digits(abs(steps(a) - steps(b)))

    fun direction(current: Double, baseline: Double): Int = (steps(current) - steps(baseline)).coerceIn(-1, 1)

    fun steps(value: Double): Int = (value * scale).roundToInt()

    // 음수는 부호를 떼고 자리를 나눈 뒤 다시 붙인다. 나머지 연산에 부호가 남으면 "0.-5"가 된다.
    private fun digits(steps: Int): String {
        val sign = if (steps < 0) "−" else ""
        val size = abs(steps)
        return sign + when (this) {
            INTEGER, PERCENT -> size.toString()
            ONE_DECIMAL -> "${size / 10}.${size % 10}"
            TWO_DECIMALS -> "${size / 100}.${(size % 100).toString().padStart(2, '0')}"
        }
    }
}

fun Int.withThousands(): String {
    val grouped = abs(this).toString().reversed().chunked(3).joinToString(",").reversed()
    return if (this < 0) "−$grouped" else grouped
}
