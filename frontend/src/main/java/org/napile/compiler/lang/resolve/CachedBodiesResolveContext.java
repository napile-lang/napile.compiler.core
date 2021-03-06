/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.napile.compiler.lang.resolve;

import java.util.Collections;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.MutableClassDescriptor;
import org.napile.compiler.lang.descriptors.PackageDescriptor;
import org.napile.compiler.lang.descriptors.SimpleMethodDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.psi.NapileAnonymClass;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileConstructor;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileEnumValue;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.psi.NapileNamedMethodOrMacro;
import org.napile.compiler.lang.psi.NapileVariable;
import org.napile.compiler.lang.resolve.scopes.NapileScope;

/**
 * A storage for the part of {@see TopDownAnalysisContext} collected during headers analysis that will be used during resolution of
 * bodies
 *
 * @author Nikolay Krasko
 */
public class CachedBodiesResolveContext implements BodiesResolveContext
{
	private final Map<NapileClass, MutableClassDescriptor> classes;
	private final Map<NapileAnonymClass, MutableClassDescriptor> objects;
	private final Map<NapileConstructor, ConstructorDescriptor> constructors;
	private final Map<NapileVariable, VariableDescriptor> variables;
	private final Map<NapileEnumValue, MutableClassDescriptor> enumValues;
	private final Map<NapileNamedMethodOrMacro, SimpleMethodDescriptor> methods;
	private final Map<NapileFile, PackageDescriptor> packages;
	private final Map<NapileDeclaration, NapileScope> declaringScopes;

	private
	@NotNull
	TopDownAnalysisParameters topDownAnalysisParameters;

	public CachedBodiesResolveContext(TopDownAnalysisContext context)
	{
		classes = Collections.unmodifiableMap(context.getClasses());
		objects = Collections.unmodifiableMap(context.getAnonymous());
		constructors = Collections.unmodifiableMap(context.getConstructors());
		variables = Collections.unmodifiableMap(context.getVariables());
		enumValues = Collections.unmodifiableMap(context.getEnumValues());
		methods = Collections.unmodifiableMap(context.getMethods());
		packages = Collections.unmodifiableMap(context.getPackages());
		declaringScopes = Collections.unmodifiableMap(context.getDeclaringScopes());

		topDownAnalysisParameters = context.getTopDownAnalysisParameters();
	}

	@Override
	public Map<NapileClass, MutableClassDescriptor> getClasses()
	{
		return classes;
	}

	@Override
	public Map<NapileFile, PackageDescriptor> getPackages()
	{
		return packages;
	}

	@Override
	public Map<NapileAnonymClass, MutableClassDescriptor> getAnonymous()
	{
		return objects;
	}

	@Override
	public Map<NapileConstructor, ConstructorDescriptor> getConstructors()
	{
		return constructors;
	}

	@Override
	public Map<NapileVariable, VariableDescriptor> getVariables()
	{
		return variables;
	}

	@Override
	public Map<NapileEnumValue, MutableClassDescriptor> getEnumValues()
	{
		return enumValues;
	}

	@Override
	public Map<NapileNamedMethodOrMacro, SimpleMethodDescriptor> getMethods()
	{
		return methods;
	}

	@Override
	public Map<NapileDeclaration, NapileScope> getDeclaringScopes()
	{
		return declaringScopes;
	}

	@Override
	public void setTopDownAnalysisParameters(@NotNull TopDownAnalysisParameters parameters)
	{
		topDownAnalysisParameters = parameters;
	}

	@Override
	public boolean completeAnalysisNeeded(@NotNull NapileElement element)
	{
		NapileFile containingFile = element.getContainingFile();
		return containingFile != null && topDownAnalysisParameters.getAnalyzeCompletely().apply(containingFile);
	}

}
