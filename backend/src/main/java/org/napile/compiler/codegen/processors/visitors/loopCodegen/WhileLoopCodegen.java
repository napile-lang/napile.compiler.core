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

import org.jetbrains.annotations.NotNull;
import org.napile.asm.AsmConstants;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.bytecode.adapter.ReservedInstruction;
import org.napile.compiler.codegen.processors.ExpressionCodegen;
import org.napile.compiler.lang.psi.NapileWhileExpression;

/**
 * @author VISTALL
 * @since 11:29/03.10.12
 */
public class WhileLoopCodegen extends LoopCodegen<NapileWhileExpression>
{
	private ReservedInstruction ifSlot;

	public WhileLoopCodegen(@NotNull NapileWhileExpression expression)
	{
		super(expression);
	}

	@Override
	protected void beforeLoop(ExpressionCodegen gen, InstructionAdapter instructions)
	{
		super.beforeLoop(gen, instructions);

		gen.gen(expression.getCondition(), AsmConstants.BOOL_TYPE);

		instructions.putTrue();

		ifSlot = instructions.reserve();
	}

	@Override
	protected void afterLoop(ExpressionCodegen gen, InstructionAdapter instructions)
	{
		int jumpOutPos = instructions.size() + 1;

		instructions.replace(ifSlot).jumpIf(jumpOutPos);

		instructions.jump(firstPos);
	}
}
