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

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.tree.members.ClassNode;
import org.napile.asm.tree.members.VariableNode;
import org.napile.asm.tree.members.bytecode.MethodRef;
import org.napile.asm.tree.members.bytecode.VariableRef;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.ParameterDescriptor;
import org.napile.compiler.lang.descriptors.PropertyDescriptor;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.resolve.name.FqName;
import org.napile.compiler.lang.resolve.name.Name;
import org.napile.compiler.lang.types.lang.JetStandardClasses;

/**
 * @author VISTALL
 * @date 22:10/07.09.12
 */
public class NodeRefUtil
{
	public static VariableRef ref(@NotNull ClassNode classNode, @NotNull VariableNode variableNode)
	{
		return new VariableRef(classNode.name.child(Name.identifier(variableNode.name)), variableNode.returnType);
	}

	public static VariableRef ref(@NotNull PropertyDescriptor propertyDescriptor)
	{
		return new VariableRef(DescriptorUtils.getFQName(propertyDescriptor).toSafe(), TypeGenerator.toAsmType(propertyDescriptor.getType()));
	}

	public static MethodRef ref(CallableDescriptor descriptor)
	{
		FqName fqName = DescriptorUtils.getFQName(descriptor).toSafe();
		List<TypeNode> typeNodes = new ArrayList<TypeNode>(descriptor.getValueParameters().size());
		for(ParameterDescriptor p : descriptor.getValueParameters())
			typeNodes.add(TypeGenerator.toAsmType(p.getType()));

		return new MethodRef(fqName, typeNodes, JetStandardClasses.isUnit(descriptor.getReturnType()) ? null : TypeGenerator.toAsmType(descriptor.getReturnType()));
	}
}