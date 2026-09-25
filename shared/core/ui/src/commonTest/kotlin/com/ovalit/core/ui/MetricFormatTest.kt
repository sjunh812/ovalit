package com.ovalit.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class MetricFormatTest {

    @Test
    fun `정수 지표는 반올림해서 보여준다`() {
        assertEquals("186", MetricFormat.INTEGER.format(186.2))
        assertEquals("138", MetricFormat.INTEGER.format(137.7))
    }

    @Test
    fun `소수 지표는 둘째 자리까지 채워서 보여준다`() {
        assertEquals("1.34", MetricFormat.TWO_DECIMALS.format(1.3409))
        assertEquals("1.00", MetricFormat.TWO_DECIMALS.format(1.0))
        assertEquals("0.05", MetricFormat.TWO_DECIMALS.format(0.05))
    }

    @Test
    fun `판당 기록은 첫째 자리까지 보여준다`() {
        assertEquals("16.2", MetricFormat.ONE_DECIMAL.format(16.24))
        assertEquals("4.0", MetricFormat.ONE_DECIMAL.format(3.96))
    }

    @Test
    fun `비율은 백분율 숫자로 보여준다`() {
        assertEquals("21", MetricFormat.PERCENT.format(0.2145))
    }

    // 74%와 69%를 띄워 놓고 변화량에 +6을 쓰면 틀려 보인다. 원래 값으로 빼면 5.8이라 6이 나온다.
    @Test
    fun `변화량은 화면에 보이는 자릿수끼리 뺀다`() {
        assertEquals("+5", MetricFormat.PERCENT.formatChange(current = 0.744, baseline = 0.686))
    }

    @Test
    fun `내려가면 마이너스 기호를 붙인다`() {
        assertEquals("−0.14", MetricFormat.TWO_DECIMALS.formatChange(current = 1.20, baseline = 1.34))
    }

    @Test
    fun `보이는 값이 같으면 부호 없이 0이다`() {
        assertEquals("0", MetricFormat.PERCENT.formatChange(current = 0.2141, baseline = 0.2149))
        assertEquals(0, MetricFormat.PERCENT.direction(current = 0.2141, baseline = 0.2149))
    }
}
