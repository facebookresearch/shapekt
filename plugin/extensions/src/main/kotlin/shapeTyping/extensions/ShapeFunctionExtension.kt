/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.extensions

import shapeTyping.analysis.SType
import com.intellij.openapi.extensions.ExtensionPointName

/**
 * Extension for defining shape functions.
 */
interface ShapeFunctionExtension<T: SType>: ShapeTypingExtension {
    /**
     * The name of the shape function.
     */
    val name: String

    /**
     * The ordered list of pairs of parameter name to parameter type.
     */
    val parameters: List<Pair<String, Class<out SType>>>

    /**
     * The type returned by [apply].
     */
    val returnType: Class<out SType>

    /**
     * Shape function implementation.
     *
     * Returns null if there are no possible reductions/simplifications to the shape function with the given inputs.
     */
    fun apply(inputs: List<SType>): T?

    override fun registerExtension() {
        if (extensions.containsKey(this.name))
            throw Exception("Duplicate shape function name found: ${this.name}. Please use unique names.")
        // Add this extension to the map of extensions that will be consumed by the plugin.
        extensions[this.name] = this
    }

    companion object {
        // Extension point name for IntelliJ IDEA plugin extensions.
        private val EP_NAME: ExtensionPointName<ShapeFunctionExtension<*>> =
                ExtensionPointName.create("shapeTyping.shapeFunction")

        // Used by the compiler plugin to obtain the available extension implementations.
        val extensions: MutableMap<String, ShapeFunctionExtension<*>> =
                try {
                    // Get IntelliJ IDEA plugin extensions if they exist.
                    EP_NAME.extensionList.associateBy { ext -> ext.name }.toMutableMap()
                } catch (_: Exception) {
                    mutableMapOf()
                }
    }
}
