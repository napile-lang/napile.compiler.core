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

package org.napile.compiler.lang.types.expressions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.psi.Call;
import org.napile.compiler.lang.psi.NapilePattern;
import org.napile.compiler.lang.psi.NapileReferenceExpression;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.calls.BasicResolutionContext;
import org.napile.compiler.lang.resolve.calls.CallMaker;
import org.napile.compiler.lang.resolve.calls.OverloadResolutionResults;
import org.napile.compiler.lang.resolve.calls.autocasts.DataFlowInfo;
import org.napile.compiler.lang.resolve.constants.CompileTimeConstantResolver;
import org.napile.compiler.lang.resolve.scopes.NapileScope;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.lang.types.TypeUtils;
import com.intellij.lang.ASTNode;

/**
 * @author abreslav
 */
public class ExpressionTypingContext
{

	@NotNull
	public static ExpressionTypingContext newContext(@NotNull ExpressionTypingServices expressionTypingServices, @NotNull BindingTrace trace, @NotNull NapileScope scope, @NotNull DataFlowInfo dataFlowInfo, @NotNull NapileType expectedType, boolean namespacesAllowed)
	{
		return newContext(expressionTypingServices, new HashMap<NapilePattern, DataFlowInfo>(), new HashMap<NapilePattern, List<VariableDescriptor>>(), new LabelResolver(), trace, scope, dataFlowInfo, expectedType, namespacesAllowed);
	}

	@NotNull
	public static ExpressionTypingContext newContext(@NotNull ExpressionTypingServices expressionTypingServices, @NotNull Map<NapilePattern, DataFlowInfo> patternsToDataFlowInfo, @NotNull Map<NapilePattern, List<VariableDescriptor>> patternsToBoundVariableLists, @NotNull LabelResolver labelResolver, @NotNull BindingTrace trace, @NotNull NapileScope scope, @NotNull DataFlowInfo dataFlowInfo, @NotNull NapileType expectedType, boolean namespacesAllowed)
	{
		return new ExpressionTypingContext(expressionTypingServices, patternsToDataFlowInfo, patternsToBoundVariableLists, labelResolver, trace, scope, dataFlowInfo, expectedType, namespacesAllowed);
	}

	public final ExpressionTypingServices expressionTypingServices;
	public final BindingTrace trace;
	public final NapileScope scope;

	public final DataFlowInfo dataFlowInfo;
	public final NapileType expectedType;

	public final Map<NapilePattern, DataFlowInfo> patternsToDataFlowInfo;
	public final Map<NapilePattern, List<VariableDescriptor>> patternsToBoundVariableLists;
	public final LabelResolver labelResolver;

	// true for positions on the lhs of a '.', i.e. allows namespace results and 'super'
	public final boolean namespacesAllowed;

	private CompileTimeConstantResolver compileTimeConstantResolver;

	private ExpressionTypingContext(@NotNull ExpressionTypingServices expressionTypingServices, @NotNull Map<NapilePattern, DataFlowInfo> patternsToDataFlowInfo, @NotNull Map<NapilePattern, List<VariableDescriptor>> patternsToBoundVariableLists, @NotNull LabelResolver labelResolver, @NotNull BindingTrace trace, @NotNull NapileScope scope, @NotNull DataFlowInfo dataFlowInfo, @NotNull NapileType expectedType, boolean namespacesAllowed)
	{
		this.expressionTypingServices = expressionTypingServices;
		this.trace = trace;
		this.patternsToBoundVariableLists = patternsToBoundVariableLists;
		this.patternsToDataFlowInfo = patternsToDataFlowInfo;
		this.labelResolver = labelResolver;
		this.scope = scope;
		this.dataFlowInfo = dataFlowInfo;
		this.expectedType = expectedType;
		this.namespacesAllowed = namespacesAllowed;
	}

	@NotNull
	public ExpressionTypingContext replaceNamespacesAllowed(boolean namespacesAllowed)
	{
		if(namespacesAllowed == this.namespacesAllowed)
			return this;
		return newContext(expressionTypingServices, patternsToDataFlowInfo, patternsToBoundVariableLists, labelResolver, trace, scope, dataFlowInfo, expectedType, namespacesAllowed);
	}

	@NotNull
	public ExpressionTypingContext replaceDataFlowInfo(DataFlowInfo newDataFlowInfo)
	{
		if(newDataFlowInfo == dataFlowInfo)
			return this;
		return newContext(expressionTypingServices, patternsToDataFlowInfo, patternsToBoundVariableLists, labelResolver, trace, scope, newDataFlowInfo, expectedType, namespacesAllowed);
	}

	@NotNull
	public ExpressionTypingContext replaceExpectedType(@Nullable NapileType newExpectedType)
	{
		if(newExpectedType == null)
			return replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
		if(expectedType == newExpectedType)
			return this;
		return newContext(expressionTypingServices, patternsToDataFlowInfo, patternsToBoundVariableLists, labelResolver, trace, scope, dataFlowInfo, newExpectedType, namespacesAllowed);
	}

	@NotNull
	public ExpressionTypingContext replaceBindingTrace(@NotNull BindingTrace newTrace)
	{
		if(newTrace == trace)
			return this;
		return newContext(expressionTypingServices, patternsToDataFlowInfo, patternsToBoundVariableLists, labelResolver, newTrace, scope, dataFlowInfo, expectedType, namespacesAllowed);
	}

	@NotNull
	public ExpressionTypingContext replaceScope(@NotNull NapileScope newScope)
	{
		if(newScope == scope)
			return this;
		return newContext(expressionTypingServices, patternsToDataFlowInfo, patternsToBoundVariableLists, labelResolver, trace, newScope, dataFlowInfo, expectedType, namespacesAllowed);
	}

	///////////// LAZY ACCESSORS

	public CompileTimeConstantResolver getCompileTimeConstantResolver()
	{
		if(compileTimeConstantResolver == null)
		{
			compileTimeConstantResolver = new CompileTimeConstantResolver(trace);
		}
		return compileTimeConstantResolver;
	}

	////////// Call resolution utilities

	private BasicResolutionContext makeResolutionContext(@NotNull Call call)
	{
		return BasicResolutionContext.create(trace, scope, call, expectedType, dataFlowInfo);
	}

	@NotNull
	public OverloadResolutionResults<MethodDescriptor> resolveCallWithGivenName(@NotNull Call call, @NotNull NapileReferenceExpression functionReference, @NotNull Name name, boolean bindReference)
	{
		return expressionTypingServices.getCallResolver().resolveCallWithGivenName(makeResolutionContext(call), functionReference, name, bindReference);
	}

	@NotNull
	public OverloadResolutionResults<MethodDescriptor> resolveCallWithGivenName(@NotNull Call call, @NotNull NapileReferenceExpression functionReference, @NotNull Name name)
	{
		return resolveCallWithGivenName(call, functionReference, name, true);
	}

	@NotNull
	public OverloadResolutionResults<MethodDescriptor> resolveFunctionCall(@NotNull Call call)
	{
		return expressionTypingServices.getCallResolver().resolveFunctionCall(makeResolutionContext(call));
	}

	@NotNull
	public OverloadResolutionResults<VariableDescriptor> resolveSimpleProperty(@NotNull ReceiverDescriptor receiver, @Nullable ASTNode callOperationNode, @NotNull NapileSimpleNameExpression nameExpression)
	{
		Call call = CallMaker.makeVariableCall(receiver, callOperationNode, nameExpression);
		return expressionTypingServices.getCallResolver().resolveSimpleProperty(makeResolutionContext(call));
	}

	@NotNull
	public OverloadResolutionResults<MethodDescriptor> resolveExactSignature(@NotNull ReceiverDescriptor receiver, @NotNull Name name, @NotNull List<NapileType> parameterTypes)
	{
		return expressionTypingServices.getCallResolver().resolveExactSignature(scope, receiver, name, parameterTypes);
	}
}
