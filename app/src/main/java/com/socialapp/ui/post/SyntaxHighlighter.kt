package com.socialapp.ui.post

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

object SyntaxHighlighter {
    fun highlight(code: String): AnnotatedString {
        // Danh sách các từ khóa phổ biến
        val keywords = listOf(
            "val", "var", "fun", "class", "package", "import", "return",
            "if", "else", "when", "for", "while", "do", "break", "continue",
            "interface", "object", "override", "private", "public", "protected",
            "String", "Int", "Boolean", "Long", "Double", "Float", "true", "false", "null"
        )

        return buildAnnotatedString {
            append(code)

            // 1. Tô màu Keywords (Màu Hồng/Hỏa tiễn)
            keywords.forEach { keyword ->
                val regex = "\\b$keyword\\b".toRegex()
                regex.findAll(code).forEach { result ->
                    addStyle(
                        style = SpanStyle(color = Color(0xFFF92672), fontWeight = FontWeight.Bold),
                        start = result.range.first,
                        end = result.range.last + 1
                    )
                }
            }

            // 2. Tô màu Strings (Màu Vàng)
            "(\".*?\")|('.*?')".toRegex().findAll(code).forEach { result ->
                addStyle(
                    style = SpanStyle(color = Color(0xFFE6DB74)),
                    start = result.range.first,
                    end = result.range.last + 1
                )
            }

            // 3. Tô màu Comments (Màu Xám/Xanh lá nhạt)
            "//.*|/\\*.*?\\*/".toRegex(RegexOption.DOT_MATCHES_ALL).findAll(code).forEach { result ->
                addStyle(
                    style = SpanStyle(color = Color(0xFF75715E)),
                    start = result.range.first,
                    end = result.range.last + 1
                )
            }
        }
    }
}