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

package org.napile.compiler.codegen.processors.visitors.loopCodegen;

import java.util.Collections;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.AsmConstants;
import org.napile.asm.lib.NapileCollectionPackage;
import org.napile.asm.resolve.name.Name;
import org.napile.asm.tree.members.MethodParameterNode;
import org.napile.asm.tree.members.bytecode.MethodRef;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.bytecode.adapter.ReservedInstruction;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.asm.tree.members.types.constructors.TypeParameterValueTypeNode;
import org.napile.compiler.codegen.processors.AsmNodeUtil;
import org.napile.compiler.codegen.processors.ExpressionCodegen;
import org.napile.compiler.codegen.processors.codegen.TypeConstants;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.psi.NapileForExpression;
import org.napile.compiler.lang.resolve.BindingTraceKeys;

/**
 * @author VISTALL
 * @since 21:16/02.10.12
 */
public class ForLoopCodegen extends LoopCodegen<NapileForExpression>
{
	private DeclarationDescriptor loopParameterDescriptor;

	private ReservedInstruction jumpIfSlot;

	public ForLoopCodegen(@NotNull NapileForExpression expression)
	{
		super(expression);
	}

	@Override
	protected void beforeLoop(ExpressionCodegen gen, InstructionAdapter instructions)
	{
		loopParameterDescriptor = gen.bindingTrace.safeGet(BindingTraceKeys.DECLARATION_TO_DESCRIPTOR, expression.getLoopParameter());
		int loopParameterIndex = gen.frameMap.enter(loopParameterDescriptor);

		// temp var for iterator ref
		int loopIteratorIndex = gen.frameMap.enterTemp();
		instructions.visitLocalVariable("temp$iterator");
		instructions.visitLocalVariable(loopParameterDescriptor.getName().getName());

		// put Iterator instance to stack
		MethodDescriptor methodDescriptor = gen.bindingTrace.safeGet(BindingTraceKeys.LOOP_RANGE_ITERATOR, expression.getLoopRange());
		gen.gen(expression.getLoopRange(), TypeConstants.ITERATOR__ANY__);
		instructions.invokeVirtual(AsmNodeUtil.ref(methodDescriptor, gen.bindingTrace, gen.classNode), false);
		instructions.localPut(loopIteratorIndex);

		firstPos = instructions.size();

		instructions.localGet(loopIteratorIndex);
		instructions.invokeVirtual(new MethodRef(NapileCollectionPackage.ITERATOR.child(Name.identifier("hasNext")), Collections.<MethodParameterNode>emptyList(), Collections.<TypeNode>emptyList(), AsmConstants.BOOL_TYPE), false);
		instructions.putTrue();
		jumpIfSlot = instructions.reserve();

		instructions.localGet(loopIteratorIndex);
		instructions.invokeVirtual(new MethodRef(NapileCollectionPackage.ITERATOR.child(Name.identifier("next")), Collections.<MethodParameterNode>emptyList(), Collections.<TypeNode>emptyList(), new TypeNode(false, new TypeParameterValueTypeNode(Name.identifier("E")))), false);
		instructions.localPut(loopParameterIndex);
	}

	@Override
	protected void afterLoop(ExpressionCodegen gen, InstructionAdapter instructions)
	{
		instructions.jump(firstPos);

		instructions.replace(jumpIfSlot).jumpIf(instructions.size());

		gen.frameMap.leaveTemp();
		gen.frameMap.leave(loopParameterDescriptor);
	}
}
