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
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.DescriptorWithParent;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ClassifierDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.PackageDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;

/**
 * @author abreslav
 */
public interface NapileScope extends DescriptorWithParent
{
	NapileScope EMPTY = new NapileScopeImpl()
	{
		@NotNull
		@Override
		public DeclarationDescriptor getContainingDeclaration()
		{
			throw new UnsupportedOperationException("Don't take containing declaration of the EMPTY scope");
		}

		@Override
		public String toString()
		{
			return "EMPTY";
		}
	};

	@Nullable
	ClassDescriptor getClass(@NotNull FqName fqName);

	@Nullable
	ClassifierDescriptor getClassifier(@NotNull Name name);

	@Nullable
	ClassDescriptor getObjectDescriptor(@NotNull Name name);

	@NotNull
	Collection<ClassDescriptor> getObjectDescriptors();

	@Nullable
	PackageDescriptor getPackage(@NotNull Name name);

	@NotNull
	Collection<VariableDescriptor> getVariables(@NotNull Name name);

	@NotNull
	Collection<MethodDescriptor> getMethods(@NotNull Name name);

	@Nullable
	VariableDescriptor getLocalVariable(@NotNull Name name);

	@Override
	@NotNull
	DeclarationDescriptor getContainingDeclaration();


	/**
	 * All visible descriptors from current scope.
	 *
	 * @return All visible descriptors from current scope.
	 */
	@NotNull
	Collection<DeclarationDescriptor> getAllDescriptors();

	/**
	 * @return EFFECTIVE implicit receiver at this point (may be corresponding to an outer scope)
	 */
	@NotNull
	ReceiverDescriptor getImplicitReceiver();

	/**
	 * Adds receivers to the list in order of locality, so that the closest (the most local) receiver goes first
	 *
	 * @param result
	 */
	void getImplicitReceiversHierarchy(@NotNull List<ReceiverDescriptor> result);

	@NotNull
	Collection<DeclarationDescriptor> getOwnDeclaredDescriptors();
}
