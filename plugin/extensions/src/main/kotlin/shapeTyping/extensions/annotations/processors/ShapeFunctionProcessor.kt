/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.extensions.annotations.processors

import shapeTyping.extensions.ShapeFunctionExtension
import shapeTyping.extensions.annotations.ShapeFunction
import com.google.auto.service.AutoService
import com.squareup.javapoet.*
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import shapeTyping.analysis.SType
import java.util.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.lang.model.type.TypeMirror
import javax.tools.StandardLocation

@AutoService(Processor::class)
@SupportedAnnotationTypes("shapeTyping.extensions.annotations.ShapeFunction")
class ShapeFunctionProcessor: AbstractProcessor() {
    companion object {
        private const val ANALYSIS_PACKAGE = "shapeTyping.analysis"

        val LIST: ClassName = ClassName.get(List::class.java)
        val PAIR: ClassName = ClassName.get(Pair::class.java)

        val SHAPE_OR_DIM: ClassName = ClassName.get(SType::class.java)
        val SHAPE_FUNCTION_EXTENSION: ClassName = ClassName.get(ShapeFunctionExtension::class.java)
    }

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.RELEASE_8

    // Set of fq names of generated extensions so we can generate a plugin.xml file.
    private val generatedExtensions: MutableSet<String> = mutableSetOf()

    private fun TypeSpec.generateFile(packageName: String) {
        val classBuilder = this
        val file = JavaFile.builder(packageName, classBuilder).build()
        file.writeTo(processingEnv.filer)
    }

    private fun generatePluginConfig() {
        // These options are passed in through a kapt gradle configuration.
        val generateIntelliJPlugin = processingEnv.options["generateIntelliJPlugin"]
        if (!generateIntelliJPlugin.toBoolean()) return

        val name = processingEnv.options["name"]
        val description = processingEnv.options["description"]
        val vendor = processingEnv.options["vendor"]
        val id = processingEnv.options["id"]

        val implementations = generatedExtensions.map { fqName ->
            "<shapeFunction implementation=\"$fqName\"/>"
        }.joinToString("")
        val writer = processingEnv.filer.createResource(
                StandardLocation.CLASS_OUTPUT, "", "META-INF/plugin.xml").openWriter()
        writer.write(
                """
                    <!--THIS FILE IS GENERATED-->
                    <idea-plugin>
                        <id>$id</id>
                        <name>$name</name>
                        <vendor>$vendor</vendor>
                        <description>
                            $description
                        </description>
                        <depends>com.intellij.modules.platform</depends>
                        <depends>shapeTyping</depends>
                        <extensions defaultExtensionNs="shapeTyping">
                         $implementations
                        </extensions>
                        <idea-version since-build="201.*" until-build="212.*"/>
                    </idea-plugin>
                """.trimIndent()
        )
        writer.close()
    }

    private fun TypeMirror.isShapeOrDim(): Boolean {
        val className = ClassName.bestGuess(this.toString())
        // We assume that all classes in ANALYSIS_PACKAGE are ShapeOrDim and its subtypes.
        return className.packageName() == ANALYSIS_PACKAGE
    }

    private fun checkMethod(method: ExecutableElement) {
        require(method.returnType.isShapeOrDim())
        require(method.parameters.all { it.asType().isShapeOrDim() })
    }

    private fun generatedFileName(method: ExecutableElement) =
            "ShapeFunctionExtension_${method.simpleName}"

    private fun generatedFileFqName(method: ExecutableElement) =
            "${method.getPackageName()}.${generatedFileName(method)}"

    private fun Element.getPackageName(): String =
            processingEnv.elementUtils.getPackageOf(this).qualifiedName.toString()

    private fun ClassName.of(vararg typeArgument: TypeName) = ParameterizedTypeName.get(this, *typeArgument)

    private fun TypeSpec.Builder.addGetReturnTypeMethod(method: ExecutableElement) {
        this.addMethod(
                MethodSpec.methodBuilder("getReturnType")
                        .returns(ClassName.get(Class::class.java).of(WildcardTypeName.subtypeOf(SHAPE_OR_DIM)))
                        .addAnnotation(NotNull::class.java)
                        .addAnnotation(Override::class.java)
                        .addModifiers(Modifier.PUBLIC)
                        .addStatement("return \$L.class", method.returnType)
                        .build()
        )
    }

    private fun TypeSpec.Builder.addGetName(method: ExecutableElement) {
        this.addMethod(
                MethodSpec.methodBuilder("getName")
                        .returns(ClassName.get(String::class.java))
                        .addAnnotation(NotNull::class.java)
                        .addAnnotation(Override::class.java)
                        .addModifiers(Modifier.PUBLIC)
                        .addStatement("return \"${method.simpleName}\"")
                        .build()
        )
    }

    private fun TypeSpec.Builder.addGetParametersMethod(method: ExecutableElement) {
        val returnType = LIST.of(
                PAIR.of(
                        ClassName.get(String::class.java),
                        ClassName.get(Class::class.java).of(WildcardTypeName.subtypeOf(SHAPE_OR_DIM))
                )
        )

        val pairs = CodeBlock.builder().let { builder ->
            val params = method.parameters
            val formatString = params.joinToString(", ") { "new \$T(\$S, \$T.class)" }
            val formatInputs = params.flatMap { listOf(Pair::class.java, it.simpleName, it.asType()) }.toTypedArray()
            builder.add(formatString, *formatInputs).build()
        }

        this.addMethod(
                MethodSpec.methodBuilder("getParameters")
                        .returns(returnType)
                        .addAnnotation(NotNull::class.java)
                        .addAnnotation(Override::class.java)
                        .addModifiers(Modifier.PUBLIC)
                        .addStatement("return \$T.asList(\$L)", Arrays::class.java, pairs)
                        .build()
        )
    }

    private fun TypeSpec.Builder.addApplyMethod(method: ExecutableElement) {
        val params = method.parameters.also { require(it.all { param -> param.kind == ElementKind.PARAMETER }) }
        val returnType = ClassName.bestGuess(method.returnType.toString())

        fun MethodSpec.Builder.addBody(): MethodSpec.Builder {
            val methodBuilder = this@addBody

            // Initial check
            methodBuilder.addStatement("if (inputs.size() != \$L) throw new IllegalArgumentException()", params.size)

            // Cast each parameter to the correct type
            params.forEachIndexed { i, param ->
                this
                        .addStatement("\$T \$L = (\$T) inputs.get(\$L)", param.asType(), param.simpleName, param.asType(), i)
            }

            // Return statement
            val originalMethodName = "${method.enclosingElement.simpleName}.${method.simpleName}"
            val paramNames = params.map { it.simpleName }
            this.addStatement("return ${originalMethodName}(${paramNames.joinToString(", ")})")
            return methodBuilder
        }

        this.addMethod(
                MethodSpec.methodBuilder("apply")
                        .returns(returnType)
                        .addAnnotation(Nullable::class.java)
                        .addAnnotation(Override::class.java)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(
                                ParameterSpec.builder(LIST.of(WildcardTypeName.subtypeOf(SHAPE_OR_DIM)),
                                        "inputs")
                                        .addAnnotation(NotNull::class.java)
                                        .build()
                        )
                        .addBody()
                        .build()
        )

    }
    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        roundEnv?.let { env ->
            if (env.processingOver()) {
                generatePluginConfig()
                return true
            }
            env.getElementsAnnotatedWith(ShapeFunction::class.java).filterIsInstance<ExecutableElement>()
                    .forEach { method: ExecutableElement ->
                        checkMethod(method)

                        val fileName = generatedFileName(method)

                        val returnType = ClassName.bestGuess(method.returnType.toString())

                        val classBuilder = TypeSpec.classBuilder(fileName)

                        // @AutoService(ShapeFunctionExtension.class)
                        classBuilder.addAnnotation(AnnotationSpec.builder(AutoService::class.java)
                                .addMember("value", "\$T.class", SHAPE_FUNCTION_EXTENSION).build()
                        )

                        // public class <file-name> implements ShapeFunctionExtension<<return-type>>
                        classBuilder.addModifiers(Modifier.PUBLIC)
                        classBuilder.addSuperinterface(
                                SHAPE_FUNCTION_EXTENSION.of(returnType)
                        )

                        //  @NotNull
                        //  @Override
                        //  public String getName() {
                        //    return "<method-name>";
                        //  }
                        classBuilder.addGetName(method)

                        classBuilder.addGetReturnTypeMethod(method)

                        //  @NotNull
                        //  @Override
                        //  public List<Pair<String, Class<? extends SType>>> getParameters() { ... }
                        classBuilder.addGetParametersMethod(method)

                        // @NotNull
                        // @Override
                        // public <return-type> apply(@NotNull List<? extends SType> inputs) { ... }
                        classBuilder.addApplyMethod(method)

                        // Generate file
                        classBuilder.build().generateFile(method.getPackageName())

                        generatedExtensions.add(generatedFileFqName(method))
                    }
        }
        return true
    }
}
