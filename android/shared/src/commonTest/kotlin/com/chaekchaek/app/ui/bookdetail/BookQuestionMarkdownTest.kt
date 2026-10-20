package com.chaekchaek.app.ui.bookdetail

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BookQuestionMarkdownTest {
    @Test
    fun `중첩 강조와 기울임을 실제 텍스트 구간에 적용한다`() {
        val runs = parseBookQuestionMarkdown("평문 **굵게 *함께*** 뒤").single().runs

        assertEquals("굵게 ", runs.filter { it.strong && !it.emphasis }.joinToString("") { it.text })
        assertEquals("함께", runs.filter { it.strong && it.emphasis }.joinToString("") { it.text })
        assertEquals("평문 굵게 함께 뒤", runs.joinToString("") { it.text })
    }

    @Test
    fun `제목 목록과 인용 구조를 보존한다`() {
        val blocks = parseBookQuestionMarkdown("# 질문\n\n- 첫째\n- 둘째\n\n3. 셋째\n\n> 인용")

        assertTrue(blocks.first().heading)
        assertEquals("질문", blocks.first().text().trim())
        assertEquals(listOf("-", "-", "3."), blocks.mapNotNull { it.listMarker })
        assertTrue(blocks.last().quoted)
        assertEquals("인용", blocks.last().text().trim())
    }

    @Test
    fun `본문 코드 안의 서식과 HTML은 실행하지 않는 문자로 보존한다`() {
        val runs = parseBookQuestionMarkdown("예시 `**문자** <script>bad()</script>`").single().runs
        val code = runs.single { it.code }

        assertEquals("**문자** <script>bad()</script>", code.text)
        assertFalse(code.strong)
    }

    @Test
    fun `코드 블록의 줄바꿈을 유지하고 언어 표시는 본문에서 제외한다`() {
        val block = parseBookQuestionMarkdown("```kotlin\nval a = 1\nval b = 2\n```").single()

        assertTrue(block.code)
        assertEquals("val a = 1\nval b = 2", block.text())
    }

    @Test
    fun `링크는 문구만 표시하고 목적지와 제목은 화면 모델에서 제거한다`() {
        val blocks = parseBookQuestionMarkdown("[**설명**](https://example.invalid/private \"비공개 제목\")")

        assertEquals("설명", blocks.single().text())
        assertTrue(blocks.single().runs.any { it.text == "설명" && it.strong })
        assertFalse(blocks.toString().contains("https://"))
        assertFalse(blocks.toString().contains("비공개 제목"))
    }

    @Test
    fun `스크립트와 이미지 자산을 화면 모델에 넣지 않는다`() {
        val source = "<script>bad()</script>\n\n![그림](https://example.invalid/tracker.png)\n\n안전한 문장"
        val blocks = parseBookQuestionMarkdown(source)

        assertEquals("안전한 문장", blocks.single().text())
        assertFalse(blocks.toString().contains("script"))
        assertFalse(blocks.toString().contains("tracker.png"))
    }

    @Test
    fun `인라인 HTML 속성과 위험 링크도 실행 경로 없이 문구만 남긴다`() {
        val blocks = parseBookQuestionMarkdown("앞 <img src=x onerror=bad()> [읽기](javascript:bad()) 뒤")
        val text = blocks.single().text()

        assertTrue(text.contains("읽기"))
        assertFalse(text.contains("onerror"))
        assertFalse(text.contains("javascript"))
        assertFalse(text.contains("<img"))
    }

    @Test
    fun `참조형 링크의 문구만 남기고 정의는 표시하지 않는다`() {
        val blocks = parseBookQuestionMarkdown("[참고][source]\n\n[source]: https://example.invalid/hidden")

        assertEquals("참고", blocks.single().text())
        assertFalse(blocks.toString().contains("example.invalid"))
    }

    @Test
    fun `운영체제별 줄바꿈을 같은 문서로 해석한다`() {
        val lf = "# 제목\n\n첫 줄\n둘째 줄"

        assertEquals(parseBookQuestionMarkdown(lf), parseBookQuestionMarkdown(lf.replace("\n", "\r\n")))
        assertEquals(parseBookQuestionMarkdown(lf), parseBookQuestionMarkdown(lf.replace("\n", "\r")))
    }

    private fun BookQuestionMarkdownBlock.text(): String = runs.joinToString("") { it.text }
}
