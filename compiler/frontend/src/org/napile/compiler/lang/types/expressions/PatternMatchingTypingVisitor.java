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

import static org.napile.compiler.lang.diagnostics.Errors.EXPECTED_CONDITION;
import static org.napile.compiler.lang.diagnostics.Errors.TYPE_MISMATCH_IN_RANGE;
import static org.napile.compiler.lang.diagnostics.Errors.UNSUPPORTED;
import static org.napile.compiler.lang.types.expressions.ExpressionTypingUtils.newWritableScopeImpl;

import java.util.Collections;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.diagnostics.Errors;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.calls.autocasts.DataFlowInfo;
import org.napile.compiler.lang.resolve.calls.autocasts.DataFlowValue;
import org.napile.compiler.lang.resolve.calls.autocasts.DataFlowValueFactory;
import org.napile.compiler.lang.resolve.calls.autocasts.Nullability;
import org.napile.compiler.lang.resolve.scopes.WritableScope;
import org.napile.compiler.lang.rt.NapileLangPackage;
import org.napile.compiler.lang.types.CommonSupertypes;
import org.napile.compiler.lang.types.ErrorUtils;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.JetTypeInfo;
import org.napile.compiler.lang.types.TypeUtils;
import org.napile.compiler.lang.types.checker.JetTypeChecker;
import com.google.common.collect.Sets;
import com.intellij.openapi.util.Ref;

/**
 * @author abreslav
 */
public class PatternMatchingTypingVisitor extends ExpressionTypingVisitor
{
	protected PatternMatchingTypingVisitor(@NotNull ExpressionTypingInternals facade)
	{
		super(facade);
	}

	@Override
	public JetTypeInfo visitIsExpression(NapileIsExpression expression, ExpressionTypingContext contextWithExpectedType)
	{
		ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
		NapileExpression leftHandSide = expression.getLeftHandSide();
		JetType knownType = facade.safeGetTypeInfo(leftHandSide, context.replaceScope(context.scope)).getType();

		if(expression.getTypeRef() != null)
		{
			DataFlowValue dataFlowValue = DataFlowValueFactory.INSTANCE.createDataFlowValue(leftHandSide, knownType, context.trace.getBindingContext());
			DataFlowInfo newDataFlowInfo = checkTypeForIs(context, knownType, expression.getTypeRef(), dataFlowValue).thenInfo;
			context.trace.record(BindingContext.DATAFLOW_INFO_AFTER_CONDITION, expression, newDataFlowInfo);
		}
		return DataFlowUtils.checkType(TypeUtils.getTypeOfClassOrErrorType(context.scope, NapileLangPackage.BOOL, false), expression, contextWithExpectedType, context.dataFlowInfo);
	}

	@Override
	public JetTypeInfo visitWhenExpression(final NapileWhenExpression expression, ExpressionTypingContext context)
	{
		return visitWhenExpression(expression, context, false);
	}

	public JetTypeInfo visitWhenExpression(final NapileWhenExpression expression, ExpressionTypingContext contextWithExpectedType, boolean isStatement)
	{
		ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
		// TODO :change scope according to the bound value in the when header
		final NapileExpression subjectExpression = expression.getSubjectExpression();

		final JetType subjectType = subjectExpression != null ? context.expressionTypingServices.safeGetType(context.scope, subjectExpression, TypeUtils.NO_EXPECTED_TYPE, context.dataFlowInfo, context.trace) : ErrorUtils.createErrorType("Unknown type");
		final DataFlowValue variableDescriptor = subjectExpression != null ? DataFlowValueFactory.INSTANCE.createDataFlowValue(subjectExpression, subjectType, context.trace.getBindingContext()) : new DataFlowValue(new Object(), TypeUtils.getTypeOfClassOrErrorType(context.scope, NapileLangPackage.NULL, true), false, Nullability.NULL);

		// TODO : exhaustive patterns

		Set<JetType> expressionTypes = Sets.newHashSet();
		DataFlowInfo commonDataFlowInfo = null;
		DataFlowInfo elseDataFlowInfo = context.dataFlowInfo;
		for(NapileWhenEntry whenEntry : expression.getEntries())
		{
			NapileWhenCondition[] conditions = whenEntry.getConditions();
			DataFlowInfo newDataFlowInfo;
			WritableScope scopeToExtend;
			if(whenEntry.isElse())
			{
				scopeToExtend = newWritableScopeImpl(context, "Scope extended in when-else entry");
				newDataFlowInfo = elseDataFlowInfo;
			}
			else if(conditions.length == 1)
			{
				scopeToExtend = newWritableScopeImpl(context, "Scope extended in when entry");
				newDataFlowInfo = context.dataFlowInfo;
				NapileWhenCondition condition = conditions[0];
				if(condition != null)
				{
					DataFlowInfos infos = checkWhenCondition(subjectExpression, subjectExpression == null, subjectType, condition, scopeToExtend, context, variableDescriptor);
					newDataFlowInfo = infos.thenInfo;
					elseDataFlowInfo = elseDataFlowInfo.and(infos.elseInfo);
				}
			}
			else
			{
				scopeToExtend = newWritableScopeImpl(context, "pattern matching"); // We don't write to this scope
				newDataFlowInfo = null;
				for(NapileWhenCondition condition : conditions)
				{
					DataFlowInfos infos = checkWhenCondition(subjectExpression, subjectExpression == null, subjectType, condition, newWritableScopeImpl(context, ""), context, variableDescriptor);
					if(newDataFlowInfo == null)
					{
						newDataFlowInfo = infos.thenInfo;
					}
					else
					{
						newDataFlowInfo = newDataFlowInfo.or(infos.thenInfo);
					}
					elseDataFlowInfo = elseDataFlowInfo.and(infos.elseInfo);
				}
				if(newDataFlowInfo == null)
				{
					newDataFlowInfo = context.dataFlowInfo;
				}
			}
			NapileExpression bodyExpression = whenEntry.getExpression();
			if(bodyExpression != null)
			{
				ExpressionTypingContext newContext = contextWithExpectedType.replaceScope(scopeToExtend).replaceDataFlowInfo(newDataFlowInfo);
				CoercionStrategy coercionStrategy = isStatement ? CoercionStrategy.COERCION_TO_UNIT : CoercionStrategy.NO_COERCION;
				JetTypeInfo typeInfo = context.expressionTypingServices.getBlockReturnedTypeWithWritableScope(scopeToExtend, Collections.singletonList(bodyExpression), coercionStrategy, newContext, context.trace);
				JetType type = typeInfo.getType();
				if(type != null)
				{
					expressionTypes.add(type);
				}
				if(commonDataFlowInfo == null)
				{
					commonDataFlowInfo = typeInfo.getDataFlowInfo();
				}
				else
				{
					commonDataFlowInfo = commonDataFlowInfo.or(typeInfo.getDataFlowInfo());
				}
			}
		}

		if(commonDataFlowInfo == null)
		{
			commonDataFlowInfo = context.dataFlowInfo;
		}

		if(!expressionTypes.isEmpty())
		{
			return DataFlowUtils.checkImplicitCast(CommonSupertypes.commonSupertype(expressionTypes), expression, contextWithExpectedType, isStatement, commonDataFlowInfo);
		}
		return JetTypeInfo.create(null, commonDataFlowInfo);
	}

	private DataFlowInfos checkWhenCondition(@Nullable final NapileExpression subjectExpression, final boolean expectedCondition, final JetType subjectType, NapileWhenCondition condition, final WritableScope scopeToExtend, final ExpressionTypingContext context, final DataFlowValue... subjectVariables)
	{
		final Ref<DataFlowInfos> newDataFlowInfo = new Ref<DataFlowInfos>(noChange(context));
		condition.accept(new NapileVisitorVoid()
		{

			@Override
			public void visitWhenConditionInRange(NapileWhenConditionInRange condition)
			{
				NapileExpression rangeExpression = condition.getRangeExpression();
				if(rangeExpression == null)
					return;
				if(expectedCondition)
				{
					context.trace.report(EXPECTED_CONDITION.on(condition));
					facade.getTypeInfo(rangeExpression, context);
					return;
				}
				if(!facade.checkInExpression(condition, condition.getOperationReference(), subjectExpression, rangeExpression, context))
				{
					context.trace.report(TYPE_MISMATCH_IN_RANGE.on(condition));
				}
			}

			@Override
			public void visitWhenConditionIsPattern(NapileWhenConditionIsPattern condition)
			{
				if(expectedCondition)
				{
					context.trace.report(EXPECTED_CONDITION.on(condition));
				}
				if(condition.getTypeRef() != null)
				{
					DataFlowInfos result = checkTypeForIs(context, subjectType, condition.getTypeRef(), subjectVariables);
					if(condition.isNegated())
					{
						newDataFlowInfo.set(new DataFlowInfos(result.elseInfo, result.thenInfo));
					}
					else
					{
						newDataFlowInfo.set(result);
					}
				}
			}

			@Override
			public void visitWhenConditionWithExpression(NapileWhenConditionWithExpression condition)
			{
				NapileExpression expression = condition.getExpression();
				if(expression != null)
				{
					newDataFlowInfo.set(checkTypeForExpressionCondition(context, expression, subjectType, subjectExpression == null, subjectVariables));
				}
			}

			@Override
			public void visitJetElement(NapileElement element)
			{
				context.trace.report(UNSUPPORTED.on(element, getClass().getCanonicalName()));
			}
		});
		return newDataFlowInfo.get();
	}

	private static class DataFlowInfos
	{
		private final DataFlowInfo thenInfo;
		private final DataFlowInfo elseInfo;

		private DataFlowInfos(DataFlowInfo thenInfo, DataFlowInfo elseInfo)
		{
			this.thenInfo = thenInfo;
			this.elseInfo = elseInfo;
		}
	}

	private static DataFlowInfos checkTypeForIs(ExpressionTypingContext context, JetType subjectType, NapileTypeReference typeReferenceAfterIs, DataFlowValue... subjectVariables)
	{
		if(typeReferenceAfterIs == null)
		{
			return noChange(context);
		}
		JetType type = context.expressionTypingServices.getTypeResolver().resolveType(context.scope, typeReferenceAfterIs, context.trace, true);
		checkTypeCompatibility(context, type, subjectType, typeReferenceAfterIs);
		if(BasicExpressionTypingVisitor.isCastErased(subjectType, type, JetTypeChecker.INSTANCE))
		{
			context.trace.report(Errors.CANNOT_CHECK_FOR_ERASED.on(typeReferenceAfterIs, type));
		}
		return new DataFlowInfos(context.dataFlowInfo.establishSubtyping(subjectVariables, type), context.dataFlowInfo);
	}

	private static DataFlowInfos noChange(ExpressionTypingContext context)
	{
		return new DataFlowInfos(context.dataFlowInfo, context.dataFlowInfo);
	}

	private DataFlowInfos checkTypeForExpressionCondition(ExpressionTypingContext context, NapileExpression expression, JetType subjectType, boolean conditionExpected, DataFlowValue... subjectVariables)
	{
		if(expression == null)
		{
			return noChange(context);
		}
		JetTypeInfo typeInfo = facade.getTypeInfo(expression, context);
		JetType type = typeInfo.getType();
		if(type == null)
			return noChange(context);
		if(conditionExpected)
		{
			if(!TypeUtils.isEqualFqName(type, NapileLangPackage.BOOL))
				context.trace.report(Errors.TYPE_MISMATCH_IN_CONDITION.on(expression, type));
			else
			{
				DataFlowInfo ifInfo = DataFlowUtils.extractDataFlowInfoFromCondition(expression, true, context);
				DataFlowInfo elseInfo = DataFlowUtils.extractDataFlowInfoFromCondition(expression, false, context);
				return new DataFlowInfos(ifInfo, elseInfo);
			}
			return noChange(context);
		}
		checkTypeCompatibility(context, type, subjectType, expression);
		DataFlowValue expressionDataFlowValue = DataFlowValueFactory.INSTANCE.createDataFlowValue(expression, type, context.trace.getBindingContext());
		DataFlowInfos result = noChange(context);
		for(DataFlowValue subjectVariable : subjectVariables)
		{
			result = new DataFlowInfos(result.thenInfo.equate(subjectVariable, expressionDataFlowValue), result.elseInfo.disequate(subjectVariable, expressionDataFlowValue));
		}
		return result;
	}

	private static void checkTypeCompatibility(@NotNull ExpressionTypingContext context, @Nullable JetType type, @NotNull JetType subjectType, @NotNull NapileElement reportErrorOn)
	{
		// TODO : Take auto casts into account?
		if(type == null)
		{
			return;
		}
		if(TypeUtils.isIntersectionEmpty(type, subjectType))
		{
			context.trace.report(Errors.INCOMPATIBLE_TYPES.on(reportErrorOn, type, subjectType));
			return;
		}

	}
}
