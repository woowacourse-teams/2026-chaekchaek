package com.chaekchaek.app.ui.bookdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaekchaek.app.ui.theme.ChaekInk
import com.chaekchaek.app.ui.theme.ChaekInkSecondary
import com.chaekchaek.app.ui.theme.ChaekSurfaceMuted
import org.intellij.markdown.MarkdownElementTypes as Elements
import org.intellij.markdown.MarkdownTokenTypes as Tokens
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.parser.CancellationToken
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

internal data class BookQuestionMarkdownRun(
    val text: String,
    val strong: Boolean = false,
    val emphasis: Boolean = false,
    val code: Boolean = false,
)

internal data class BookQuestionMarkdownBlock(
    val runs: List<BookQuestionMarkdownRun>,
    val heading: Boolean = false,
    val quoted: Boolean = false,
    val code: Boolean = false,
    val indent: Int = 0,
    val listMarker: String? = null,
)

/** 목적지나 이미지 자산을 화면 모델에 담지 않는 읽기 전용 Markdown 변환이다. */
internal fun parseBookQuestionMarkdown(markdown: String): List<BookQuestionMarkdownBlock> {
    val source = markdown.replace("\r\n", "\n").replace('\r', '\n')
    val tree = MarkdownParser(CommonMarkFlavourDescriptor(), cancellationToken = CancellationToken.NonCancellable)
        .buildMarkdownTreeFromString(source as CharSequence)
    return BookQuestionMarkdownParser(source).blocks(tree)
}

private class BookQuestionMarkdownParser(private val source: String) {
    fun blocks(node: ASTNode, indent: Int = 0, quoted: Boolean = false): List<BookQuestionMarkdownBlock> =
        when (node.type) {
            Elements.HTML_BLOCK, Elements.LINK_DEFINITION -> emptyList()
            Elements.PARAGRAPH -> listOf(BookQuestionMarkdownBlock(inlines(node), quoted = quoted, indent = indent))
            Elements.ATX_1, Elements.ATX_2, Elements.ATX_3, Elements.ATX_4, Elements.ATX_5, Elements.ATX_6,
            Elements.SETEXT_1, Elements.SETEXT_2 -> listOf(
                BookQuestionMarkdownBlock(
                    runs = node.children.filter { it.type == Tokens.ATX_CONTENT || it.type == Tokens.SETEXT_CONTENT }
                        .flatMap { inlines(it) },
                    heading = true,
                    quoted = quoted,
                    indent = indent,
                ),
            )
            Elements.BLOCK_QUOTE -> node.children.flatMap { blocks(it, indent + 1, quoted = true) }
            Elements.UNORDERED_LIST, Elements.ORDERED_LIST -> node.children.filter { it.type == Elements.LIST_ITEM }
                .flatMap { item ->
                    val marker = item.children.firstOrNull { it.type == Tokens.LIST_NUMBER }?.text()?.trim() ?: "-"
                    item.children.flatMap { blocks(it, indent + 1, quoted) }.mapIndexed { index, block ->
                        if (index == 0) block.copy(listMarker = marker) else block
                    }
                }
            Elements.CODE_FENCE, Elements.CODE_BLOCK -> {
                val code = node.children.filter {
                    it.type == Tokens.CODE_FENCE_CONTENT || it.type == Tokens.CODE_LINE || it.type == Tokens.EOL
                }.joinToString("") { it.text() }.trim('\n')
                listOf(BookQuestionMarkdownBlock(listOf(BookQuestionMarkdownRun(code, code = true)), code = true,
                    quoted = quoted, indent = indent))
            }
            else -> node.children.flatMap { blocks(it, indent, quoted) }
        }.filter { block -> block.runs.any { it.text.isNotBlank() } }

    private fun inlines(
        node: ASTNode,
        strong: Boolean = false,
        emphasis: Boolean = false,
    ): List<BookQuestionMarkdownRun> = when (node.type) {
        Elements.IMAGE, Elements.LINK_DESTINATION, Elements.LINK_TITLE, Elements.LINK_DEFINITION,
        Tokens.HTML_TAG, Tokens.HTML_BLOCK_CONTENT -> emptyList()
        Elements.STRONG, Elements.EMPH -> node.children.filterNot { it.type == Tokens.EMPH }.flatMap {
            inlines(it, strong || node.type == Elements.STRONG, emphasis || node.type == Elements.EMPH)
        }
        Elements.INLINE_LINK, Elements.FULL_REFERENCE_LINK, Elements.SHORT_REFERENCE_LINK ->
            node.children.firstOrNull { it.type == Elements.LINK_TEXT || it.type == Elements.LINK_LABEL }
                ?.let { inlines(it, strong, emphasis) }.orEmpty()
        Elements.LINK_TEXT, Elements.LINK_LABEL -> node.children
            .filterNot { it.type == Tokens.LBRACKET || it.type == Tokens.RBRACKET }
            .flatMap { inlines(it, strong, emphasis) }
        Elements.AUTOLINK -> listOf(BookQuestionMarkdownRun(node.text().removePrefix("<").removeSuffix(">"), strong, emphasis))
        Elements.CODE_SPAN -> {
            var code = node.children.filterNot { it.type == Tokens.BACKTICK }.joinToString("") { it.text() }
                .replace('\n', ' ')
            if (code.startsWith(' ') && code.endsWith(' ') && code.isNotBlank()) code = code.drop(1).dropLast(1)
            listOf(BookQuestionMarkdownRun(code, strong, emphasis, code = true))
        }
        Tokens.HARD_LINE_BREAK -> listOf(BookQuestionMarkdownRun("\n", strong, emphasis))
        Tokens.EOL -> listOf(BookQuestionMarkdownRun(" ", strong, emphasis))
        else -> if (node.children.isEmpty()) listOf(BookQuestionMarkdownRun(node.text(), strong, emphasis))
            else node.children.flatMap { inlines(it, strong, emphasis) }
    }

    private fun ASTNode.text(): String = source.substring(startOffset, endOffset)
}

@Composable
internal fun BookQuestionMarkdown(
    markdown: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle(fontSize = 14.sp, lineHeight = 23.sp),
) {
    val blocks = remember(markdown) { parseBookQuestionMarkdown(markdown) }
    val baseStyle = MaterialTheme.typography.bodyMedium.merge(textStyle)
    val codeBackground = ChaekSurfaceMuted
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = (block.indent * 12).dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                block.listMarker?.let { Text(it, color = ChaekInk, style = baseStyle) }
                val contentModifier = if (block.code) {
                    Modifier.weight(1f).background(codeBackground).padding(8.dp)
                } else Modifier.weight(1f)
                Text(
                    text = buildAnnotatedString {
                        block.runs.forEach { run ->
                            withStyle(SpanStyle(
                                fontWeight = if (run.strong) FontWeight.Bold else null,
                                fontStyle = if (run.emphasis) FontStyle.Italic else null,
                                background = if (run.code) codeBackground else Color.Unspecified,
                            )) { append(run.text) }
                        }
                    },
                    modifier = if (block.heading) contentModifier.semantics { heading() } else contentModifier,
                    color = if (block.quoted) ChaekInkSecondary else ChaekInk,
                    style = if (block.heading) baseStyle.copy(fontWeight = FontWeight.Bold) else baseStyle,
                )
            }
        }
    }
}
