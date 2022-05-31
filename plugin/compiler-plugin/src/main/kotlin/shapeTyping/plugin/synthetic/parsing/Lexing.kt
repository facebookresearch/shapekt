/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin.synthetic.parsing

import shapeTyping.analysis.exceptions.STypeParsingError
import java.util.*

class LexStream(val tokens: List<Token>) {
    private var pos = 0

    fun peek(): Token? = if (pos < tokens.size) tokens[pos] else null
    fun consume() = peek()?.also { pos += 1 }

    override fun toString(): String = "LexStream($tokens)"
}

data class Token(
    val text: String,
    val startIdx: Int,
    val endIdx: Int,
    val tokenType: TokenType
) {

    constructor(
        text: Char,
        startIdx: Int,
        tokenType: TokenType
    ) : this(text.toString(), startIdx, startIdx+1, tokenType)

    enum class TokenType {
        WHITESPACE,
        IDENTIFIER,
        INTLITERAL,
        LSQUARE,
        RSQUARE,
        LPARENS,
        RPARENS,
        SEP,
        WILDCARD,
        SUBTYPE,
        STRICTSUBTYPE,
        DIM,
        SHAPE
    }
}

class STypeLexer(val input: String, val parseMode: ParseMode) {

    private var pos = 0

    private fun peek(): Char? =
        if (pos < input.length) input[pos] else null
    private fun consume() = peek()?.also { pos += 1 }

    fun lex(includeWhitespace: Boolean = false): LexStream {
        val builder = mutableListOf<Token>()
        while (pos < input.length) {
            val currPos = pos
            val next = consume() ?: break
            val nextToken = when {
                next == '[' -> Token(next, currPos, Token.TokenType.LSQUARE)
                next == ']' -> Token(next, currPos, Token.TokenType.RSQUARE)
                next == '(' -> Token(next, currPos, Token.TokenType.LPARENS)
                next == ')' -> Token(next, currPos, Token.TokenType.RPARENS)
                next == ',' -> Token(next, currPos, Token.TokenType.SEP)
                next == '*' -> Token(next, currPos, Token.TokenType.WILDCARD)
                next.isJavaIdentifierStart() -> lexIdentifier(next, currPos)
                next.isDigit() -> lexInt(next, currPos)
                next.isWhitespace() -> if (includeWhitespace) lexWhitespace(next, currPos) else null
                next == ':'  ->
                    if (parseMode == ParseMode.DECLARATION)
                        Token(next, currPos, Token.TokenType.SUBTYPE)
                    else throw STypeParsingError("Invalid token : at $currPos") // TODO: File-level diagnostic
                next == '<' ->
                    if (parseMode == ParseMode.DECLARATION && peek() == ':') {
                        consume()
                        Token("<:", currPos, pos, Token.TokenType.STRICTSUBTYPE)
                    }
                    else throw STypeParsingError("Invalid token < at $currPos") // TODO: File-level diagnostic
                else -> throw STypeParsingError("Invalid token $next at $currPos") // TODO: File-level diagnostic
            }
            nextToken?.let { builder.add(it) }
        }
        return LexStream(Collections.unmodifiableList(builder))
    }

    fun lexIdentifier(start: Char, startIdx: Int) : Token {
        if (!start.isJavaIdentifierStart()) throw STypeParsingError("Invalid start to identifier $start at $startIdx")
        var next = peek()
        var name = start.toString()
        while (next?.isJavaIdentifierPart() == true) {
            name += next
            consume()
            next = peek()
        }
        val tokenType = when (name) {
            "Shape" -> Token.TokenType.SHAPE
            "Dim" -> Token.TokenType.DIM
            else -> Token.TokenType.IDENTIFIER
        }
        return Token(name, startIdx, startIdx + name.length, tokenType)
    }

    fun lexInt(start: Char, startIdx: Int) : Token {
        if (!start.isDigit()) {
            throw STypeParsingError("Expected Int in decimal base, got $start at $startIdx")
        }

        var next = peek()
        var result = start.toString()
        while (next?.isDigit() == true) {
            result += next
            consume()
            next = peek()
        }
        return Token(result, startIdx, startIdx + result.length, Token.TokenType.INTLITERAL)
    }

    fun lexWhitespace(start: Char, startIdx: Int) : Token {
        var next = peek()
        var result = start.toString()
        while (next?.isWhitespace() == true) {
            result += next
            consume()
            next = peek()
        }
        return Token(result, startIdx, startIdx + result.length, Token.TokenType.WHITESPACE)
    }

}