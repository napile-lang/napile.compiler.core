/*
 * Copyright 2010-2012 napile.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.napile.compiler.lang.resolve.processors.members;

import static org.napile.compiler.lang.diagnostics.Errors.FINAL_UPPER_BOUND;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.CallParameterDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptorImpl;
import org.napile.compiler.lang.descriptors.Visibility;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.psi.NapileCallParameterList;
import org.napile.compiler.lang.psi.NapilePsiUtil;
import org.napile.compiler.lang.psi.NapileTypeParameter;
import org.napile.compiler.lang.psi.NapileTypeParameterListOwner;
import org.napile.compiler.lang.psi.NapileTypeReference;
import org.napile.compiler.lang.resolve.BindingTraceKeys;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.TraceBasedRedeclarationHandler;
import org.napile.compiler.lang.resolve.processors.DescriptorResolver;
import org.napile.compiler.lang.resolve.processors.TypeResolver;
import org.napile.compiler.lang.resolve.scopes.NapileScope;
import org.napile.compiler.lang.resolve.scopes.WritableScope;
import org.napile.compiler.lang.resolve.scopes.WritableScopeImpl;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.lang.types.TypeUtils;
import org.napile.compiler.lang.types.checker.NapileTypeChecker;
import com.google.common.collect.Lists;
import com.intellij.openapi.util.Pair;

/**
 * @author VISTALL
 * @since 18:42/08.10.12
 */
public class TypeParameterResolver
{
	@NotNull
	private AnnotationResolver annotationResolver;

	@NotNull
	private TypeResolver typeResolver;

	@NotNull
	private DescriptorResolver descriptorResolver;

	@Inject
	public void setTypeResolver(@NotNull TypeResolver typeResolver)
	{
		this.typeResolver = typeResolver;
	}

	@Inject
	public void setAnnotationResolver(@NotNull AnnotationResolver annotationResolver)
	{
		this.annotationResolver = annotationResolver;
	}

	@Inject
	public void setDescriptorResolver(@NotNull DescriptorResolver descriptorResolver)
	{
		this.descriptorResolver = descriptorResolver;
	}

	public List<TypeParameterDescriptor> resolveTypeParameters(DeclarationDescriptor containingDescriptor, WritableScope extensibleScope, NapileTypeParameter[] typeParameters, BindingTrace trace)
	{
		List<TypeParameterDescriptor> result = new ArrayList<TypeParameterDescriptor>();
		for(int i = 0, typeParametersSize = typeParameters.length; i < typeParametersSize; i++)
		{
			NapileTypeParameter typeParameter = typeParameters[i];
			result.add(resolveTypeParameter(containingDescriptor, extensibleScope, typeParameter, i, trace));
		}
		return result;
	}

	private TypeParameterDescriptorImpl resolveTypeParameter(DeclarationDescriptor containingDescriptor, WritableScope extensibleScope, NapileTypeParameter typeParameter, int index, BindingTrace trace)
	{
		TypeParameterDescriptorImpl typeParameterDescriptor = new TypeParameterDescriptorImpl(containingDescriptor, annotationResolver.bindAnnotations(extensibleScope, typeParameter, trace), NapilePsiUtil.safeName(typeParameter.getName()), index);

		extensibleScope.addTypeParameterDescriptor(typeParameterDescriptor);
		trace.record(BindingTraceKeys.TYPE_PARAMETER, typeParameter, typeParameterDescriptor);
		return typeParameterDescriptor;
	}

	public void postResolving(@NotNull NapileTypeParameterListOwner declaration, @NotNull NapileScope scope, @NotNull List<TypeParameterDescriptor> parameters, @NotNull BindingTrace trace)
	{
		resolveGenericBounds(declaration, scope, parameters, trace);

		resolveConstructors(declaration, scope, parameters, trace);
	}

	private void resolveConstructors(@NotNull NapileTypeParameterListOwner declaration, NapileScope scope, List<TypeParameterDescriptor> parameters, BindingTrace trace)
	{
		NapileTypeParameter[] typeParameters = declaration.getTypeParameters();
		for(int i = 0; i < parameters.size(); i++)
		{
			NapileTypeParameter typeParameter = typeParameters[i];
			TypeParameterDescriptor typeParameterDescriptor = parameters.get(i);

			for(NapileCallParameterList parameterList : typeParameter.getConstructorParameterLists())
			{
				ConstructorDescriptor constructorDescriptor = new ConstructorDescriptor(typeParameterDescriptor, Collections.<AnnotationDescriptor>emptyList(), false);

				WritableScope innerScope = new WritableScopeImpl(scope, constructorDescriptor, new TraceBasedRedeclarationHandler(trace), "TPConstructor descriptor header scope");
				innerScope.changeLockLevel(WritableScope.LockLevel.BOTH);
				List<CallParameterDescriptor> parameterDescriptors = descriptorResolver.resolveCallParameters(constructorDescriptor, innerScope, parameterList.getParameters(), trace);

				//constructorDescriptor.setParametersScope(innerScope);
				constructorDescriptor.initialize(Collections.<TypeParameterDescriptor>emptyList(), parameterDescriptors, Visibility.PUBLIC);
				innerScope.changeLockLevel(WritableScope.LockLevel.READING);

				((TypeParameterDescriptorImpl) typeParameterDescriptor).addConstructor(constructorDescriptor);
			}
		}
	}

	private void resolveGenericBounds(@NotNull NapileTypeParameterListOwner declaration, NapileScope scope, List<TypeParameterDescriptor> parameters, BindingTrace trace)
	{
		List<Pair<NapileTypeReference, NapileType>> deferredUpperBoundCheckerTasks = Lists.newArrayList();

		NapileTypeParameter[] typeParameters = declaration.getTypeParameters();
		for(int i = 0; i < typeParameters.length; i++)
		{
			NapileTypeParameter jetTypeParameter = typeParameters[i];
			TypeParameterDescriptor typeParameterDescriptor = parameters.get(i);

			for(NapileTypeReference extendsBound : jetTypeParameter.getSuperTypes())
			{
				NapileType type = typeResolver.resolveType(scope, extendsBound, trace, false);
				((TypeParameterDescriptorImpl) typeParameterDescriptor).addUpperBound(type);
				deferredUpperBoundCheckerTasks.add(new Pair<NapileTypeReference, NapileType>(extendsBound, type));
			}
		}

		for(TypeParameterDescriptor typeParameterDescriptor : parameters)
		{
			((TypeParameterDescriptorImpl) typeParameterDescriptor).addDefaultUpperBound(scope);

			((TypeParameterDescriptorImpl) typeParameterDescriptor).setInitialized();
		}

		for(Pair<NapileTypeReference, NapileType> checkerTask : deferredUpperBoundCheckerTasks)
			checkUpperBoundType(checkerTask.getFirst(), checkerTask.getSecond(), trace);
	}

	private static void checkUpperBoundType(NapileTypeReference upperBound, NapileType upperBoundType, BindingTrace trace)
	{
		if(!TypeUtils.canHaveSubtypes(NapileTypeChecker.INSTANCE, upperBoundType))
		{
			trace.report(FINAL_UPPER_BOUND.on(upperBound, upperBoundType));
		}
	}
}
