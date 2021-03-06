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
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ClassKind;
import org.napile.compiler.lang.descriptors.ClassifierDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.resolve.AbstractScopeAdapter;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

/**
 * @author svtk
 */
public class InnerClassesScopeWrapper extends AbstractScopeAdapter
{
	private final NapileScope actualScope;

	public InnerClassesScopeWrapper(NapileScope actualScope)
	{
		this.actualScope = actualScope;
	}

	@NotNull
	@Override
	protected NapileScope getWorkerScope()
	{
		return actualScope;
	}

	private boolean isClass(DeclarationDescriptor descriptor)
	{
		return descriptor instanceof ClassDescriptor && ((ClassDescriptor) descriptor).getKind() != ClassKind.ANONYM_CLASS;
	}

	@Override
	public ClassifierDescriptor getClassifier(@NotNull Name name)
	{
		ClassifierDescriptor classifier = actualScope.getClassifier(name);
		if(isClass(classifier))
			return classifier;
		return null;
	}

	@NotNull
	@Override
	public Collection<DeclarationDescriptor> getAllDescriptors()
	{
		Collection<DeclarationDescriptor> allDescriptors = actualScope.getAllDescriptors();
		return Collections2.filter(allDescriptors, new Predicate<DeclarationDescriptor>()
		{
			@Override
			public boolean apply(@Nullable DeclarationDescriptor descriptor)
			{
				return isClass(descriptor);
			}
		});
	}

	@NotNull
	@Override
	public ReceiverDescriptor getImplicitReceiver()
	{
		return ReceiverDescriptor.NO_RECEIVER;
	}

	@Override
	public void getImplicitReceiversHierarchy(@NotNull List<ReceiverDescriptor> result)
	{
		// Do nothing
	}

	@Override
	public String toString()
	{
		return "Classes from " + actualScope;
	}
}
