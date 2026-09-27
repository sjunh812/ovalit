package com.ovalit.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class JosaTest {

    @Test
    fun `받침이 있으면 받침 쪽 조사를 붙인다`() {
        assertEquals("생존율이", "생존율".withJosa(Josa.I_GA))
        assertEquals("팬텀을", "팬텀".withJosa(Josa.EUL_REUL))
        assertEquals("브림스톤으로", "브림스톤".withJosa(Josa.EURO_RO))
    }

    @Test
    fun `받침이 없으면 받침 없는 쪽 조사를 붙인다`() {
        assertEquals("멀티킬 라운드가", "멀티킬 라운드".withJosa(Josa.I_GA))
        assertEquals("오퍼레이터를", "오퍼레이터".withJosa(Josa.EUL_REUL))
        assertEquals("레이즈로", "레이즈".withJosa(Josa.EURO_RO))
        assertEquals("타격대는", "타격대".withJosa(Josa.EUN_NEUN))
    }

    // "밴달으로"가 아니라 "밴달로"다
    @Test
    fun `ㄹ 받침 뒤에는 로를 붙인다`() {
        assertEquals("밴달로", "밴달".withJosa(Josa.EURO_RO))
        assertEquals("밴달을", "밴달".withJosa(Josa.EUL_REUL))
    }

    @Test
    fun `한글로 끝나지 않으면 받침이 없다고 본다`() {
        assertEquals("KAY/O로", "KAY/O".withJosa(Josa.EURO_RO))
    }
}
