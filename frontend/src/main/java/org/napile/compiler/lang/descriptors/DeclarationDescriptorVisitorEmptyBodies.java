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

/**
 * @author abreslav
 */
public class DeclarationDescriptorVisitorEmptyBodies<R, D> implements DeclarationDescriptorVisitor<R, D>
{
	public R visitDeclarationDescriptor(DeclarationDescriptor descriptor, D data)
	{
		return null;
	}

	@Override
	public R visitVariableDescriptor(VariableDescriptor descriptor, D data)
	{
		return visitDeclarationDescriptor(descriptor, data);
	}

	@Override
	public R visitFunctionDescriptor(MethodDescriptor descriptor, D data)
	{
		return visitDeclarationDescriptor(descriptor, data);
	}

	@Override
	public R visitTypeParameterDescriptor(TypeParameterDescriptor descriptor, D data)
	{
		return visitDeclarationDescriptor(descriptor, data);
	}

	@Override
	public R visitNamespaceDescriptor(PackageDescriptor descriptor, D data)
	{
		return visitDeclarationDescriptor(descriptor, data);
	}

	@Override
	public R visitClassDescriptor(ClassDescriptor descriptor, D data)
	{
		return visitDeclarationDescriptor(descriptor, data);
	}

	@Override
	public R visitModuleDeclaration(ModuleDescriptor descriptor, D data)
	{
		return visitDeclarationDescriptor(descriptor, data);
	}

	@Override
	public R visitConstructorDescriptor(ConstructorDescriptor constructorDescriptor, D data)
	{
		return visitFunctionDescriptor(constructorDescriptor, data);
	}

	@Override
	public R visitLocalVariableDescriptor(LocalVariableDescriptor descriptor, D data)
	{
		return visitVariableDescriptor(descriptor, data);
	}

	@Override
	public R visitPropertyDescriptor(VariableDescriptorImpl descriptor, D data)
	{
		return visitVariableDescriptor(descriptor, data);
	}

	@Override
	public R visitCallParameterAsVariableDescriptor(CallParameterDescriptor descriptor, D data)
	{
		return visitVariableDescriptor(descriptor, data);
	}

	@Override
	public R visitCallParameterAsReferenceDescriptor(CallParameterAsReferenceDescriptorImpl descriptor, D data)
	{
		return visitDeclarationDescriptor(descriptor, data);
	}
}
