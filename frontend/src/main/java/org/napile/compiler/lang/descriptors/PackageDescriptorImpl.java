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

package org.napile.compiler.lang.descriptors;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.resolve.scopes.WritableScope;

/**
 * @author abreslav
 */
public class PackageDescriptorImpl extends AbstractPackageDescriptorImpl implements WithDeferredResolve
{

	private WritableScope memberScope;

	public PackageDescriptorImpl(@NotNull NamespaceDescriptorParent containingDeclaration, @NotNull List<AnnotationDescriptor> annotations, @NotNull Name name)
	{
		super(containingDeclaration, annotations, name);
	}

	public void initialize(@NotNull WritableScope memberScope)
	{
		if(this.memberScope != null)
		{
			throw new IllegalStateException("Namespace member scope reinitialize");
		}

		this.memberScope = memberScope;
	}

	@Override
	@NotNull
	public WritableScope getMemberScope()
	{
		return memberScope;
	}

	@Override
	public void addNamespace(@NotNull PackageDescriptor packageDescriptor)
	{
		getMemberScope().addNamespace(packageDescriptor);
	}

	@NotNull
	@Override
	public FqName getQualifiedName()
	{
		return DescriptorUtils.getFQName(this).toSafe();
	}

	private DescriptorBuilder builder = null;

	public DescriptorBuilder getBuilder()
	{
		if(builder == null)
		{
			builder = new DescriptorBuilderDummy()
			{
				@Override
				public void addClassifierDescriptor(@NotNull MutableClassDescriptorLite classDescriptor)
				{
					getMemberScope().addClassifierDescriptor(classDescriptor);
				}

				@NotNull
				@Override
				public DeclarationDescriptor getOwnerForChildren()
				{
					return PackageDescriptorImpl.this;
				}
			};
		}

		return builder;
	}

	@Override
	public void forceResolve()
	{

	}

	@Override
	public boolean isAlreadyResolved()
	{
		return false;
	}
}
