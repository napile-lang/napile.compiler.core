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

package org.napile.compiler.codegen.processors;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.tree.members.types.ClassTypeNode;
import org.napile.asm.tree.members.types.TypeConstructorNode;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ClassifierDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.resolve.name.FqName;
import org.napile.compiler.lang.types.JetType;

/**
 * @author VISTALL
 * @date 0:48/07.09.12
 */
public class TypeGenerator
{
	@NotNull
	public static TypeNode toAsmType(@NotNull JetType jetType)
	{
		TypeConstructorNode typeConstructorNode = null;
		ClassifierDescriptor owner = jetType.getConstructor().getDeclarationDescriptor();
		if(owner instanceof ClassDescriptor)
			typeConstructorNode = new ClassTypeNode(DescriptorUtils.getFQName(owner).toSafe());
		else if(owner instanceof TypeParameterDescriptor)
			typeConstructorNode = new ClassTypeNode(new FqName(owner.getName().getName())) ;//TODO [VISTALL] invalid
		else
			throw new RuntimeException("invalid " + owner);

		TypeNode typeNode = new TypeNode(jetType.isNullable(), typeConstructorNode);

		//TODO [VISTALL] annotations & type parameters
		return typeNode;
	}
}