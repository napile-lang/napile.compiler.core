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

package org.napile.compiler.lang.resolve.scopes;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ClassifierDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.PackageDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import com.google.common.collect.Sets;

/**
 * @author abreslav
 */
public class ChainedScope implements NapileScope
{
	private final DeclarationDescriptor containingDeclaration;
	private final NapileScope[] scopeChain;
	private Collection<DeclarationDescriptor> allDescriptors;

	public ChainedScope(DeclarationDescriptor containingDeclaration, NapileScope... scopes)
	{
		this.containingDeclaration = containingDeclaration;
		scopeChain = scopes.clone();
	}

	@Override
	public ClassDescriptor getClass(@NotNull FqName fqName)
	{
		for(NapileScope scope : scopeChain)
		{
			ClassDescriptor classifier = scope.getClass(fqName);
			if(classifier != null)
				return classifier;
		}
		return null;
	}

	@Override
	public ClassifierDescriptor getClassifier(@NotNull Name name)
	{
		for(NapileScope scope : scopeChain)
		{
			ClassifierDescriptor classifier = scope.getClassifier(name);
			if(classifier != null)
				return classifier;
		}
		return null;
	}

	@Override
	public ClassDescriptor getObjectDescriptor(@NotNull Name name)
	{
		for(NapileScope scope : scopeChain)
		{
			ClassDescriptor objectDescriptor = scope.getObjectDescriptor(name);
			if(objectDescriptor != null)
				return objectDescriptor;
		}
		return null;
	}

	@NotNull
	@Override
	public Set<ClassDescriptor> getObjectDescriptors()
	{
		Set<ClassDescriptor> objectDescriptors = Sets.newHashSet();
		for(NapileScope scope : scopeChain)
		{
			objectDescriptors.addAll(scope.getObjectDescriptors());
		}
		return objectDescriptors;
	}

	@Override
	public PackageDescriptor getPackage(@NotNull Name name)
	{
		for(NapileScope napileScope : scopeChain)
		{
			PackageDescriptor namespace = napileScope.getPackage(name);
			if(namespace != null)
			{
				return namespace;
			}
		}
		return null;
	}

	@NotNull
	@Override
	public Set<VariableDescriptor> getVariables(@NotNull Name name)
	{
		Set<VariableDescriptor> properties = Sets.newLinkedHashSet();
		for(NapileScope napileScope : scopeChain)
		{
			properties.addAll(napileScope.getVariables(name));
		}
		return properties;
	}

	@Override
	public VariableDescriptor getLocalVariable(@NotNull Name name)
	{
		for(NapileScope napileScope : scopeChain)
		{
			VariableDescriptor variable = napileScope.getLocalVariable(name);
			if(variable != null)
			{
				return variable;
			}
		}
		return null;
	}

	@NotNull
	@Override
	public Set<MethodDescriptor> getMethods(@NotNull Name name)
	{
		if(scopeChain.length == 0)
		{
			return Collections.emptySet();
		}

		Set<MethodDescriptor> result = Sets.newLinkedHashSet();
		for(NapileScope napileScope : scopeChain)
		{
			result.addAll(napileScope.getMethods(name));
		}
		return result;
	}

	@NotNull
	@Override
	public ReceiverDescriptor getImplicitReceiver()
	{
		throw new UnsupportedOperationException(); // TODO
	}

	@Override
	public void getImplicitReceiversHierarchy(@NotNull List<ReceiverDescriptor> result)
	{
		for(NapileScope napileScope : scopeChain)
		{
			napileScope.getImplicitReceiversHierarchy(result);
		}
	}

	@NotNull
	@Override
	public DeclarationDescriptor getContainingDeclaration()
	{
		return containingDeclaration;
	}

	@NotNull
	@Override
	public Collection<DeclarationDescriptor> getAllDescriptors()
	{
		if(allDescriptors == null)
		{
			allDescriptors = Sets.newHashSet();
			for(NapileScope scope : scopeChain)
			{
				allDescriptors.addAll(scope.getAllDescriptors());
			}
		}
		return allDescriptors;
	}

	@NotNull
	@Override
	public Collection<DeclarationDescriptor> getOwnDeclaredDescriptors()
	{
		throw new UnsupportedOperationException();
	}
}
