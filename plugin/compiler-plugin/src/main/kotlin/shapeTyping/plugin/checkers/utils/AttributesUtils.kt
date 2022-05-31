/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin.checkers.utils

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.parentsWithSelf
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeAttributes
import shapeTyping.analysis.*
import shapeTyping.plugin.STypeAttribute
import shapeTyping.plugin.ShapeTypingAnnotationFqNames
import shapeTyping.analysis.utils.STypeSubtyper.isSubtypeOf
import shapeTyping.plugin.synthetic.parsing.Parser
import shapeTyping.plugin.synthetic.parsing.parseAndCheckDeclarations
import shapeTyping.plugin.synthetic.parsing.toSType

object AttributesUtils {
    fun TypeAttributes.getSTypeAttribute(): STypeAttribute? = this.filterIsInstance<STypeAttribute>().firstOrNull()
    fun TypeAttributes.getSType(): SType? = this.getSTypeAttribute()?.sType
    fun KotlinType.getSTypeAttribute(): STypeAttribute? = this.attributes.getSTypeAttribute()
    fun KotlinType.getSType(): SType? = this.attributes.getSType()

    fun KotlinType.getDefaultSType(): SType? {
        this.attributes.getSType()?.let { return it }
        if (this is SimpleType) {
            val desc = (this.constructor.declarationDescriptor as? ClassDescriptor) ?: return null
            return desc.getDefaultSType()
        }
        return null
    }

    fun ClassDescriptor.getDefaultSType(): SType? {
        val sDataAnnotation = this.annotations.findAnnotation(ShapeTypingAnnotationFqNames.STYPE_FQNAME)
            ?: return null
        val string = sDataAnnotation.allValueArguments.values.first().value as String
        val sTypeDeclarations = this.parentsWithSelf.toList().map {
            it.annotations.findAnnotation(ShapeTypingAnnotationFqNames.STYPE_FQNAME)?.let { annotation ->
                parseAndCheckDeclarations(it, annotation, emptyList())
            }
        }.filterNotNull()
        return if (sTypeDeclarations.isEmpty()) null else
            Parser.parseSTypeDeclarations(string, this, sTypeDeclarations).toSType()
    }


    fun SType.union(other: SType): SType? {
        return when {
            this.isSubtypeOf(other) -> other
            other.isSubtypeOf(this) -> this
            else -> when {
                this is Shape && other is Shape -> this.shapeUnion(other)
                this is Dim && other is Dim -> this.dimUnion(other)
                this is STypeTuple && other is STypeTuple -> {
                    val types = this.types.zip(other.types).map { it.first.union(it.second) }
                    if (types.contains(null)) null else STypeTuple(types as List<SType>)
                }
                else -> null
            }
        }
    }

    private fun Dim.dimUnion(other: Dim): Dim =
        when (this) {
            is WildcardDim -> WildcardDim.DEFAULT
            is ErrorDim -> if (other.isError()) this else WildcardDim.DEFAULT // TODO: Combine exceptions
            is NumericDim -> when (other) {
                is WildcardDim, is ErrorDim -> WildcardDim.DEFAULT
                is NumericDim -> if (this == other) this else WildcardDim.DEFAULT
                is SymbolicDim -> other.declarationInfo.upperBound.dimUnion(this)
            }
            is SymbolicDim -> when (other) {
                is WildcardDim, is ErrorDim -> WildcardDim.DEFAULT
                is NumericDim -> this.declarationInfo.upperBound.dimUnion(other)
                is SymbolicDim -> this.symbolUnion(other)
            }
        }

    private fun Shape.shapeUnion(other: Shape): Shape =
        when (this) {
            is WildcardShape, is ShapeFunctionCall -> WildcardShape.DEFAULT
            is ErrorShape -> if (other.isError()) this else WildcardShape.DEFAULT // TODO: Combine exceptions
            is DimShape -> when (other) {
                is WildcardShape, is ShapeFunctionCall, is ErrorShape -> WildcardShape.DEFAULT
                is DimShape ->
                    if (this.rank == other.rank)
                        DimShape(this.dims.zip(other.dims).map { it.first.dimUnion(it.second) })
                    else WildcardShape.DEFAULT
                is SymbolicShape -> other.declarationInfo.upperBound.shapeUnion(this)
            }
            is SymbolicShape -> when (other) {
                is WildcardShape, is ShapeFunctionCall, is ErrorShape -> WildcardShape.DEFAULT
                is SymbolicShape -> this.symbolUnion(other)
                is DimShape -> this.declarationInfo.upperBound.shapeUnion(other)
            }
        }

    private fun SymbolicShape.symbolUnion(other: SymbolicShape): Shape =
        when {
            this.isSubtypeOf(other) -> other
            other.isSubtypeOf(this) -> this
            else -> {
                val boundsForThis = allBounds()
                val boundsForOther = other.allBounds()
                val boundSet = boundsForThis.toSet()
                val firstMatch = boundsForOther.firstOrNull { it in boundSet }
                // If no match is found, compare the final bounds. These are guaranteed non-symbolic.
                firstMatch ?: allBounds().last().shapeUnion(other.allBounds().last())
            }
        }

    private fun SymbolicDim.symbolUnion(other: SymbolicDim) : Dim =
        when {
            this.isSubtypeOf(other) -> other
            other.isSubtypeOf(this) -> this
            else -> {
                val boundsForThis = allBounds()
                val boundsForOther = other.allBounds()
                val boundSet = boundsForThis.toSet()
                val firstMatch = boundsForOther.firstOrNull { it in boundSet }
                // If no match is found, compare the final bounds. These are guaranteed non-symbolic.
                firstMatch ?: allBounds().last().dimUnion(other.allBounds().last())
            }
        }

}