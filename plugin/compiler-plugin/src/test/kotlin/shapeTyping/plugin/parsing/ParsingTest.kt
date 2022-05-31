/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin.synthetic.parsing

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

// TODO: Revise failure cases when we have proper diagnostics

class ParsingTest {

    @Test
    fun testSimpleShapeArguments() {
        // [10,Moo,*], Dim, B
        val input = LexStream(listOf(
            Token("[", 0, 1, Token.TokenType.LSQUARE),
            Token("10", 1, 3, Token.TokenType.INTLITERAL),
            Token(",", 3, 4, Token.TokenType.SEP),
            Token("Moo",4, 7, Token.TokenType.IDENTIFIER),
            Token(",", 7, 8, Token.TokenType.SEP),
            Token("*", 8, 9, Token.TokenType.WILDCARD),
            Token("]", 9, 10, Token.TokenType.RSQUARE),
            Token(",", 10, 11, Token.TokenType.SEP),
            Token("Dim", 12, 15, Token.TokenType.DIM),
            Token(",", 15, 16, Token.TokenType.SEP),
            Token("B", 17, 18, Token.TokenType.IDENTIFIER)
        ))
        val parsed = Parser(input, ParseMode.ARGUMENT, emptyList()).parse()
        val expectedParse = ArgumentList(
            arguments = listOf(
                ShapeLiteral(
                    dims = listOf(
                        IntLiteral(10, startIdx = 1, endIdx = 3),
                        STypeIdentifier("Moo", dataType = null, startIdx = 4, endIdx = 7, null, false, null),
                        Wildcard(dataType = null, startIdx = 8, endIdx = 9)
                    ),
                    startIdx = 0,
                    endIdx = 10
                ),
                STypeIdentifier("Dim", dataType = DataType.DIM, startIdx = 12, endIdx = 15, null, false, null),
                STypeIdentifier("B", dataType = null, startIdx = 17, endIdx = 18, null, false, null)
            ),
            startIdx = 0,
            endIdx = 18
        )
        assertEquals(expectedParse, parsed)
    }

    @Test
    fun testSimpleDeclaration() {
        // B: [10,Moo,*], M <: Dim
        val input = LexStream(listOf(
            Token("B", 0, 1, Token.TokenType.IDENTIFIER),
            Token(":", 1, 2, Token.TokenType.SUBTYPE),
            Token("[", 3, 4, Token.TokenType.LSQUARE),
            Token("10", 4, 6, Token.TokenType.INTLITERAL),
            Token(",", 6, 7, Token.TokenType.SEP),
            Token("Moo", 7, 10, Token.TokenType.IDENTIFIER),
            Token(",", 10, 11, Token.TokenType.SEP),
            Token("*", 11, 12, Token.TokenType.WILDCARD),
            Token("]", 12, 13, Token.TokenType.RSQUARE),
            Token(",", 13, 14, Token.TokenType.SEP),
            Token("M", 15, 16, Token.TokenType.IDENTIFIER),
            Token("<:", 17, 19, Token.TokenType.STRICTSUBTYPE),
            Token("Dim", 20, 23, Token.TokenType.DIM),
        ))
        val parsed = Parser(input, ParseMode.DECLARATION, emptyList()).parse()
        // B: [10,Moo,*], M <: Dim
        val bLiteral = ShapeLiteral(
            dims = listOf(
                IntLiteral(10, startIdx = 4, endIdx = 6),
                STypeIdentifier("Moo", dataType = null, startIdx = 7, endIdx = 10, null, false, null),
                Wildcard(dataType = null, startIdx = 11, endIdx = 12)
            ),
            startIdx = 3,
            endIdx = 13
        )
        val mDim = STypeIdentifier("Dim", DataType.DIM, startIdx = 20, endIdx = 23, null, false, null)
        val expectedParse = DeclarationList(
            declarations = listOf(
                STypeDeclaration(
                    id = STypeIdentifier("B", dataType = DataType.SHAPE, startIdx = 0, endIdx = 1, bLiteral, false, null),
                    startIdx = 0,
                    endIdx = 13
                ),
                STypeDeclaration(
                    id = STypeIdentifier("M", dataType = DataType.DIM, startIdx = 15, endIdx = 16, mDim, true, null),
                    startIdx = 15,
                    endIdx = 23,
                )
            ),
            startIdx = 0,
            endIdx = 23
        )
        assertEquals(expectedParse, parsed)
    }

    @Test
    fun testDeclarationWithInternalReference() {
        // B: [10,Moo,*], M <: B
        val input = LexStream(listOf(
            Token("B", 0, 1, Token.TokenType.IDENTIFIER),
            Token(":", 1, 2, Token.TokenType.SUBTYPE),
            Token("[", 3, 4, Token.TokenType.LSQUARE),
            Token("10", 4, 6, Token.TokenType.INTLITERAL),
            Token(",", 6, 7, Token.TokenType.SEP),
            Token("Moo", 7, 10, Token.TokenType.IDENTIFIER),
            Token(",", 10, 11, Token.TokenType.SEP),
            Token("*", 11, 12, Token.TokenType.WILDCARD),
            Token("]", 12, 13, Token.TokenType.RSQUARE),
            Token(",", 13, 14, Token.TokenType.SEP),
            Token("M", 15, 16, Token.TokenType.IDENTIFIER),
            Token("<:", 17, 19, Token.TokenType.STRICTSUBTYPE),
            Token("B", 20, 21, Token.TokenType.IDENTIFIER),
        ))
        val parser = Parser(input, ParseMode.DECLARATION, emptyList(), scopeId = 1)
        val parsed = parser.parse()

        val expectedBUpperBound = ShapeLiteral(
            dims = listOf(
                IntLiteral(10, startIdx = 4, endIdx = 6),
                STypeIdentifier("Moo", dataType = null, startIdx = 7, endIdx = 10, null, false, null),
                Wildcard(dataType = null, startIdx = 11, endIdx = 12)
            ),
            startIdx = 3,
            endIdx = 13
        )
        val expectedB = STypeDeclaration(
            id = STypeIdentifier("B", dataType = DataType.SHAPE, startIdx = 0, endIdx = 1, scopeId = 1, upperBound = expectedBUpperBound, isStrictBound = false),
            startIdx = 0,
            endIdx = 13
        )

        val expectedMUpperBound = STypeIdentifier("B", DataType.SHAPE, startIdx = 20, endIdx = 21, expectedB.id.upperBound, false, 1)
        val expectedM = STypeDeclaration(
            id = STypeIdentifier("M", dataType = DataType.SHAPE, startIdx = 15, endIdx = 16, scopeId = 1, upperBound = expectedMUpperBound, isStrictBound = true),
            startIdx = 15,
            endIdx = 21,
        )

        val expectedParse = DeclarationList(
            declarations = listOf(
                expectedB,
                expectedM
            ),
            startIdx = 0,
            endIdx = 21
        )
        assertEquals(expectedParse, parsed)

        assertEquals(parser.declarationInfo?.lookupInScope("B"), expectedB)
        assertEquals(parser.declarationInfo?.lookupInScope("M"), expectedM)
    }

    @Test
    fun testShapeFunctionArgument() {
        // F( [10,Moo,*], g(A,B) ), Dim, B
        val input = LexStream(listOf(
            Token("F", 0, 1, Token.TokenType.IDENTIFIER),
            Token("(", 1, 2, Token.TokenType.LPARENS),
            Token("[", 3, 4, Token.TokenType.LSQUARE),
            Token("10", 4, 6, Token.TokenType.INTLITERAL),
            Token(",", 6, 7, Token.TokenType.SEP),
            Token("Moo", 7, 10, Token.TokenType.IDENTIFIER),
            Token(",", 10, 11, Token.TokenType.SEP),
            Token("*", 11, 12, Token.TokenType.WILDCARD),
            Token("]", 12, 13, Token.TokenType.RSQUARE),
            Token(",", 13, 14, Token.TokenType.SEP),
            Token("g", 15, 16, Token.TokenType.IDENTIFIER),
            Token("(", 16, 17, Token.TokenType.LPARENS),
            Token("A", 17, 18, Token.TokenType.IDENTIFIER),
            Token(",", 18, 19, Token.TokenType.SEP),
            Token("B", 19, 20, Token.TokenType.IDENTIFIER),
            Token(")", 20, 21, Token.TokenType.RPARENS),
            Token(")", 22, 23, Token.TokenType.RPARENS),
            Token(",", 23, 24, Token.TokenType.SEP),
            Token("Dim", 25, 28, Token.TokenType.DIM),
            Token(",", 28, 29, Token.TokenType.SEP),
            Token("B", 30, 31, Token.TokenType.IDENTIFIER)
        ))
        val parsed = Parser(input, ParseMode.ARGUMENT, emptyList()).parse()
        val expectedParse = ArgumentList(
            arguments = listOf(
                SFunctionCall(
                    name = FunctionIdentifier("F", startIdx = 0, endIdx = 1),
                    arguments = ArgumentList(
                        listOf(
                            ShapeLiteral(
                                dims = listOf(
                                    IntLiteral(10, startIdx = 4, endIdx = 6),
                                    STypeIdentifier("Moo", dataType = null, startIdx = 7, endIdx = 10, null, false,null),
                                    Wildcard(dataType = null, startIdx = 11, endIdx = 12)
                                ),
                                startIdx = 3,
                                endIdx = 13
                            ),
                            SFunctionCall(
                                name = FunctionIdentifier("g", startIdx = 15, endIdx = 16),
                                arguments = ArgumentList(
                                    listOf(
                                        STypeIdentifier("A", dataType = null, startIdx = 17, endIdx = 18, scopeId = null, upperBound = null, isStrictBound = false),
                                        STypeIdentifier("B", dataType = null, startIdx = 19, endIdx = 20, scopeId = null, upperBound = null, isStrictBound = false),
                                    ),
                                    startIdx = 16,
                                    endIdx = 21
                                ),
                                dataType = null,
                                startIdx = 15,
                                endIdx = 21
                            )
                        ),
                        startIdx = 1,
                        endIdx = 23
                    ),
                    dataType = null,
                    startIdx = 0,
                    endIdx = 23
                ),
                STypeIdentifier("Dim", DataType.DIM, 25, 28, null, false, null),
                STypeIdentifier("B", null, 30, 31, null, false, null),
            ),
            startIdx = 0,
            endIdx = 31
        )
        assertEquals(expectedParse, parsed)
    }

    @Test
    fun testUnmatchedParensInArgument() {
        // F( [10,Moo,*], g(A,B ), Dim, B
        val input = LexStream(listOf(
            Token("F", 0, 1, Token.TokenType.IDENTIFIER),
            Token("(", 1, 2, Token.TokenType.LPARENS),
            Token("[", 3, 4, Token.TokenType.LSQUARE),
            Token("10", 4, 6, Token.TokenType.INTLITERAL),
            Token(",", 6, 7, Token.TokenType.SEP),
            Token("Moo", 7, 10, Token.TokenType.IDENTIFIER),
            Token(",", 10, 11, Token.TokenType.SEP),
            Token("*", 11, 12, Token.TokenType.WILDCARD),
            Token("]", 12, 13, Token.TokenType.RSQUARE),
            Token(",", 13, 14, Token.TokenType.SEP),
            Token("g", 15, 16, Token.TokenType.IDENTIFIER),
            Token("(", 16, 17, Token.TokenType.LPARENS),
            Token("A", 17, 18, Token.TokenType.IDENTIFIER),
            Token(",", 18, 19, Token.TokenType.SEP),
            Token("B", 19, 20, Token.TokenType.IDENTIFIER),
            Token(")", 21, 22, Token.TokenType.RPARENS),
            Token(",", 22, 23, Token.TokenType.SEP),
            Token("Dim", 24, 27, Token.TokenType.DIM),
            Token(",", 27, 28, Token.TokenType.SEP),
            Token("B", 29, 30, Token.TokenType.IDENTIFIER)
        ))
        val e = assertFailsWith<Exception> { Parser(input, ParseMode.ARGUMENT, emptyList()).parse() }
        assertEquals("Unmatched left parenthesis ( at 1", e.message)
    }


    @Test
    fun testUnclosedShapeLiteral() {
        // B: [10,Moo,*, M <: Dim
        val input = LexStream(listOf(
            Token("B", 0, 1, Token.TokenType.IDENTIFIER),
            Token(":", 1, 2, Token.TokenType.SUBTYPE),
            Token("[", 3, 4, Token.TokenType.LSQUARE),
            Token("10", 4, 6, Token.TokenType.INTLITERAL),
            Token(",", 6, 7, Token.TokenType.SEP),
            Token("Moo", 7, 10, Token.TokenType.IDENTIFIER),
            Token(",", 10, 11, Token.TokenType.SEP),
            Token("*", 11, 12, Token.TokenType.WILDCARD),
            Token(",", 12, 13, Token.TokenType.SEP),
            Token("M", 14, 15, Token.TokenType.IDENTIFIER),
            Token("<:", 16, 18, Token.TokenType.STRICTSUBTYPE),
            Token("Dim", 19, 22, Token.TokenType.DIM),
        ))
        val e = assertFailsWith<Exception> { Parser(input, ParseMode.DECLARATION, emptyList()).parse() }
        assertEquals("Expected comma or ], instead got <: at 16", e.message)
    }

    @Test
    fun testMissingBoundInDec() {
        // B: [10,Moo,*], M, C : Fruit
        val input = LexStream(listOf(
            Token("B", 0, 1, Token.TokenType.IDENTIFIER),
            Token(":", 1, 2, Token.TokenType.SUBTYPE),
            Token("[", 3, 4, Token.TokenType.LSQUARE),
            Token("10", 4, 6, Token.TokenType.INTLITERAL),
            Token(",", 6, 7, Token.TokenType.SEP),
            Token("Moo", 7, 10, Token.TokenType.IDENTIFIER),
            Token(",", 10, 11, Token.TokenType.SEP),
            Token("*", 11, 12, Token.TokenType.WILDCARD),
            Token("]", 12, 13, Token.TokenType.RSQUARE),
            Token(",", 13, 14, Token.TokenType.SEP),
            Token("M", 15, 16, Token.TokenType.IDENTIFIER),
            Token(",", 16, 17, Token.TokenType.SEP),
            Token("C", 18, 19, Token.TokenType.IDENTIFIER),
            Token(":", 20, 21, Token.TokenType.SUBTYPE),
            Token("Fruit", 22, 27, Token.TokenType.IDENTIFIER)
        ))
        val e = assertFailsWith<Exception> { Parser(input, ParseMode.DECLARATION, emptyList()).parse() }
        assertEquals("Expected parameter declaration with bound, got ,", e.message)
    }

    @Test
    fun testMissingSeparator() {
        // B: [10,Moo,*]  M <: Dim
        val input = LexStream(listOf(
            Token("B", 0, 1, Token.TokenType.IDENTIFIER),
            Token(":", 1, 2, Token.TokenType.SUBTYPE),
            Token("[", 3, 4, Token.TokenType.LSQUARE),
            Token("10", 4, 6, Token.TokenType.INTLITERAL),
            Token(",", 6, 7, Token.TokenType.SEP),
            Token("Moo", 7, 10, Token.TokenType.IDENTIFIER),
            Token(",", 10, 11, Token.TokenType.SEP),
            Token("*", 11, 12, Token.TokenType.WILDCARD),
            Token("]", 12, 13, Token.TokenType.RSQUARE),
            Token("M", 15, 16, Token.TokenType.IDENTIFIER),
            Token("<:", 17, 19, Token.TokenType.STRICTSUBTYPE),
            Token("Dim", 20, 23, Token.TokenType.DIM),
        ))
        val e = assertFailsWith<Exception> { Parser(input, ParseMode.DECLARATION, emptyList()).parse() }
        assertEquals("Expected comma or end of declarations, instead got M at 15", e.message)
    }
}