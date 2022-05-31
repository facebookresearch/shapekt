/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin.extensions

import shapeTyping.extensions.ShapeTypingExtension
import org.jetbrains.kotlin.cli.jvm.plugins.ServiceLoaderLite
import java.net.URLClassLoader
import kotlin.reflect.KClass

object ExtensionLoader {
    fun <T: ShapeTypingExtension> load(klass: KClass<T>, urlClassLoader: URLClassLoader) {
        val implementations = ServiceLoaderLite.loadImplementations(klass.java, urlClassLoader)
        implementations.forEach { it.registerExtension() }
    }
}
