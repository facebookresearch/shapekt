/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin

import com.google.auto.service.AutoService
import config.BuildConfig
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor

@AutoService(CommandLineProcessor::class)
class ShapeTypingCommandLineProcessor : CommandLineProcessor {

  override val pluginId: String = BuildConfig.PLUGIN_ID

  override val pluginOptions: Collection<CliOption> = listOf()

}
