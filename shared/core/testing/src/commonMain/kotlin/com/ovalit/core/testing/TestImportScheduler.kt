package com.ovalit.core.testing

import com.ovalit.core.data.ImportScheduler

/** 맡긴 일을 적어 두기만 하는 [ImportScheduler]입니다. 경기를 받지는 않습니다. */
class TestImportScheduler : ImportScheduler {

    var started = false
        private set

    var retried = false
        private set

    /** 새 경기 이어 받기를 맡겼는지입니다([continueNewMatches]). */
    var continued = false
        private set

    var cancelled = false
        private set

    override fun start() {
        started = true
    }

    override fun retry() {
        retried = true
    }

    override fun continueNewMatches(total: Int) {
        continued = true
    }

    override fun cancel() {
        cancelled = true
    }
}
