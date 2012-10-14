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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.asm.resolve.name.Name;
import org.napile.asm.tree.members.bytecode.MethodRef;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.bytecode.adapter.ReservedInstruction;
import org.napile.asm.tree.members.bytecode.impl.JumpIfInstruction;
import org.napile.asm.tree.members.bytecode.impl.JumpInstruction;
import org.napile.asm.tree.members.bytecode.tryCatch.CatchBlock;
import org.napile.asm.tree.members.bytecode.tryCatch.TryBlock;
import org.napile.asm.tree.members.bytecode.tryCatch.TryCatchBlockNode;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.codegen.CompilationException;
import org.napile.compiler.codegen.processors.codegen.BinaryOperationCodegen;
import org.napile.compiler.codegen.processors.codegen.CallTransformer;
import org.napile.compiler.codegen.processors.codegen.CallableMethod;
import org.napile.compiler.codegen.processors.codegen.FrameMap;
import org.napile.compiler.codegen.processors.codegen.TypeConstants;
import org.napile.compiler.codegen.processors.codegen.loopGen.DoWhileLoopCodegen;
import org.napile.compiler.codegen.processors.codegen.loopGen.ForLoopCodegen;
import org.napile.compiler.codegen.processors.codegen.loopGen.LabelLoopCodegen;
import org.napile.compiler.codegen.processors.codegen.loopGen.LoopCodegen;
import org.napile.compiler.codegen.processors.codegen.loopGen.WhileLoopCodegen;
import org.napile.compiler.codegen.processors.codegen.stackValue.Local;
import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.ParameterDescriptor;
import org.napile.compiler.lang.descriptors.PropertyDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.resolve.calls.AutoCastReceiver;
import org.napile.compiler.lang.resolve.calls.ExpressionValueArgument;
import org.napile.compiler.lang.resolve.calls.ResolvedCall;
import org.napile.compiler.lang.resolve.calls.ResolvedValueArgument;
import org.napile.compiler.lang.resolve.calls.VariableAsFunctionResolvedCall;
import org.napile.compiler.lang.resolve.constants.CompileTimeConstant;
import org.napile.compiler.lang.resolve.scopes.receivers.ClassReceiver;
import org.napile.compiler.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.expressions.OperatorConventions;
import org.napile.compiler.lexer.NapileTokens;
import org.napile.compiler.psi.NapileElement;
import org.napile.compiler.psi.NapileExpression;
import com.google.common.collect.Lists;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.Comparing;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.Function;

/**
 * @author VISTALL
 * @date 0:24/07.09.12
 * <p/>
 * base code getting from Kotlin
 */
public class ExpressionGenerator extends NapileVisitor<StackValue, StackValue>
{
	@NotNull
	public final BindingTrace bindingTrace;
	@NotNull
	private final Map<NapileElement, Local> tempVariables = new HashMap<NapileElement, Local>();
	@NotNull
	private final InstructionAdapter instructs = new InstructionAdapter();
	@NotNull
	public final FrameMap frameMap;
	@NotNull
	private final TypeNode returnType;
	@NotNull
	private final Deque<LoopCodegen<?>> loops = new ArrayDeque<LoopCodegen<?>>();

	private final boolean isInstanceConstructor;

	public ExpressionGenerator(@NotNull BindingTrace b, @NotNull TypeNode r)
	{
		bindingTrace = b;
		isInstanceConstructor = false;
		returnType = r;
		frameMap = new FrameMap();
	}

	public ExpressionGenerator(@NotNull BindingTrace b, @NotNull CallableDescriptor d)
	{
		bindingTrace = b;
		isInstanceConstructor = d instanceof ConstructorDescriptor;
		returnType = isInstanceConstructor ? TypeTransformer.toAsmType(((ClassDescriptor) d.getContainingDeclaration()).getDefaultType()) : TypeTransformer.toAsmType(d.getReturnType());
		frameMap = new FrameMap();

		if(!d.isStatic())
			frameMap.enterTemp();
		for(ParameterDescriptor p : d.getValueParameters())
			frameMap.enter(p);
	}

	@Override
	public StackValue visitVariable(NapileVariable property, StackValue receiver)
	{
		final NapileExpression initializer = property.getInitializer();
		if(initializer == null)
			return StackValue.none();

		initializeLocalVariable(property, new Function<VariableDescriptor, Void>()
		{
			@Override
			public Void fun(VariableDescriptor descriptor)
			{
				TypeNode varType = TypeTransformer.toAsmType(descriptor.getType());
				gen(initializer, varType);
				return null;
			}
		});
		return StackValue.none();
	}

	@Override
	public StackValue visitExpression(NapileExpression expression, StackValue receiver)
	{
		throw new UnsupportedOperationException("Codegen for " + expression + " is not yet implemented");
	}

	@Override
	public StackValue visitDotQualifiedExpression(NapileDotQualifiedExpression expression, StackValue receiver)
	{
		return genQualified(StackValue.none(), expression.getSelectorExpression());
	}

	@Override
	public StackValue visitParenthesizedExpression(NapileParenthesizedExpression expression, StackValue receiver)
	{
		return genQualified(receiver, expression.getExpression());
	}

	@Override
	public StackValue visitBlockExpression(NapileBlockExpression expression, StackValue receiver)
	{
		List<NapileElement> statements = expression.getStatements();
		return generateBlock(statements);
	}

	@Override
	public StackValue visitForExpression(NapileForExpression expression, StackValue data)
	{
		return loopGen(new ForLoopCodegen(expression));
	}

	@Override
	public StackValue visitWhileExpression(NapileWhileExpression expression, StackValue data)
	{
		return loopGen(new WhileLoopCodegen(expression));
	}

	@Override
	public StackValue visitDoWhileExpression(NapileDoWhileExpression expression, StackValue data)
	{
		return loopGen(new DoWhileLoopCodegen(expression));
	}

	@Override
	public StackValue visitLabelExpression(NapileLabelExpression expression, StackValue data)
	{
		return loopGen(new LabelLoopCodegen(expression));
	}

	@Override
	public StackValue visitContinueExpression(NapileContinueExpression expression, StackValue data)
	{
		LoopCodegen<?> last = loops.getLast();

		last.addContinue(instructs);

		return StackValue.none();
	}

	@Override
	public StackValue visitTryExpression(NapileTryExpression expression, StackValue data)
	{
		JetType jetType = bindingTrace.safeGet(BindingContext.EXPRESSION_TYPE, expression);

		TypeNode expectedAsmType = TypeTransformer.toAsmType(jetType);

		final int tryStartIndex = instructs.size();

		gen(expression.getTryBlock());

		List<ReservedInstruction> jumpOutInstructions = new ArrayList<ReservedInstruction>(2);
		jumpOutInstructions.add(instructs.reserve());

		TryBlock tryBlock = new TryBlock(tryStartIndex, instructs.size());
		List<CatchBlock> catchBlocks = new ArrayList<CatchBlock>(2);

		for(NapileCatchClause catchClause : expression.getCatchClauses())
		{
			VariableDescriptor catchParameter = (VariableDescriptor) bindingTrace.safeGet(BindingContext.DECLARATION_TO_DESCRIPTOR, catchClause.getCatchParameter());

			int index = frameMap.enter(catchParameter);

			instructs.visitLocalVariable(catchClause.getName());

			int startCatchIndex = instructs.size();

			gen(catchClause.getCatchBody());

			jumpOutInstructions.add(instructs.reserve());

			catchBlocks.add(new CatchBlock(startCatchIndex, instructs.size(), index, TypeTransformer.toAsmType(catchParameter.getType())));

			frameMap.leave(catchParameter);
		}

		final int nextIndex = instructs.size();
		for(ReservedInstruction r : jumpOutInstructions)
			instructs.replace(r, new JumpInstruction(nextIndex));

		instructs.tryCatch(new TryCatchBlockNode(tryBlock, catchBlocks));

		return StackValue.onStack(expectedAsmType);
	}

	@Override
	public StackValue visitThrowExpression(NapileThrowExpression expression, StackValue data)
	{
		NapileExpression throwExp = expression.getThrownExpression();

		assert throwExp != null;

		gen(throwExp, TypeConstants.THROWABLE);

		instructs.throwVal();

		return StackValue.onStack(TypeConstants.THROWABLE);
	}

	@Override
	public StackValue visitBreakExpression(NapileBreakExpression expression, StackValue data)
	{
		LoopCodegen<?> targetLoop = null;
		NapileSimpleNameExpression labelRef = expression.getTargetLabel();
		if(labelRef != null)
		{
			Iterator<LoopCodegen<?>> it = loops.descendingIterator();
			while(it.hasNext())
			{
				LoopCodegen<?> e = it.next();
				if(Comparing.equal(e.getName(), expression.getLabelName()))
				{
					targetLoop = e;
					break;
				}
			}
		}
		else
			targetLoop = loops.getLast();

		assert targetLoop != null;

		targetLoop.addBreak(instructs);

		return StackValue.none();
	}

	@Override
	public StackValue visitIsExpression(NapileIsExpression expression, StackValue data)
	{
		gen(expression.getLeftHandSide(), TypeTransformer.toAsmType(bindingTrace.safeGet(BindingContext.EXPRESSION_TYPE, expression.getLeftHandSide())));

		JetType rightType = bindingTrace.safeGet(BindingContext.TYPE, expression.getTypeRef());

		instructs.is(TypeTransformer.toAsmType(rightType));

		if(expression.isNegated())
			instructs.invokeVirtual(new MethodRef(NapileLangPackage.BOOL.child(Name.identifier("not")), Collections.<TypeNode>emptyList(), Collections.<TypeNode>emptyList(), TypeConstants.BOOL));

		return StackValue.onStack(TypeConstants.BOOL);
	}

	@Override
	public StackValue visitPrefixExpression(NapilePrefixExpression expression, StackValue receiver)
	{
		DeclarationDescriptor op = bindingTrace.safeGet(BindingContext.REFERENCE_TARGET, expression.getOperationReference());
		ResolvedCall<? extends CallableDescriptor> resolvedCall = bindingTrace.safeGet(BindingContext.RESOLVED_CALL, expression.getOperationReference());

		final CallableMethod callableMethod = CallTransformer.transformToCallable(resolvedCall);

		if(!(op.getName().getName().equals("inc") || op.getName().getName().equals("dec")))
			return invokeOperation(expression, (MethodDescriptor) op, callableMethod);
		else
		{
			StackValue value = gen(expression.getBaseExpression());
			value.dupReceiver(instructs);
			value.dupReceiver(instructs);

			TypeNode type = expressionType(expression.getBaseExpression());
			value.put(type, instructs);
			callableMethod.invoke(instructs);
			value.store(callableMethod.getReturnType(), instructs);
			value.put(type, instructs);
			return StackValue.onStack(type);
		}
	}

	@Override
	public StackValue visitThisExpression(NapileThisExpression expression, StackValue receiver)
	{
		final DeclarationDescriptor descriptor = bindingTrace.get(BindingContext.REFERENCE_TARGET, expression.getInstanceReference());
		if(descriptor instanceof ClassDescriptor)
			return StackValue.thisOrOuter(this, (ClassDescriptor) descriptor, false);
		else
		{
			/*if(descriptor instanceof CallableDescriptor)
			{
				return generateReceiver(descriptor);
			}  */
			throw new UnsupportedOperationException("neither this nor receiver");
		}
	}

	@Override
	public StackValue visitIfExpression(NapileIfExpression expression, StackValue receiver)
	{
		TypeNode asmType = expressionType(expression);

		NapileExpression thenExpression = expression.getThen();
		NapileExpression elseExpression = expression.getElse();

		if(thenExpression == null && elseExpression == null)
			throw new CompilationException("Both brunches of if/else are null", null, expression);

		if(isEmptyExpression(thenExpression))
		{
			if(isEmptyExpression(elseExpression))
			{
				if(!asmType.equals(TypeConstants.NULL))
					throw new CompilationException("Completely empty 'if' is expected to have Null type", null, expression);

				instructs.putNull();
				return StackValue.onStack(asmType);
			}
			StackValue condition = gen(expression.getCondition());
			return generateSingleBranchIf(condition, elseExpression, false);
		}
		else
		{
			if(isEmptyExpression(elseExpression))
			{
				StackValue condition = gen(expression.getCondition());
				return generateSingleBranchIf(condition, thenExpression, true);
			}
		}


		StackValue condition = gen(expression.getCondition());

		condition.put(TypeConstants.BOOL, instructs);

		instructs.putTrue();

		ReservedInstruction ifSlot = instructs.reserve();

		gen(thenExpression, asmType);

		ReservedInstruction afterIfSlot = instructs.reserve();

		int elseStartIndex = instructs.size();

		gen(elseExpression, asmType);

		int afterIfStartIndex = instructs.size();

		// replace ifSlot - by jump_if - index is start 'else' block
		instructs.replace(ifSlot, new JumpIfInstruction(elseStartIndex));
		// at end of 'then' block ignore 'else' block
		instructs.replace(afterIfSlot, new JumpInstruction(afterIfStartIndex));

		return StackValue.onStack(asmType);
	}

	private StackValue generateSingleBranchIf(StackValue condition, NapileExpression expression, boolean inverse)
	{
		TypeNode expressionType = expressionType(expression);
		TypeNode targetType = expressionType;
		if(!expressionType.equals(TypeConstants.NULL))
			targetType = TypeConstants.ANY;

		condition.put(TypeConstants.BOOL, instructs);

		if(inverse)
			instructs.putTrue();
		else
			instructs.putFalse();

		ReservedInstruction ifSlot = instructs.reserve();

		gen(expression, expressionType);

		StackValue.castTo(expressionType, targetType, instructs);

		instructs.replace(ifSlot, new JumpIfInstruction(instructs.size()));

		return StackValue.onStack(targetType);
	}

	@Override
	public StackValue visitReturnExpression(NapileReturnExpression expression, StackValue receiver)
	{
		final NapileExpression returnedExpression = expression.getReturnedExpression();
		if(returnedExpression != null)
		{
			gen(returnedExpression, returnType);

			doFinallyOnReturn();

			instructs.returnVal();
		}
		else
		{
			if(isInstanceConstructor)
				instructs.load(0);
			else
				instructs.putNull();

			instructs.returnVal();
		}
		return StackValue.none();
	}

	@Override
	public StackValue visitStringTemplateExpression(NapileStringTemplateExpression expression, StackValue receiver)
	{
		StringBuilder constantValue = new StringBuilder("");
		final NapileStringTemplateEntry[] entries = expression.getEntries();
		for(NapileStringTemplateEntry entry : entries)
		{
			if(entry instanceof NapileLiteralStringTemplateEntry)
				constantValue.append(entry.getText());
			else if(entry instanceof NapileEscapeStringTemplateEntry)
				constantValue.append(((NapileEscapeStringTemplateEntry) entry).getUnescapedValue());
			else
			{
				constantValue = null;
				break;
			}
		}
		if(constantValue != null)
		{
			final TypeNode type = expressionType(expression);
			return StackValue.constant(constantValue.toString(), type);
		}
		else
		{
			/*instructs.newObject(TypeConstants.STRING_BUILDER);
			instructs.invokeSpecial(NodeRefUtil.constructorRef(CodeTodo.STRING_BUILDER));

			for(NapileStringTemplateEntry entry : entries)
			{
				if(entry instanceof NapileStringTemplateEntryWithExpression)
				{
					invokeAppend(entry.getExpression());
				}
				else
				{
					String text = entry instanceof NapileEscapeStringTemplateEntry ? ((NapileEscapeStringTemplateEntry) entry).getUnescapedValue() : entry.getText();
					v.aconst(text);
					genInvokeAppendMethod(v, JAVA_STRING_TYPE);
				}
			}
			v.invokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
			return StackValue.onStack(TypeConstants.STRING); */
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public StackValue visitBinaryExpression(NapileBinaryExpression expression, StackValue receiver)
	{
		final IElementType opToken = expression.getOperationReference().getReferencedNameElementType();
		if(opToken == NapileTokens.EQ)
			return BinaryOperationCodegen.genEq(expression, this, instructs);
		else if(OperatorConventions.ASSIGNMENT_OPERATION_COUNTERPARTS.containsKey(opToken))
			return BinaryOperationCodegen.genAugmentedAssignment(expression, this, instructs);
		else if(opToken == NapileTokens.ANDAND)
			return BinaryOperationCodegen.genAndAnd(expression, this, instructs);
		else if(opToken == NapileTokens.OROR)
			return BinaryOperationCodegen.genOrOr(expression, this, instructs);
		else if(opToken == NapileTokens.EQEQ || opToken == NapileTokens.EXCLEQ)
			return BinaryOperationCodegen.genEqEq(expression, this, instructs);
		else if(opToken == NapileTokens.LT || opToken == NapileTokens.LTEQ || opToken == NapileTokens.GT || opToken == NapileTokens.GTEQ)
			return BinaryOperationCodegen.genGeLe(expression, this, instructs);
		else if(opToken == NapileTokens.ELVIS)
			return BinaryOperationCodegen.genElvis(expression, this, instructs);
		/*else if(opToken == NapileTokens.IN_KEYWORD || opToken == NapileTokens.NOT_IN)
		{
				return final Type exprType = expressionType(expression);
        JetType type = bindingContext.get(BindingContext.EXPRESSION_TYPE, expression.getLeft());(expression);
		}
		else */
		else
		{
			DeclarationDescriptor op = bindingTrace.get(BindingContext.REFERENCE_TARGET, expression.getOperationReference());
			final CallableMethod callable = CallTransformer.transformToCallable((MethodDescriptor) op, Collections.<TypeNode>emptyList());

			return invokeOperation(expression, (MethodDescriptor) op, callable);
		}
	}

	@Override
	public StackValue visitPostfixExpression(NapilePostfixExpression expression, StackValue receiver)
	{
		if(expression.getOperationReference().getReferencedNameElementType() == NapileTokens.EXCLEXCL)
		{
			NapileExpression baseExpression = expression.getBaseExpression();
			JetType type = bindingTrace.get(BindingContext.EXPRESSION_TYPE, baseExpression);
			StackValue base = genQualified(receiver, baseExpression);
			if(type != null && type.isNullable())
			{
				/*base.put(base.type, v);
				v.dup();
				Label ok = new Label();
				v.ifnonnull(ok);
				v.invokestatic("jet/runtime/Intrinsics", "throwNpe", "()V");
				v.mark(ok);
				return StackValue.onStack(base.type);  */
				throw new UnsupportedOperationException();
			}
			else
				return base;
		}
		DeclarationDescriptor op = bindingTrace.get(BindingContext.REFERENCE_TARGET, expression.getOperationReference());
		if(op instanceof MethodDescriptor)
		{
			if(op.getName().getName().equals("inc") || op.getName().getName().equals("dec"))
			{
				ResolvedCall<? extends CallableDescriptor> resolvedCall = bindingTrace.safeGet(BindingContext.RESOLVED_CALL, expression.getOperationReference());

				final CallableMethod callable = CallTransformer.transformToCallable(resolvedCall);

				StackValue value = gen(expression.getBaseExpression());
				value.dupReceiver(instructs);

				TypeNode type = expressionType(expression.getBaseExpression());
				value.put(type, instructs);

				switch(value.receiverSize())
				{
					case 0:
						instructs.dup();
						break;
					case 1:
						instructs.dup1x1();
						break;
					default:
						throw new UnsupportedOperationException("Unknown receiver size " + value.receiverSize());
				}

				callable.invoke(instructs);
				value.store(callable.getReturnType(), instructs);

				return StackValue.onStack(type);
			}
		}
		throw new UnsupportedOperationException("Don't know how to generate this postfix expression");
	}

	@Override
	public StackValue visitCallExpression(NapileCallExpression expression, StackValue receiver)
	{
		final NapileExpression callee = expression.getCalleeExpression();
		assert callee != null;

		ResolvedCall<? extends CallableDescriptor> resolvedCall = bindingTrace.safeGet(BindingContext.RESOLVED_CALL, callee);

		DeclarationDescriptor funDescriptor = resolvedCall.getResultingDescriptor();

		if(!(funDescriptor instanceof MethodDescriptor))
			throw new UnsupportedOperationException("unknown type of callee descriptor: " + funDescriptor);

		//funDescriptor = accessableFunctionDescriptor((FunctionDescriptor) funDescriptor);

		if(funDescriptor instanceof ConstructorDescriptor)
		{
			receiver = StackValue.receiver(resolvedCall, receiver, this, null);
			return generateConstructorCall(expression, (NapileSimpleNameExpression) callee, receiver);
		}
		else
		{
			if(resolvedCall instanceof VariableAsFunctionResolvedCall)
			{
				throw new UnsupportedOperationException();
				//	VariableAsFunctionResolvedCall variableAsFunctionResolvedCall = (VariableAsFunctionResolvedCall) resolvedCall;
				//	ResolvedCallWithTrace<FunctionDescriptor> functionCall = variableAsFunctionResolvedCall.getFunctionCall();
				//	return invokeFunction(call, receiver, functionCall);
			}
			else
				return invokeFunction(receiver, resolvedCall);
		}
	}

	@Override
	public StackValue visitArrayAccessExpression(NapileArrayAccessExpression expression, StackValue receiver)
	{
		MethodDescriptor operationDescriptor = (MethodDescriptor) bindingTrace.safeGet(BindingContext.REFERENCE_TARGET, expression);
		CallableMethod accessor = CallTransformer.transformToCallable(operationDescriptor, Collections.<TypeNode>emptyList());

		boolean isGetter = accessor.getName().endsWith("get");

		ResolvedCall<MethodDescriptor> resolvedGetCall = bindingTrace.get(BindingContext.INDEXED_LVALUE_GET, expression);
		ResolvedCall<MethodDescriptor> resolvedSetCall = bindingTrace.get(BindingContext.INDEXED_LVALUE_SET, expression);

		List<TypeNode> argumentTypes = accessor.getValueParameterTypes();
		int index = 0;

		TypeNode asmType;
		if(isGetter)
		{
			genThisAndReceiverFromResolvedCall(receiver, resolvedGetCall, CallTransformer.transformToCallable(resolvedGetCall));

			asmType = accessor.getReturnType();
		}
		else
		{
			genThisAndReceiverFromResolvedCall(receiver, resolvedSetCall, CallTransformer.transformToCallable(resolvedSetCall));

			asmType = argumentTypes.get(argumentTypes.size() - 1);
		}

		for(NapileExpression jetExpression : expression.getIndexExpressions())
		{
			gen(jetExpression, argumentTypes.get(index));
			index++;
		}

		return StackValue.collectionElement(asmType, resolvedGetCall, resolvedSetCall, this);
	}

	@Override
	public StackValue visitSimpleNameExpression(NapileSimpleNameExpression expression, StackValue receiver)
	{
		ResolvedCall<? extends CallableDescriptor> resolvedCall = bindingTrace.get(BindingContext.RESOLVED_CALL, expression);

		DeclarationDescriptor descriptor;
		if(resolvedCall == null)
			descriptor = bindingTrace.safeGet(BindingContext.REFERENCE_TARGET, expression);
		else
		{
			if(resolvedCall instanceof VariableAsFunctionResolvedCall)
			{
				VariableAsFunctionResolvedCall call = (VariableAsFunctionResolvedCall) resolvedCall;
				resolvedCall = call.getVariableCall();
			}
			receiver = StackValue.receiver(resolvedCall, receiver, this, null);
			descriptor = resolvedCall.getResultingDescriptor();
		}

		final DeclarationDescriptor container = descriptor.getContainingDeclaration();

		int index = lookupLocalIndex(descriptor);
		if(index >= 0)
			return stackValueForLocal(descriptor, index);

		if(descriptor instanceof PropertyDescriptor)
		{
			PropertyDescriptor propertyDescriptor = (PropertyDescriptor) descriptor;

			boolean isStatic = propertyDescriptor.isStatic();
			final boolean directToField = expression.getReferencedNameElementType() == NapileTokens.FIELD_IDENTIFIER;
			NapileExpression r = getReceiverForSelector(expression);
			final boolean isSuper = r instanceof NapileSuperExpression;

			final StackValue iValue = intermediateValueForProperty(propertyDescriptor, directToField, isSuper ? (NapileSuperExpression) r : null);
			if(!directToField && resolvedCall != null && !isSuper)
				receiver.put(isStatic ? receiver.getType() : TypeTransformer.toAsmType(((ClassDescriptor) container).getDefaultType()), instructs);
			else
			{
				if(!isStatic)
				{
					if(receiver == StackValue.none())
					{
						if(resolvedCall == null)
							receiver = generateThisOrOuter((ClassDescriptor) propertyDescriptor.getContainingDeclaration(), false);
						else
							receiver = generateThisOrOuter((ClassDescriptor) propertyDescriptor.getContainingDeclaration(), false);
					}
					JetType receiverType = bindingTrace.get(BindingContext.EXPRESSION_TYPE, r);
					receiver.put(receiverType != null && !isSuper ? TypeTransformer.toAsmType(receiverType) : TypeConstants.ANY, instructs);
				}
			}
			return iValue;
		}

		if(descriptor instanceof ClassDescriptor)
			return StackValue.none();

		throw new UnsupportedOperationException();
	}

	@Override
	public StackValue visitClassOfExpression(NapileClassOfExpression classOfExpression, StackValue data)
	{
		TypeNode typeNode = expressionType(classOfExpression);

		instructs.classOf(TypeTransformer.toAsmType(bindingTrace.safeGet(BindingContext.TYPE, classOfExpression.getTypeReference())));

		return StackValue.onStack(typeNode);
	}

	@Override
	public StackValue visitTypeOfExpression(NapileTypeOfExpression typeOfExpression, StackValue data)
	{
		TypeNode typeNode = expressionType(typeOfExpression);

		instructs.typeOf(TypeTransformer.toAsmType(bindingTrace.safeGet(BindingContext.TYPE, typeOfExpression.getTypeReference())));

		return StackValue.onStack(typeNode);
	}

	public StackValue intermediateValueForProperty(PropertyDescriptor propertyDescriptor, final boolean forceField, @Nullable NapileSuperExpression superExpression)
	{
		boolean isSuper = superExpression != null;

		//TODO [VISTALL] super?

		if(!forceField)
			return StackValue.property(DescriptorUtils.getFQName(propertyDescriptor).toSafe(), TypeTransformer.toAsmType(propertyDescriptor.getType()), propertyDescriptor.isStatic());
		else
			return StackValue.variable(DescriptorUtils.getFQName(propertyDescriptor).toSafe(), TypeTransformer.toAsmType(propertyDescriptor.getType()), propertyDescriptor.isStatic());
	}

	private StackValue generateConstructorCall(NapileCallExpression expression, NapileSimpleNameExpression constructorReference, StackValue receiver)
	{
		DeclarationDescriptor constructorDescriptor = bindingTrace.safeGet(BindingContext.REFERENCE_TARGET, constructorReference);

		//final PsiElement declaration = BindingContextUtils.descriptorToDeclaration(bindingTrace.getBindingContext(), constructorDescriptor);
		TypeNode type;
		if(constructorDescriptor instanceof ConstructorDescriptor)
		{
			//noinspection ConstantConditions
			JetType expressionType = bindingTrace.safeGet(BindingContext.EXPRESSION_TYPE, expression);

			type = TypeTransformer.toAsmType(expressionType);

			//final ClassDescriptor classDescriptor = ((ConstructorDescriptor) constructorDescriptor).getContainingDeclaration();

			CallableMethod method = CallTransformer.transformToCallable((ConstructorDescriptor) constructorDescriptor, Collections.<TypeNode>emptyList());

			receiver.put(receiver.getType(), instructs);

			ResolvedCall<? extends CallableDescriptor> resolvedCall = bindingTrace.safeGet(BindingContext.RESOLVED_CALL, expression.getCalleeExpression());

			pushMethodArguments(resolvedCall, method.getValueParameterTypes());

			instructs.newObject(type, method.getValueParameterTypes());
		}
		else
		{
			throw new UnsupportedOperationException("don't know how to generate this new expression");
		}
		return StackValue.onStack(type);
	}

	private StackValue invokeOperation(NapileOperationExpression expression, MethodDescriptor op, CallableMethod callable)
	{
		int functionLocalIndex = lookupLocalIndex(op);
		if(functionLocalIndex >= 0)
			throw new UnsupportedOperationException();
		//stackValueForLocal(op, functionLocalIndex).put(getInternalClassName(op).getAsmType(), v);

		ResolvedCall<? extends CallableDescriptor> resolvedCall = bindingTrace.safeGet(BindingContext.RESOLVED_CALL, expression.getOperationReference());

		genThisAndReceiverFromResolvedCall(StackValue.none(), resolvedCall, callable);
		pushMethodArguments(resolvedCall, callable.getValueParameterTypes());
		callable.invoke(instructs);

		return StackValue.onStack(callable.getReturnType());
	}

	private StackValue invokeFunction(StackValue receiver, ResolvedCall<? extends CallableDescriptor> resolvedCall)
	{
		final CallableMethod callableMethod = CallTransformer.transformToCallable(resolvedCall);

		invokeMethodWithArguments(callableMethod, resolvedCall, receiver);

		final TypeNode callReturnType = callableMethod.getReturnType();

		return StackValue.onStack(callReturnType);
	}

	private void doFinallyOnReturn()
	{
		//TODO [VISTALL] make it
	}

	public void invokeMethodWithArguments(@NotNull CallableMethod callableMethod, NapileCallElement expression, StackValue receiver)
	{
		NapileExpression calleeExpression = expression.getCalleeExpression();

		ResolvedCall<? extends CallableDescriptor> resolvedCall = bindingTrace.safeGet(BindingContext.RESOLVED_CALL, calleeExpression);

		invokeMethodWithArguments(callableMethod, resolvedCall, receiver);
	}

	public void invokeMethodWithArguments(@NotNull CallableMethod callableMethod, @NotNull ResolvedCall<? extends CallableDescriptor> resolvedCall, @NotNull StackValue receiver)
	{
		/*final Type calleeType = callableMethod.getGenerateCalleeType();
		if(calleeType != null)
		{
			assert !callableMethod.isNeedsThis();
			gen(call.getCalleeExpression(), calleeType);
		}   */

		if(resolvedCall instanceof VariableAsFunctionResolvedCall)
			resolvedCall = ((VariableAsFunctionResolvedCall) resolvedCall).getFunctionCall();

		if(!(resolvedCall.getResultingDescriptor() instanceof ConstructorDescriptor))  // otherwise already
		{
			receiver = StackValue.receiver(resolvedCall, receiver, this, callableMethod);
			receiver.put(receiver.getType(), instructs);
			/*if(calleeType != null)
			{
				StackValue.onStack(receiver.type).put(boxType(receiver.type), v);
			}    */
		}

		int mask = pushMethodArguments(resolvedCall, callableMethod.getValueParameterTypes());
		if(mask == 0)
			callableMethod.invoke(instructs);
		else
			callableMethod.invokeWithDefault(instructs, mask);
	}

	private void genThisAndReceiverFromResolvedCall(StackValue receiver, ResolvedCall<? extends CallableDescriptor> resolvedCall, CallableMethod callableMethod)
	{
		receiver = StackValue.receiver(resolvedCall, receiver, this, callableMethod);
		receiver.put(receiver.getType(), instructs);
	}

	private StackValue stackValueForLocal(DeclarationDescriptor descriptor, int index)
	{
		if(descriptor instanceof VariableDescriptor)
		{
			final JetType outType = ((VariableDescriptor) descriptor).getType();

			return StackValue.local(index, TypeTransformer.toAsmType(outType));
		}
		else
			return StackValue.local(index, TypeConstants.ANY);
	}

	public int pushMethodArguments(@NotNull ResolvedCall<?> resolvedCall, List<TypeNode> valueParameterTypes)
	{
		List<ResolvedValueArgument> valueArguments = resolvedCall.getValueArgumentsByIndex();
		CallableDescriptor fd = resolvedCall.getResultingDescriptor();

		if(fd.getValueParameters().size() != valueArguments.size())
		{
			throw new IllegalStateException();
		}

		int index = 0;
		int mask = 0;

		for(ParameterDescriptor valueParameterDescriptor : fd.getValueParameters())
		{
			ResolvedValueArgument resolvedValueArgument = valueArguments.get(valueParameterDescriptor.getIndex());
			if(resolvedValueArgument instanceof ExpressionValueArgument)
			{
				ExpressionValueArgument valueArgument = (ExpressionValueArgument) resolvedValueArgument;
				//noinspection ConstantConditions
				gen(valueArgument.getValueArgument().getArgumentExpression(), valueParameterTypes.get(index));
			}
			/*else if(resolvedValueArgument instanceof DefaultValueArgument)
			{
				Type type = valueParameterTypes.get(index);
				if(type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY)
				{
					v.aconst(null);
				}
				else if(type.getSort() == Type.FLOAT)
				{
					v.aconst(0f);
				}
				else if(type.getSort() == Type.DOUBLE)
				{
					v.aconst(0d);
				}
				else if(type.getSort() == Type.LONG)
				{
					v.aconst(0l);
				}
				else
				{
					v.iconst(0);
				}
				mask |= (1 << index);
			}    */
			/*else if(resolvedValueArgument instanceof VarargValueArgument)
			{
				VarargValueArgument valueArgument = (VarargValueArgument) resolvedValueArgument;

				genVarargs(valueParameterDescriptor, valueArgument);
			}  */
			else
			{
				throw new UnsupportedOperationException();
			}
			index++;
		}
		return mask;
	}

	private StackValue generateBlock(List<NapileElement> statements)
	{
		List<Function<StackValue, Void>> leaveTasks = Lists.newArrayList();

		StackValue answer = StackValue.none();

		for(Iterator<NapileElement> iterator = statements.iterator(); iterator.hasNext(); )
		{
			NapileElement statement = iterator.next();

			if(statement instanceof NapileVariable)
				generateLocalVariableDeclaration((NapileVariable) statement, leaveTasks);

			if(!iterator.hasNext())
				answer = gen(statement);
			else
				gen(statement, TypeConstants.NULL);
		}

		for(Function<StackValue, Void> task : Lists.reverse(leaveTasks))
			task.fun(answer);

		return answer;
	}

	private <E extends NapileLoopExpression> StackValue loopGen(LoopCodegen<E> l)
	{
		loops.add(l);

		l.gen(this, instructs, bindingTrace);

		loops.getLast();

		return StackValue.none();
	}

	@Nullable
	private static NapileExpression getReceiverForSelector(PsiElement expression)
	{
		if(expression.getParent() instanceof NapileDotQualifiedExpression && !isReceiver(expression))
		{
			final NapileDotQualifiedExpression parent = (NapileDotQualifiedExpression) expression.getParent();
			return parent.getReceiverExpression();
		}
		return null;
	}

	private static boolean isReceiver(PsiElement expression)
	{
		final PsiElement parent = expression.getParent();
		if(parent instanceof NapileQualifiedExpression)
		{
			final NapileExpression receiverExpression = ((NapileQualifiedExpression) parent).getReceiverExpression();
			return expression == receiverExpression;
		}
		return false;
	}

	public void generateFromResolvedCall(@NotNull ReceiverDescriptor descriptor, @NotNull TypeNode type)
	{
		if(descriptor instanceof ClassReceiver)
		{
			TypeNode exprType = TypeTransformer.toAsmType(descriptor.getType());
			ClassReceiver classReceiver = (ClassReceiver) descriptor;
			ClassDescriptor classReceiverDeclarationDescriptor = classReceiver.getDeclarationDescriptor();

			StackValue.thisOrOuter(this, classReceiverDeclarationDescriptor, false).put(type, instructs);
		}
		else if(descriptor instanceof ExpressionReceiver)
		{
			ExpressionReceiver expressionReceiver = (ExpressionReceiver) descriptor;
			NapileExpression expr = expressionReceiver.getExpression();
			gen(expr, type);
		}
		else if(descriptor instanceof AutoCastReceiver)
		{
			AutoCastReceiver autoCastReceiver = (AutoCastReceiver) descriptor;
			TypeNode intermediateType = TypeTransformer.toAsmType(autoCastReceiver.getType());
			generateFromResolvedCall(autoCastReceiver.getOriginal(), intermediateType);
			StackValue.onStack(intermediateType).put(type, instructs);
		}
		else
		{
			throw new UnsupportedOperationException("Unsupported receiver type: " + descriptor);
		}
	}

	public StackValue generateThisOrOuter(@NotNull final ClassDescriptor calleeContainingClass, boolean isSuper)
	{
		//TODO [VISTALL]
		return StackValue.local(0, TypeTransformer.toAsmType(calleeContainingClass.getDefaultType()));
	}

	private void generateLocalVariableDeclaration(@NotNull final NapileVariable variableDeclaration, @NotNull List<Function<StackValue, Void>> leaveTasks)
	{
		final VariableDescriptor variableDescriptor = bindingTrace.get(BindingContext.VARIABLE, variableDeclaration);
		assert variableDescriptor != null;

		final TypeNode type = TypeTransformer.toAsmType(variableDescriptor.getType());
		int index = frameMap.enter(variableDescriptor);

		leaveTasks.add(new Function<StackValue, Void>()
		{
			@Override
			public Void fun(StackValue answer)
			{
				int index = frameMap.leave(variableDescriptor);

				getInstructs().visitLocalVariable(variableDescriptor.getName().getName());
				return null;
			}
		});
	}

	private void initializeLocalVariable(@NotNull NapileVariable variableDeclaration, @NotNull Function<VariableDescriptor, Void> generateInitializer)
	{
		VariableDescriptor variableDescriptor = bindingTrace.safeGet(BindingContext.VARIABLE, variableDeclaration);

		int index = lookupLocalIndex(variableDescriptor);

		if(index < 0)
			throw new IllegalStateException("Local variable not found for " + variableDescriptor);

		generateInitializer.fun(variableDescriptor);

		getInstructs().store(index);
	}

	public int lookupLocalIndex(DeclarationDescriptor descriptor)
	{
		return frameMap.getIndex(descriptor);
	}

	public void gen(NapileElement expr, TypeNode type)
	{
		StackValue value = gen(expr);
		value.put(type, getInstructs());
	}

	public StackValue gen(NapileElement element)
	{
		StackValue tempVar = tempVariables.get(element);
		if(tempVar != null)
			return tempVar;

		if(element instanceof NapileExpression)
		{
			NapileExpression expression = (NapileExpression) element;
			CompileTimeConstant<?> constant = bindingTrace.get(BindingContext.COMPILE_TIME_VALUE, expression);
			if(constant != null)
				return StackValue.constant(constant.getValue(), expressionType(expression));
		}

		return genQualified(StackValue.none(), element);
	}

	public StackValue genQualified(StackValue receiver, NapileElement selector)
	{
		if(tempVariables.containsKey(selector))
		{
			throw new IllegalStateException("Inconsistent state: expression saved to a temporary variable is a selector");
		}

		//if(!(selector instanceof NapileBlockExpression))
		//	markLineNumber(selector);

		try
		{
			return selector.accept(this, receiver);
		}
		catch(ProcessCanceledException e)
		{
			throw e;
		}
		catch(CompilationException e)
		{
			throw e;
		}
		catch(Throwable error)
		{
			String message = error.getMessage();
			throw new CompilationException(message != null ? message : "null", error, selector);
		}
	}

	public void returnExpression(@NotNull NapileExpression expr)
	{
		StackValue lastValue = gen(expr);

		if(!lastValue.getType().equals(TypeConstants.NULL))
		{
			lastValue.put(returnType, instructs);
			instructs.returnVal();
		}
		else if(!endsWithReturn(expr))
		{
			if(isInstanceConstructor)
				instructs.load(0);
			else
				instructs.putNull();
			instructs.returnVal();
		}
	}

	private static boolean endsWithReturn(NapileElement bodyExpression)
	{
		if(bodyExpression instanceof NapileBlockExpression)
		{
			final List<NapileElement> statements = ((NapileBlockExpression) bodyExpression).getStatements();
			return statements.size() > 0 && statements.get(statements.size() - 1) instanceof NapileReturnExpression;
		}

		return bodyExpression instanceof NapileReturnExpression;
	}

	private static boolean isEmptyExpression(NapileElement expr)
	{
		if(expr == null)
			return true;
		if(expr instanceof NapileBlockExpression)
		{
			NapileBlockExpression blockExpression = (NapileBlockExpression) expr;
			List<NapileElement> statements = blockExpression.getStatements();
			if(statements.size() == 0 || statements.size() == 1 && isEmptyExpression(statements.get(0)))
				return true;
		}
		return false;
	}

	@NotNull
	public TypeNode expressionType(NapileExpression expr)
	{
		JetType type = bindingTrace.get(BindingContext.EXPRESSION_TYPE, expr);
		return type == null ? StackValue.none().getType() : TypeTransformer.toAsmType(type);
	}

	@NotNull
	public InstructionAdapter getInstructs()
	{
		return instructs;
	}
}
