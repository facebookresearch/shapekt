/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin.synthetic.parsing

import shapeTyping.plugin.synthetic.parsing.Token.TokenType
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

// TODO: Revise failure cases when we have proper diagnostics

class LexingTest {

    @Test
    fun testSimpleShapeArguments() {
        val input = "[10,Moo,*], Dim, B"
        val tokens = STypeLexer(input, ParseMode.ARGUMENT).lex().tokens
        val expectedTokens = listOf(
            Token("[", 0, 1, TokenType.LSQUARE),
            Token("10", 1, 3, TokenType.INTLITERAL),
            Token(",", 3, 4, TokenType.SEP),
            Token("Moo",4, 7, TokenType.IDENTIFIER),
            Token(",", 7, 8, TokenType.SEP),
            Token("*", 8, 9, TokenType.WILDCARD),
            Token("]", 9, 10, TokenType.RSQUARE),
            Token(",", 10, 11, TokenType.SEP),
            Token("Dim", 12, 15, TokenType.DIM),
            Token(",", 15, 16, TokenType.SEP),
            Token("B", 17, 18, TokenType.IDENTIFIER)
        )
        assertEquals(expectedTokens, tokens)
    }

    @Test
    fun testShapeFunctionArgument() {
        val input = "F( [10,Moo,*], g(A,B) ), Dim, B"
        val tokens = STypeLexer(input, ParseMode.ARGUMENT).lex().tokens
        val expectedTokens = listOf(
            Token("F", 0, 1, TokenType.IDENTIFIER),
            Token("(", 1, 2, TokenType.LPARENS),
            Token("[", 3, 4, TokenType.LSQUARE),
            Token("10", 4, 6, TokenType.INTLITERAL),
            Token(",", 6, 7, TokenType.SEP),
            Token("Moo", 7, 10, TokenType.IDENTIFIER),
            Token(",", 10, 11, TokenType.SEP),
            Token("*", 11, 12, TokenType.WILDCARD),
            Token("]", 12, 13, TokenType.RSQUARE),
            Token(",", 13, 14, TokenType.SEP),
            Token("g", 15, 16, TokenType.IDENTIFIER),
            Token("(", 16, 17, TokenType.LPARENS),
            Token("A", 17, 18, TokenType.IDENTIFIER),
            Token(",", 18, 19, TokenType.SEP),
            Token("B", 19, 20, TokenType.IDENTIFIER),
            Token(")", 20, 21, TokenType.RPARENS),
            Token(")", 22, 23, TokenType.RPARENS),
            Token(",", 23, 24, TokenType.SEP),
            Token("Dim", 25, 28, TokenType.DIM),
            Token(",", 28, 29, TokenType.SEP),
            Token("B", 30, 31, TokenType.IDENTIFIER)
        )
        assertEquals(expectedTokens, tokens)
    }

    @Test
    fun testLexingwithWhitespace() {
        val input = "F( [10,Moo,*], g(A,B) ), Dim, B"
        val tokens = STypeLexer(input, ParseMode.ARGUMENT).lex(includeWhitespace = true).tokens
        val expectedTokens = listOf(
            Token("F", 0, 1, TokenType.IDENTIFIER),
            Token("(", 1, 2, TokenType.LPARENS),
            Token(" ", 2, 3, TokenType.WHITESPACE),
            Token("[", 3, 4, TokenType.LSQUARE),
            Token("10", 4, 6, TokenType.INTLITERAL),
            Token(",", 6, 7, TokenType.SEP),
            Token("Moo", 7, 10, TokenType.IDENTIFIER),
            Token(",", 10, 11, TokenType.SEP),
            Token("*", 11, 12, TokenType.WILDCARD),
            Token("]", 12, 13, TokenType.RSQUARE),
            Token(",", 13, 14, TokenType.SEP),
            Token(" ", 14, 15, TokenType.WHITESPACE),
            Token("g", 15, 16, TokenType.IDENTIFIER),
            Token("(", 16, 17, TokenType.LPARENS),
            Token("A", 17, 18, TokenType.IDENTIFIER),
            Token(",", 18, 19, TokenType.SEP),
            Token("B", 19, 20, TokenType.IDENTIFIER),
            Token(")", 20, 21, TokenType.RPARENS),
            Token(" ", 21, 22, TokenType.WHITESPACE),
            Token(")", 22, 23, TokenType.RPARENS),
            Token(",", 23, 24, TokenType.SEP),
            Token(" ", 24, 25, TokenType.WHITESPACE),
            Token("Dim", 25, 28, TokenType.DIM),
            Token(",", 28, 29, TokenType.SEP),
            Token(" ", 29, 30, TokenType.WHITESPACE),
            Token("B", 30, 31, TokenType.IDENTIFIER)
        )
        assertEquals(expectedTokens, tokens)
    }


    @Test
    fun testDeclarationsInArgsMode() {
        val input = "B: [10,Moo,*], M <: Dim, C : Fruit"
        val e = assertFailsWith(Exception::class) { STypeLexer(input, ParseMode.ARGUMENT).lex() }
        assertEquals("Invalid token : at 1", e.message)
    }

    @Test
    fun testValidDeclarations() {
        val input = "B: [10,Moo,*], M <: Dim, C : Fruit"
        val tokens = STypeLexer(input, ParseMode.DECLARATION).lex().tokens
        val expectedTokens = listOf(
            Token("B", 0, 1, TokenType.IDENTIFIER),
            Token(":", 1, 2, TokenType.SUBTYPE),
            Token("[", 3, 4, TokenType.LSQUARE),
            Token("10", 4, 6, TokenType.INTLITERAL),
            Token(",", 6, 7, TokenType.SEP),
            Token("Moo", 7, 10, TokenType.IDENTIFIER),
            Token(",", 10, 11, TokenType.SEP),
            Token("*", 11, 12, TokenType.WILDCARD),
            Token("]", 12, 13, TokenType.RSQUARE),
            Token(",", 13, 14, TokenType.SEP),
            Token("M", 15, 16, TokenType.IDENTIFIER),
            Token("<:", 17, 19, TokenType.STRICTSUBTYPE),
            Token("Dim", 20, 23, TokenType.DIM),
            Token(",", 23, 24, TokenType.SEP),
            Token("C", 25, 26, TokenType.IDENTIFIER),
            Token(":", 27, 28, TokenType.SUBTYPE),
            Token("Fruit", 29, 34, TokenType.IDENTIFIER)
        )
        assertEquals(expectedTokens, tokens)
    }

    @Test
    fun testMalformedStrictSubtype() {
        val input = "B: [10,Moo,*], M < : Dim, C : Fruit"
        val e = assertFailsWith<Exception> { STypeLexer(input, ParseMode.DECLARATION).lex() }
        assertEquals("Invalid token < at 17", e.message)
    }

    @Test
    fun testIllegalIdentifier() {
        val input = "B: [10,Moo,*], !8M <: Dim, C : Fruit"
        val e = assertFailsWith<Exception> { STypeLexer(input, ParseMode.DECLARATION).lex() }
        assertEquals("Invalid token ! at 15", e.message)
    }

}