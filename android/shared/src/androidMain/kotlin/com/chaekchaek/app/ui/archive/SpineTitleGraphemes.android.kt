package com.chaekchaek.app.ui.archive

import java.text.BreakIterator

internal actual fun spineTitleGraphemes(title: String): List<String> {
    // Android의 java.text 문자 분리기는 ICU 문자 분리기에 위임한다.
    val iterator = BreakIterator.getCharacterInstance().apply { setText(title) }
    return buildList {
        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            add(title.substring(start, end))
            start = end
            end = iterator.next()
        }
    }
}
