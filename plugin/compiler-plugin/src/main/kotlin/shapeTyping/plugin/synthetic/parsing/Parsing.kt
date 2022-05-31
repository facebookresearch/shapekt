/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin.synthetic.parsing

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import shapeTyping.analysis.exceptions.STypeParsingError
import shapeTyping.plugin.checkers.utils.DeclarationInfo
import shapeTyping.plugin.synthetic.parsing.Token.TokenType

enum class ParseMode { DECLARATION, ARGUMENT }

class Parser(
    private val tokens: LexStream,
    val parseMode: ParseMode,
    val outerDeclarationInfo: List<DeclarationInfo>,
    val scopeId: Int? = null
) {

    val declarationInfo = scopeId?.let { DeclarationInfo(mutableMapOf(), scopeId) }
    private val allDeclarationInfo = declarationInfo?.let { listOf(declarationInfo) + outerDeclarationInfo } ?:
        outerDeclarationInfo

    // TODO: Convert Exceptions to diagnostics with indexes in file
    // TODO: This assumes parens only occur in shape function calls. Document or change this.

    fun parse(): SNode = when (parseMode) {
        ParseMode.ARGUMENT -> parseArgumentList() ?: throw STypeParsingError("No shape arguments provided")
        ParseMode.DECLARATION -> parseDeclarationList() ?: throw STypeParsingError("No declarations provided")
    }

    // TODO: Temporarily using null scopeID for "no resolution". Remove when this is hooked up to something.
    // TODO: Mutually recursive parameters?
    fun parseDeclarationList(): DeclarationList? {
        var curr: Token? = tokens.peek() ?: return null
        val start = curr!!.startIdx
        val declarations = mutableListOf<STypeDeclaration>()

        while (curr != null) {
            parseDeclaration()?.let { dec ->
                scopeId?.let { scope -> declarationInfo?.addToScope(dec) }
                declarations.add(dec)
            } ?: throw STypeParsingError("Expected shape parameter declaration, instead got ${curr.text} at ${curr.startIdx}")
            val next = tokens.peek()
            curr = when (next?.tokenType) {
                TokenType.SEP -> {
                    tokens.consume()
                    tokens.peek()
                }
                null -> null
                else -> throw STypeParsingError("Expected comma or end of declarations, instead got ${next.text} at ${next.startIdx}")
            }
        }

        return DeclarationList(declarations, start, declarations.last().endIdx)
    }

    fun parseArgumentList(): ArgumentList? {
        var curr: Token? = tokens.peek() ?: return null
        val start = curr!!.startIdx
        val arguments = mutableListOf<STypeNode>()
        val withParens = (curr.tokenType == TokenType.LPARENS).also { if (it) tokens.consume() }

        while (curr != null && curr.tokenType != TokenType.RPARENS) {
            parseDimOrShape()?.let { arguments.add(it) } ?:
            throw STypeParsingError("Could not process argument ${curr.text} at ${curr.startIdx}")
            val next = tokens.peek()
            curr = when (next?.tokenType) {
                TokenType.SEP -> {
                    tokens.consume()
                    tokens.peek()
                }
                TokenType.RPARENS -> {
                    if (withParens) tokens.consume() else throw STypeParsingError("Unmatched right parenthesis ) at ${curr.startIdx}")
                }
                null -> if (withParens) throw STypeParsingError("Unmatched left parenthesis ( at $start") else null
                else -> throw STypeParsingError("Expected comma or end of arguments, instead got ${next.text} at ${next.startIdx}")
            }
        }

        return if (withParens && curr?.tokenType == TokenType.RPARENS) {
            ArgumentList(arguments, start, curr.endIdx)
        } else {
            ArgumentList(arguments, start, arguments.lastOrNull()?.endIdx ?: (start + 1))
        }
    }

    fun parseDeclaration(): STypeDeclaration? {
        val idUntyped = parseIdentifier(true) ?: return null
        val maybeSubtypeOp = tokens.consume() ?: throw STypeParsingError("Expected subtype specification for ${idUntyped.name}")
        val isStrictBound = when (maybeSubtypeOp.tokenType) {
            TokenType.SUBTYPE -> false
            TokenType.STRICTSUBTYPE -> true
            else -> throw STypeParsingError("Expected parameter declaration with bound, got ${maybeSubtypeOp.text}")
        }
        val constraint = parseDimOrShape() ?: throw STypeParsingError("No constraint specified for parameter ${idUntyped.name}")
        val constraintDataType = constraint.dataType
        val fullId = retypeIdentifier(idUntyped, constraintDataType, constraint, isStrictBound, scopeId)
        return STypeDeclaration(fullId, fullId.startIdx, constraint.endIdx)
    }

    private fun parseIdentifier(isNewDeclaration: Boolean): STypeIdentifier? {
        val maybeId = tokens.peek() ?: return null
        return when (maybeId.tokenType) {
            TokenType.IDENTIFIER -> makeIdentifier(
                maybeId,
                isNewDeclaration,
                dataType = null,
            )
            else -> throw STypeParsingError("Invalid token for shape declaration: ${maybeId.text}")
        }
    }

    private fun parseDimOrShape(): STypeNode? {
        val curr = tokens.peek() ?: return null
        return when (curr.tokenType) {
            TokenType.SHAPE -> makeIdentifier(curr, false, DataType.SHAPE)
            TokenType.DIM -> makeIdentifier(curr, false, DataType.DIM)
            TokenType.IDENTIFIER -> {
                tokens.consume()
                if (tokens.peek()?.tokenType == TokenType.LPARENS) parseShapeFunctionCall(curr, null)
                else makeIdentifier(curr, false, null, scopeId, false)
            }
            TokenType.INTLITERAL -> makeInt(curr)
            TokenType.WILDCARD -> makeWildcard(curr, null)
            TokenType.LSQUARE -> parseShapeLiteral()
            else -> throw STypeParsingError("Expected shape at ${curr.startIdx}, instead got ${curr.text}")
        }
    }

    private fun parseShapeFunctionCall(id: Token, returnType: DataType?): SFunctionCall {
        val arguments = parseArgumentList() ?: throw STypeParsingError("Attempted to parse shape function call but could not find invocation.")
        return SFunctionCall(
            FunctionIdentifier(id.text, id.startIdx, id.endIdx),
            arguments,
            returnType,
            id.startIdx,
            arguments.endIdx
        )
    }

    private fun parseShapeLiteral(): ShapeLiteral? {
        var curr: Token? = tokens.consume() ?: return null
        val start = curr!!.startIdx
        val dims = mutableListOf<STypeNode>()
        if (curr.tokenType != TokenType.LSQUARE) throw STypeParsingError("Expected shape literal beginning with [")
        var isClosed = false

        if (tokens.peek()?.tokenType == TokenType.RSQUARE) {
            val endIdx = tokens.consume()!!.endIdx
            return ShapeLiteral(emptyList(), start, endIdx)
        }

        while (curr?.tokenType != TokenType.RSQUARE) {
            val dim = parseDimOrShape() ?: throw STypeParsingError("Could not process argument at ${curr?.startIdx}")

            // TODO: If we can resolve everything, this should be changed to (dim.dataType == DataType.DIM) or equivalent
            if (dim.dataType != DataType.SHAPE) dims.add(dim)
            else throw STypeParsingError("Invalid argument for dimensions at ${dim.startIdx}")

            val next = tokens.peek()
            curr = when (next?.tokenType) {
                TokenType.SEP -> {
                    tokens.consume()
                    tokens.peek()
                }
                TokenType.RSQUARE-> {
                    isClosed = true
                    tokens.consume()
                }
                null -> throw STypeParsingError("Unmatched left bracket at $start")
                else -> throw STypeParsingError("Expected comma or ], instead got ${next.text} at ${next.startIdx}")
            }
        }

        if (!isClosed) throw STypeParsingError("Unmatched left bracket at $start")

        return ShapeLiteral(dims, start, curr.endIdx)
    }

    // Utils
    // Mostly to abstract the index-tracking for common node types

    private fun makeInt(token: Token, consumeOnParse: Boolean = true) =
        IntLiteral(token.text.toInt(), token.startIdx, token.endIdx).also {
            if (consumeOnParse) tokens.consume()
        }

    private fun makeIdentifier(
        token: Token,
        isNewDeclaration: Boolean,
        dataType: DataType?,
        scopeId: Int? = null,
        consumeOnParse: Boolean = true
    ): STypeIdentifier {
        return if (isNewDeclaration) {
            STypeIdentifier(token.text, dataType, token.startIdx, token.endIdx, null, false, scopeId)
        } else {
            val existingDec = lookupInScopeAndParents(token.text)
            val lookupType = dataType ?: existingDec?.dataType
            STypeIdentifier(token.text, lookupType, token.startIdx, token.endIdx, existingDec?.upperBound, existingDec?.isStrictBound ?: false, existingDec?.scopeId)
        }.also {
            if (consumeOnParse) tokens.consume()
        }
    }

    private fun retypeIdentifier(
        id: STypeIdentifier,
        newDataType: DataType?,
        upperBound: STypeNode?,
        isStrictBound: Boolean,
        newScopeId: Int?
    ) = STypeIdentifier(id.name, newDataType, id.startIdx, id.endIdx, upperBound ?: id.upperBound, isStrictBound, newScopeId)

    private fun makeWildcard(token: Token, dataType: DataType?, consumeOnParse: Boolean = true) =
        Wildcard(dataType, token.startIdx, token.endIdx).also {
            if (consumeOnParse) tokens.consume()
        }

    private fun lookupInScopeAndParents(id: String): STypeIdentifier? =
        allDeclarationInfo.firstNotNullOfOrNull { it.lookupInScope(id) }?.id

    companion object {
        fun parseSTypeArguments(
            text: String,
            containingDeclaration: DeclarationDescriptor?,
            sTypeDeclarations: List<DeclarationInfo>
        ): ArgumentList {
            val lexStream = STypeLexer(text, ParseMode.ARGUMENT).lex()
            return Parser(lexStream, ParseMode.ARGUMENT, sTypeDeclarations, containingDeclaration.hashCode()).parse()
                    as ArgumentList
        }

        fun parseSTypeDeclarations(
            text: String,
            containingDeclaration: DeclarationDescriptor?,
            sTypeDeclarations: List<DeclarationInfo>
        ): DeclarationList {
            val lexStream = STypeLexer(text, ParseMode.DECLARATION).lex()
            return Parser(lexStream, ParseMode.DECLARATION, sTypeDeclarations, containingDeclaration.hashCode()).parse()
                    as DeclarationList
        }
    }
}