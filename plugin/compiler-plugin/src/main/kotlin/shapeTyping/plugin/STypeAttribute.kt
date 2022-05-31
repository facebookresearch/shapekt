/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin

import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeAttribute
import shapeTyping.analysis.*
import shapeTyping.plugin.checkers.utils.AttributesUtils.union
import kotlin.reflect.KClass

class STypeAttribute(val annotationType: KotlinType, val sType: SType) : TypeAttribute<STypeAttribute>() {
    // TODO: ADD SHAPE AND OTHER METADATA HERE.

    override val key: KClass<out STypeAttribute>
        get() = STypeAttribute::class

    override fun union(other: STypeAttribute?): STypeAttribute? {
        if (other == null) return null
        val sTypeUnion = this.sType.union(other.sType) ?: return null
        return STypeAttribute(this.annotationType, sTypeUnion)
    }

    override fun add(other: STypeAttribute?): STypeAttribute {
        // Used in cases where there are multiple SData annotations for a type.
        // Usually, multiple annotations are not allowed, but in the case of TypeAliases, this may occur.
        // TODO: Probably can just pick the most specific shape for now and throw an exception if there is a conflicting shape.
        return this
    }

    override fun intersect(other: STypeAttribute?): STypeAttribute? {
        // Produce Greatest Lower Bound (Largest common subtype)
        // Can hold off on this implementation because we really only need union for most cases
        // and occasionally add.
        return null
    }

    // I think Dmitriy will want us to eventually not have this method. It is not currently used anywhere,
    // so we just return true as a placeholder.
    override fun isSubtypeOf(other: STypeAttribute?): Boolean = true

}