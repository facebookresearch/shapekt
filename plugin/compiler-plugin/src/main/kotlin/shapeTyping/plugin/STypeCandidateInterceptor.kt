/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.extensions.internal.CallResolutionInterceptorExtension
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallDiagnostic
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCallAtom
import org.jetbrains.kotlin.resolve.calls.tower.psiKotlinCall
import org.jetbrains.kotlin.resolve.descriptorUtil.isAnnotationConstructor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.expressions.ControlStructureTypingUtils
import shapeTyping.analysis.*
import shapeTyping.analysis.exceptions.STypeErrorPropagationException
import shapeTyping.analysis.utils.substitute
import shapeTyping.plugin.ShapeTypingAnnotationFqNames.STYPE_FQNAME
import shapeTyping.plugin.analysis.WritableSlices
import shapeTyping.plugin.checkers.utils.*
import shapeTyping.plugin.checkers.utils.AttributesUtils.getDefaultSType

@Suppress("INVISIBLE_REFERENCE", "EXPERIMENTAL_IS_NOT_ENABLED", "IllegalExperimentalApiUsage")
@OptIn(org.jetbrains.kotlin.extensions.internal.InternalNonStableExtensionPoints::class)
class STypeCandidateInterceptor : CallResolutionInterceptorExtension {

    private fun interceptCallReturnType(
        returnType: KotlinType,
        substitutionMap: Map<SType, SType>,
        propagationError: Boolean
    ): KotlinType {
        return returnType.substituteAndEvaluateSTypeAttribute(substitutionMap, propagationError)
    }

    private fun substituteValueParameterTypeAttribute(
        param: ValueParameterDescriptor,
        substitutionMap: Map<SType, SType>
    ): ValueParameterDescriptor {
        val newParamType = param.type.substituteAndEvaluateSTypeAttribute(substitutionMap, false)
        return ValueParameterDescriptorImpl(
            containingDeclaration = param.containingDeclaration,
            original = param.original,
            index = param.index,
            annotations = param.annotations,
            name = param.name,
            outType = newParamType,
            declaresDefaultValue = param.declaresDefaultValue(),
            isNoinline = param.isNoinline,
            isCrossinline = param.isCrossinline,
            varargElementType = param.varargElementType,
            source = param.source
        )
    }

    private fun replaceConstructorType(
        candidateDescriptor: ClassConstructorDescriptor,
        valueParameters: List<ValueParameterDescriptor>,
        returnType: KotlinType
    ): ClassConstructorDescriptor {
        return candidateDescriptor.newCopyBuilder()
            .setOriginal(candidateDescriptor.original)
            .setValueParameters(valueParameters)
            .setReturnType(returnType)
            .build()!! as ClassConstructorDescriptor
    }

    private fun substituteReceiverParameterTypeAttribute(
        param: ReceiverParameterDescriptor?,
        substitutionMap: Map<SType, SType>
    ): ReceiverParameterDescriptor? = param?.let {
        return ReceiverParameterDescriptorImpl(
            param.containingDeclaration,
            param.value.replaceType(param.type.substituteAndEvaluateSTypeAttribute(substitutionMap, false)),
            param.annotations
        )
    }

    private fun interceptClassConstructorCallCandidate(
        candidateDescriptor: ClassConstructorDescriptor,
        completedCallAtom: ResolvedCallAtom,
        trace: BindingTrace,
        resultSubstitutor: NewTypeSubstitutor?,
        context: BindingContext
    ): ClassConstructorDescriptor {
        // Return early if visiting an annotation constructor.
        if (candidateDescriptor.isAnnotationConstructor())
            return candidateDescriptor

        val call = completedCallAtom.atom.psiKotlinCall.psiCall
        val returnType = candidateDescriptor.returnType.let { tpe ->
            // Substitute type variables
            resultSubstitutor?.safeSubstitute(completedCallAtom.freshVariablesSubstitutor.safeSubstitute(tpe.unwrap()))
                ?: tpe
        }
        val annotationType = candidateDescriptor.constructedClass.annotations.findAnnotation(STYPE_FQNAME)?.type ?: return candidateDescriptor

        val declaredSType = candidateDescriptor.constructedClass.getDefaultSType() ?: return candidateDescriptor
        val (substitutionMap, subSuccess) = substitutionMapAndStatusFromArguments(candidateDescriptor, call, context)
        val sType = declaredSType.substitute(substitutionMap).let { substitutedType ->
            if (subSuccess) substitutedType else substitutedType.withException(STypeErrorPropagationException(null))
        }
        val newValueParameters = candidateDescriptor.valueParameters.map {
            substituteValueParameterTypeAttribute(it, substitutionMap)
        }
        val newReturnType = returnType.replaceSType(sType, annotationType)

        return replaceConstructorType(candidateDescriptor, newValueParameters, newReturnType)
    }

    private fun FunctionDescriptor.isSpecialConstruct(): Boolean {
        return enumValues<ControlStructureTypingUtils.ResolveConstruct>().any { name == it.specialFunctionName }
    }

    fun interceptFunctionCallCandidate(
        candidateDescriptor: FunctionDescriptor,
        completedCallAtom: ResolvedCallAtom,
        context: BindingContext,
        resultSubstitutor: NewTypeSubstitutor?,
    ): FunctionDescriptor {
        val call = completedCallAtom.atom.psiKotlinCall.psiCall
        val returnType = candidateDescriptor.substitutedReturnType(resultSubstitutor, completedCallAtom) ?:
            candidateDescriptor.returnType ?: return candidateDescriptor

        if (!returnType.containsSType()) return candidateDescriptor

        val (substitutions, subSuccess) = substitutionMapAndStatusFromArguments(candidateDescriptor, call, context)
        val newReturnType = interceptCallReturnType(returnType, substitutions, !subSuccess)
        val newValueParameters = candidateDescriptor.valueParameters.map {
            substituteValueParameterTypeAttribute(it, substitutions)
        }

        val desc = if (candidateDescriptor is DeserializedSimpleFunctionDescriptor) {
            DeserializedSimpleFunctionDescriptor(
                candidateDescriptor.containingDeclaration,
                candidateDescriptor.original,
                candidateDescriptor.annotations,
                candidateDescriptor.name,
                candidateDescriptor.kind,
                candidateDescriptor.proto,
                candidateDescriptor.nameResolver,
                candidateDescriptor.typeTable,
                candidateDescriptor.versionRequirementTable,
                candidateDescriptor.containerSource,
                candidateDescriptor.source
            ).initialize(
                substituteReceiverParameterTypeAttribute(candidateDescriptor.extensionReceiverParameter, substitutions),
                candidateDescriptor.dispatchReceiverParameter,
                candidateDescriptor.contextReceiverParameters,
                candidateDescriptor.typeParameters,
                newValueParameters,
                newReturnType,
                candidateDescriptor.modality,
                candidateDescriptor.visibility
            ).also {
                it.setFlagsFrom(candidateDescriptor)
            }
        } else {
            candidateDescriptor.newCopyBuilder()
                .setOriginal(candidateDescriptor.original)
                .setReturnType(newReturnType)
                .setValueParameters(newValueParameters)
                .setExtensionReceiverParameter(substituteReceiverParameterTypeAttribute(candidateDescriptor.extensionReceiverParameter, substitutions))
                .build()!!
        }
        return desc
    }

    fun interceptPropertyCallCandidate(
        candidateDescriptor: PropertyDescriptor,
        completedCallAtom: ResolvedCallAtom,
        context: BindingContext,
        resultSubstitutor: NewTypeSubstitutor?,
    ): PropertyDescriptor {
        val call = completedCallAtom.atom.psiKotlinCall.psiCall
        val (substitutions, subSuccess) = substitutionMapAndStatusFromArguments(candidateDescriptor, call, context)
        val substitutedReturnType = candidateDescriptor.substitutedReturnType(resultSubstitutor, completedCallAtom)
            ?: return candidateDescriptor
        val newReturnType = interceptCallReturnType(substitutedReturnType, substitutions, !subSuccess)
        if (newReturnType.getDefaultSType() == null) {
            return candidateDescriptor
        }

        return candidateDescriptor.newCopyBuilder()
            .setOriginal(candidateDescriptor.original)
            .setReturnType(newReturnType)
            .build()!!
    }

    private fun CallableDescriptor.substitutedReturnType(
        resultSubstitutor: NewTypeSubstitutor?,
        callAtom: ResolvedCallAtom
    ) = this.returnType?.let { type ->
        resultSubstitutor?.safeSubstitute(callAtom.freshVariablesSubstitutor.safeSubstitute(type.unwrap()))
    }

    override fun interceptResolvedCallAtomCandidate(
        candidateDescriptor: CallableDescriptor,
        completedCallAtom: ResolvedCallAtom,
        trace: BindingTrace?,
        resultSubstitutor: NewTypeSubstitutor?,
        diagnostics: Collection<KotlinCallDiagnostic>
    ): CallableDescriptor {

        // Return early if bindingContext does not exist.
        val context = trace?.bindingContext ?: return candidateDescriptor

        // TODO: Can we get the scope of the call from somewhere? Presently the scope of the function declaration matters,
        // but not the scope of the call.
        if (candidateDescriptor.noSType(trace)) return candidateDescriptor

        return when (candidateDescriptor) {
            is PropertyDescriptor -> interceptPropertyCallCandidate(candidateDescriptor, completedCallAtom, context, resultSubstitutor)
            is ClassConstructorDescriptor -> interceptClassConstructorCallCandidate(candidateDescriptor, completedCallAtom, trace, resultSubstitutor, context)
            is FunctionDescriptor ->
                if (candidateDescriptor.isSpecialConstruct()) candidateDescriptor else
                    interceptFunctionCallCandidate(candidateDescriptor, completedCallAtom, context, resultSubstitutor)
            else -> candidateDescriptor
        }
    }

    private fun DeclarationDescriptor.noSType(trace: BindingTrace): Boolean {
        // Return result if cached
        trace.get(WritableSlices.NOSTYPE_ON, this)?.let { return it }

        // Check for annotation
        if (this.annotations.hasAnnotation(ShapeTypingAnnotationFqNames.NO_STYPE)) {
            trace.record(WritableSlices.NOSTYPE_ON, this, true)
            return true
        }

        val resultFromParent = this.containingDeclaration?.noSType(trace) ?: false
        trace.record(WritableSlices.NOSTYPE_ON, this, resultFromParent)

        return resultFromParent
    }

}
