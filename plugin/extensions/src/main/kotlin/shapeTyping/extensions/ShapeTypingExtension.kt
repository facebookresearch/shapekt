/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.extensions

interface ShapeTypingExtension {
    /**
     * [registerExtension] is called for each plugin loaded by the ExtensionLoader.
     *
     * It should contain any necessary initialization logic.
     */
    fun registerExtension()
}
