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

import java.util.Arrays;
import java.util.Collections;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.AsmConstants;
import org.napile.asm.lib.NapileConditionPackage;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.asm.resolve.name.Name;
import org.napile.asm.tree.members.bytecode.MethodRef;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.bytecode.adapter.ReservedInstruction;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.asm.tree.members.types.constructors.ClassTypeNode;
import org.napile.compiler.codegen.processors.codegen.CallTransformer;
import org.napile.compiler.codegen.processors.codegen.CallableMethod;
import org.napile.compiler.codegen.processors.codegen.TypeConstants;
import org.napile.compiler.codegen.processors.codegen.stackValue.SimpleVariableAccessor;
import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.NapileBinaryExpression;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapilePostfixExpression;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.calls.ResolvedCall;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.expressions.OperatorConventions;
import com.intellij.psi.tree.IElementType;

/**
 * @author VISTALL
 * @date 10:50/05.10.12
 */
public class BinaryOperationCodegen
{
	public static final MethodRef ANY_EQUALS = new MethodRef(NapileLangPackage.ANY.child(Name.identifier("equals")), Arrays.asList(new TypeNode(true, new ClassTypeNode(NapileLangPackage.ANY))), Collections.<TypeNode>emptyList(), AsmConstants.BOOL_TYPE);

	private static final SimpleVariableAccessor GREATER = new SimpleVariableAccessor(NapileConditionPackage.COMPARE_RESULT.child(Name.identifier("GREATER")), TypeConstants.COMPARE_RESULT, CallableMethod.CallType.STATIC);
	private static final SimpleVariableAccessor EQUAL = new SimpleVariableAccessor(NapileConditionPackage.COMPARE_RESULT.child(Name.identifier("EQUAL")), TypeConstants.COMPARE_RESULT, CallableMethod.CallType.STATIC);
	private static final SimpleVariableAccessor LOWER = new SimpleVariableAccessor(NapileConditionPackage.COMPARE_RESULT.child(Name.identifier("LOWER")), TypeConstants.COMPARE_RESULT, CallableMethod.CallType.STATIC);

	public static StackValue genSure(@NotNull NapilePostfixExpression expression, @NotNull ExpressionCodegen gen, @NotNull InstructionAdapter instructs, @NotNull StackValue receiver)
	{
		NapileExpression baseExpression = expression.getBaseExpression();
		JetType type = gen.bindingTrace.get(BindingContext.EXPRESSION_TYPE, baseExpression);
		StackValue base = gen.genQualified(receiver, baseExpression);
		if(type != null && type.isNullable())
		{
			base.put(base.getType(), instructs);
			instructs.dup();

			instructs.putNull();

			instructs.invokeVirtual(ANY_EQUALS, false);

			instructs.putTrue();

			ReservedInstruction jump = instructs.reserve();

			instructs.newString("'" + baseExpression.getText() + "' cant be null");
			instructs.newObject(TypeConstants.NULL_POINTER_EXCEPTION, Collections.<TypeNode>singletonList(TypeConstants.NULLABLE_STRING));
			instructs.throwVal();

			instructs.replace(jump).jumpIf(instructs.size());

			return StackValue.onStack(base.getType());
		}
		else
			return base;
	}

	public static StackValue genGeLe(@NotNull NapileBinaryExpression expression, @NotNull ExpressionCodegen gen)
	{
		final IElementType opToken = expression.getOperationReference().getReferencedNameElementType();

		TypeNode leftType = gen.expressionType(expression.getLeft());
		TypeNode rightType = gen.expressionType(expression.getRight());

		gen.gen(expression.getLeft(), leftType);

		gen.gen(expression.getRight(), rightType);

		ClassTypeNode leftClassType = (ClassTypeNode) leftType.typeConstructorNode;
		//ClassTypeNode rightClassType = (ClassTypeNode) rightType.typeConstructorNode;

		gen.instructs.invokeVirtual(new MethodRef(leftClassType.className.child(OperatorConventions.COMPARE_TO), Collections.singletonList(rightType), Collections.<TypeNode>emptyList(), TypeConstants.COMPARE_RESULT), false);

		if(opToken == NapileTokens.GT)
			gtOrLt(GREATER, gen.instructs);
		else if(opToken == NapileTokens.LT)
			gtOrLt(LOWER, gen.instructs);
		else if(opToken == NapileTokens.GTEQ)
			gtOrLtEq(GREATER, gen.instructs);
		else if(opToken == NapileTokens.LTEQ)
			gtOrLtEq(LOWER, gen.instructs);

		return StackValue.onStack(AsmConstants.BOOL_TYPE);
	}

	private static void gtOrLtEq(@NotNull SimpleVariableAccessor simpleVariableAccessor, @NotNull InstructionAdapter instructs)
	{
		instructs.dup();

		simpleVariableAccessor.put(TypeConstants.COMPARE_RESULT, instructs);

		instructs.invokeVirtual(ANY_EQUALS, false);

		instructs.putTrue();

		ReservedInstruction ifSlot = instructs.reserve();

		// if is equal - property - put true and jump over
		instructs.putTrue();

		ReservedInstruction jumpSlot = instructs.reserve();

		instructs.replace(ifSlot).jumpIf(instructs.size());

		// else check is is equal

		gtOrLt(EQUAL, instructs);

		instructs.replace(jumpSlot).jump(instructs.size());
	}

	private static void gtOrLt(@NotNull SimpleVariableAccessor simpleVariableAccessor, @NotNull InstructionAdapter instructs)
	{
		simpleVariableAccessor.put(TypeConstants.COMPARE_RESULT, instructs);

		instructs.invokeVirtual(ANY_EQUALS, false);

		instructs.putTrue();

		ReservedInstruction ifSlot = instructs.reserve();

		instructs.putTrue();

		ReservedInstruction jumpSlot = instructs.reserve();

		instructs.replace(ifSlot).jumpIf(instructs.size());

		instructs.putFalse();

		// jump - ignored else
		instructs.replace(jumpSlot).jump(instructs.size());
	}

	public static StackValue genEq(@NotNull NapileBinaryExpression expression, @NotNull ExpressionCodegen gen)
	{
		StackValue leftStackValue = gen.gen(expression.getLeft());

		gen.gen(expression.getRight(), leftStackValue.getType());

		MethodDescriptor methodDescriptor = gen.bindingTrace.get(BindingContext.VARIABLE_CALL, expression);
		if(methodDescriptor != null)
			leftStackValue = StackValue.variableAccessor(methodDescriptor, leftStackValue.getType(), gen);

		leftStackValue.store(leftStackValue.getType(), gen.instructs);
		return StackValue.none();
	}

	public static StackValue genEqEq(@NotNull NapileBinaryExpression expression, @NotNull ExpressionCodegen gen)
	{
		final IElementType opToken = expression.getOperationReference().getReferencedNameElementType();

		NapileExpression left = expression.getLeft();
		NapileExpression right = expression.getRight();

		JetType leftJetType = gen.bindingTrace.safeGet(BindingContext.EXPRESSION_TYPE, left);
		TypeNode leftType = TypeTransformer.toAsmType(gen.bindingTrace, leftJetType, gen.classNode);

		JetType rightJetType = gen.bindingTrace.safeGet(BindingContext.EXPRESSION_TYPE, right);
		TypeNode rightType = TypeTransformer.toAsmType(gen.bindingTrace, rightJetType, gen.classNode);

		gen.gen(left, leftType);

		gen.gen(right, rightType);

		DeclarationDescriptor op = gen.bindingTrace.safeGet(BindingContext.REFERENCE_TARGET, expression.getOperationReference());
		final CallableMethod callable = CallTransformer.transformToCallable(gen, (MethodDescriptor) op, Collections.<TypeNode>emptyList(), false, false, false);
		callable.invoke(gen.instructs);

		// revert bool
		if(opToken == NapileTokens.EXCLEQ)
			gen.instructs.invokeVirtual(new MethodRef(NapileLangPackage.BOOL.child(Name.identifier("not")), Collections.<TypeNode>emptyList(), Collections.<TypeNode>emptyList(), AsmConstants.BOOL_TYPE), false);

		return StackValue.onStack(AsmConstants.BOOL_TYPE);
	}

	public static StackValue genAugmentedAssignment(@NotNull NapileBinaryExpression expression, @NotNull ExpressionCodegen gen, @NotNull InstructionAdapter instructs)
	{
		final NapileExpression lhs = expression.getLeft();

		TypeNode lhsType = gen.expressionType(lhs);

		ResolvedCall<? extends CallableDescriptor> resolvedCall = gen.bindingTrace.safeGet(BindingContext.RESOLVED_CALL, expression.getOperationReference());

		final CallableMethod callable = CallTransformer.transformToCallable(gen, resolvedCall, false, false, false);

		StackValue value = gen.gen(expression.getLeft());
		value.dupReceiver(instructs);
		value.put(lhsType, instructs);
		StackValue receiver = StackValue.onStack(lhsType);

		if(!(resolvedCall.getResultingDescriptor() instanceof ConstructorDescriptor))
		{ // otherwise already
			receiver = StackValue.receiver(resolvedCall, receiver, gen, callable);
			receiver.put(receiver.getType(), instructs);
		}

		gen.pushMethodArguments(resolvedCall, callable.getValueParameterTypes());
		callable.invoke(instructs);

		MethodDescriptor methodDescriptor = gen.bindingTrace.get(BindingContext.VARIABLE_CALL, expression);
		if(methodDescriptor != null)
			value = StackValue.variableAccessor(methodDescriptor, value.getType(), gen);

		value.store(callable.getReturnType(), instructs);

		return StackValue.none();
	}

	public static StackValue genElvis(@NotNull NapileBinaryExpression expression, @NotNull ExpressionCodegen gen)
	{
		final TypeNode exprType = gen.expressionType(expression);
		JetType type = gen.bindingTrace.safeGet(BindingContext.EXPRESSION_TYPE, expression.getLeft());
		final TypeNode leftType = TypeTransformer.toAsmType(gen.bindingTrace, type, gen.classNode);

		gen.gen(expression.getLeft(), leftType);

		gen.instructs.dup();

		gen.instructs.putNull();

		gen.instructs.invokeVirtual(ANY_EQUALS, false);

		gen.instructs.putTrue();

		ReservedInstruction ifSlot = gen.instructs.reserve();

		gen.gen(expression.getRight(), exprType);

		gen.instructs.replace(ifSlot).jumpIf(gen.instructs.size());

		return StackValue.onStack(exprType);
	}

	public static StackValue genAndAnd(@NotNull NapileBinaryExpression expression, @NotNull ExpressionCodegen gen, @NotNull InstructionAdapter instructs)
	{
		gen.gen(expression.getLeft(), AsmConstants.BOOL_TYPE);

		instructs.putTrue();

		ReservedInstruction ifSlot = instructs.reserve();

		gen.gen(expression.getRight(), AsmConstants.BOOL_TYPE);

		instructs.putTrue();

		ReservedInstruction ifSlot2 = instructs.reserve();

		instructs.putTrue();

		ReservedInstruction ignoreFalseSlot = instructs.reserve();

		// if left of right exp failed jump to false
		instructs.replace(ifSlot).jumpIf(instructs.size());
		instructs.replace(ifSlot2).jumpIf(instructs.size());

		instructs.putFalse();

		instructs.replace(ignoreFalseSlot).jump(instructs.size());

		return StackValue.onStack(AsmConstants.BOOL_TYPE);
	}

	public static StackValue genOrOr(@NotNull NapileBinaryExpression expression, @NotNull ExpressionCodegen gen, @NotNull InstructionAdapter instructs)
	{
		gen.gen(expression.getLeft(), AsmConstants.BOOL_TYPE);

		instructs.putTrue();

		ReservedInstruction ifSlot = instructs.reserve();

		// result
		instructs.putTrue();

		ReservedInstruction skipNextSlot = instructs.reserve();

		// is first is failed - jump to right part
		instructs.replace(ifSlot).jumpIf(instructs.size());

		gen.gen(expression.getRight(), AsmConstants.BOOL_TYPE);

		instructs.putTrue();

		ReservedInstruction ifSlot2 = instructs.reserve();

		instructs.putTrue();

		ReservedInstruction skipNextSlot2 = instructs.reserve();

		// jump to false
		instructs.replace(ifSlot2).jumpIf(instructs.size());

		// result
		instructs.putFalse();

		// skips instructions - jump over expression
		instructs.replace(skipNextSlot).jump(instructs.size());
		instructs.replace(skipNextSlot2).jump(instructs.size());

		return StackValue.onStack(AsmConstants.BOOL_TYPE);
	}
}
