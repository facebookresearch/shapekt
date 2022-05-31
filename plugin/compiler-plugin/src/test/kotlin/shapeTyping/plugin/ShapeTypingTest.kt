/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin

import shapeTyping.extensions.ShapeFunctionExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import org.junit.Before
import java.io.File
import java.net.URL
import java.net.URLClassLoader

interface ShapeTypingTest {
    @Before
    fun clearExtensions() {
        // Remove extensions to avoid duplicate extension errors when running multiple tests at once.
        ShapeFunctionExtension.extensions.clear()
    }

    companion object {

        // The below code is taken from the Google Compose plugin tests.
        // https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/compiler/compiler-hosted/integration-tests/src/test/java/androidx/compose/compiler/plugins/kotlin/AbstractComposeDiagnosticsTest.kt

        private const val TEST_MODULE_NAME = "test-module"
        private val defaultClassPath by lazy { systemClassLoaderJars() }

        private fun File.applyExistenceCheck(): File = this.apply {
            if (!exists()) throw NoSuchFileException(this)
        }

        private fun computeHomeDirectory(): String {
            val userDir = System.getProperty("user.dir")
            val dir = File(userDir ?: ".")
            return FileUtil.toCanonicalPath(dir.absolutePath)
        }

        init {
            val homeDir: String by lazy { File(computeHomeDirectory()).applyExistenceCheck().absolutePath }
            System.setProperty(
                "idea.home",
                homeDir
            )
        }

        private fun systemClassLoaderJars(): List<File> {
            val classpath = System.getProperty("java.class.path")!!.split(
                System.getProperty("path.separator")!!
            )
            val urls = classpath.map { URL("file://$it") }
            val result = URLClassLoader(urls.toTypedArray()).urLs?.filter {
                it.protocol == "file"
            }?.map {
                File(it.path)
            }?.toList() ?: emptyList()
            return result
        }

        private fun newConfiguration(): CompilerConfiguration {
            val configuration = CompilerConfiguration()
            configuration.put(
                CommonConfigurationKeys.MODULE_NAME,
                TEST_MODULE_NAME
            )

            configuration.put(
                CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
                object : MessageCollector {
                    override fun clear() {}

                    override fun report(
                        severity: CompilerMessageSeverity,
                        message: String,
                        location: CompilerMessageSourceLocation?
                    ) {
                        if (severity === CompilerMessageSeverity.ERROR) {
                            val prefix = if (location == null)
                                ""
                            else
                                "(" + location.path + ":" + location.line + ":" + location.column + ") "
                            throw AssertionError(prefix + message)
                        }
                    }

                    override fun hasErrors(): Boolean {
                        return false
                    }
                }
            )

            return configuration
        }

        private fun createClasspath() = defaultClassPath.filter {
            // TODO: Do we need this? Can we just use defaultClassPath?
            !it.path.contains("robolectric") && it.extension != "xml"
        }.toList()

        internal fun createEnvironment(): KotlinCoreEnvironment {
            val classPath = createClasspath()

            val configuration = newConfiguration()
            configuration.addJvmClasspathRoots(classPath)

            System.setProperty("idea.ignore.disabled.plugins", "true") // TODO: Do we need this?
            return KotlinCoreEnvironment.createForTests(
                { },
                configuration,
                EnvironmentConfigFiles.JVM_CONFIG_FILES
            )
        }

        internal fun createFile(name: String, text: String, project: Project): KtFile {
            var shortName = name.substring(name.lastIndexOf('/') + 1)
            shortName = shortName.substring(shortName.lastIndexOf('\\') + 1)
            val virtualFile = object : LightVirtualFile(
                shortName,
                KotlinLanguage.INSTANCE,
                StringUtilRt.convertLineSeparators(text)
            ) {
                override fun getPath(): String = "/$name"
            }

            virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET)
            val factory = PsiFileFactory.getInstance(project) as PsiFileFactoryImpl

            return factory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false) as KtFile
        }
    }
}
