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

import static org.napile.compiler.lang.diagnostics.Errors.*;
import static org.napile.compiler.lang.resolve.BindingContext.EXPRESSION_TYPE;
import static org.napile.compiler.lang.resolve.BindingContext.INDEXED_LVALUE_GET;
import static org.napile.compiler.lang.resolve.BindingContext.INDEXED_LVALUE_SET;
import static org.napile.compiler.lang.resolve.BindingContext.NON_DEFAULT_EXPRESSION_DATA_FLOW;
import static org.napile.compiler.lang.resolve.BindingContext.REFERENCE_TARGET;
import static org.napile.compiler.lang.resolve.BindingContext.RESOLUTION_SCOPE;
import static org.napile.compiler.lang.resolve.BindingContext.RESOLVED_CALL;
import static org.napile.compiler.lang.types.expressions.OperatorConventions.BYTE;
import static org.napile.compiler.lang.types.expressions.OperatorConventions.DOUBLE;
import static org.napile.compiler.lang.types.expressions.OperatorConventions.FLOAT;
import static org.napile.compiler.lang.types.expressions.OperatorConventions.INT;
import static org.napile.compiler.lang.types.expressions.OperatorConventions.LONG;
import static org.napile.compiler.lang.types.expressions.OperatorConventions.SHORT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.lib.NapileConditionPackage;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.asm.lib.NapileReflectPackage;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.injection.CodeInjection;
import org.napile.compiler.lang.descriptors.*;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.diagnostics.Errors;
import org.napile.compiler.lang.lexer.NapileNodes;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingContextUtils;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.TemporaryBindingTrace;
import org.napile.compiler.lang.resolve.calls.CallMaker;
import org.napile.compiler.lang.resolve.calls.OverloadResolutionResults;
import org.napile.compiler.lang.resolve.calls.OverloadResolutionResultsUtil;
import org.napile.compiler.lang.resolve.calls.ResolutionStatus;
import org.napile.compiler.lang.resolve.calls.ResolvedCall;
import org.napile.compiler.lang.resolve.calls.ResolvedCallWithTrace;
import org.napile.compiler.lang.resolve.calls.autocasts.DataFlowInfo;
import org.napile.compiler.lang.resolve.calls.autocasts.DataFlowValue;
import org.napile.compiler.lang.resolve.calls.autocasts.DataFlowValueFactory;
import org.napile.compiler.lang.resolve.calls.autocasts.Nullability;
import org.napile.compiler.lang.resolve.constants.ByteValue;
import org.napile.compiler.lang.resolve.constants.CompileTimeConstant;
import org.napile.compiler.lang.resolve.constants.CompileTimeConstantResolver;
import org.napile.compiler.lang.resolve.constants.DoubleValue;
import org.napile.compiler.lang.resolve.constants.ErrorValue;
import org.napile.compiler.lang.resolve.constants.FloatValue;
import org.napile.compiler.lang.resolve.constants.IntValue;
import org.napile.compiler.lang.resolve.constants.LongValue;
import org.napile.compiler.lang.resolve.constants.ShortValue;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.resolve.scopes.WritableScopeImpl;
import org.napile.compiler.lang.resolve.scopes.receivers.ClassReceiver;
import org.napile.compiler.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.resolve.scopes.receivers.ThisReceiverDescriptor;
import org.napile.compiler.lang.types.CommonSupertypes;
import org.napile.compiler.lang.types.ErrorUtils;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.JetTypeInfo;
import org.napile.compiler.lang.types.NamespaceType;
import org.napile.compiler.lang.types.SubstitutionUtils;
import org.napile.compiler.lang.types.TypeConstructor;
import org.napile.compiler.lang.types.TypeSubstitutor;
import org.napile.compiler.lang.types.TypeUtils;
import org.napile.compiler.lang.types.checker.JetTypeChecker;
import org.napile.compiler.lang.types.impl.JetTypeImpl;
import org.napile.compiler.lang.types.impl.MethodTypeConstructorImpl;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;

/**
 * @author abreslav
 */
@SuppressWarnings("SuspiciousMethodCalls")
public class BasicExpressionTypingVisitor extends ExpressionTypingVisitor
{
	protected BasicExpressionTypingVisitor(@NotNull ExpressionTypingInternals facade)
	{
		super(facade);
	}

	@Override
	public JetTypeInfo visitSimpleNameExpression(NapileSimpleNameExpression expression, ExpressionTypingContext context)
	{
		JetTypeInfo typeInfo = getSelectorReturnTypeInfo(ReceiverDescriptor.NO_RECEIVER, null, expression, context);
		JetType type = DataFlowUtils.checkType(typeInfo.getType(), expression, context);
		ExpressionTypingUtils.checkWrappingInRef(expression, context);
		return JetTypeInfo.create(type, typeInfo.getDataFlowInfo());
	}

	@Nullable
	private JetType lookupNamespaceOrClassObject(NapileSimpleNameExpression expression, Name referencedName, ExpressionTypingContext context)
	{
		ClassifierDescriptor classifier = context.scope.getClassifier(referencedName);
		if(classifier != null)
		{
			if(!context.namespacesAllowed)
				context.trace.report(NO_CLASS_OBJECT.on(expression, classifier));

			context.trace.record(REFERENCE_TARGET, expression, classifier);

			return DataFlowUtils.checkType(classifier.getDefaultType(), expression, context);
		}
		JetType[] result = new JetType[1];
		TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(context.trace);
		if(furtherNameLookup(expression, referencedName, result, context.replaceBindingTrace(temporaryTrace)))
		{
			temporaryTrace.commit();
			return DataFlowUtils.checkType(result[0], expression, context);
		}
		// To report NO_CLASS_OBJECT when no namespace found
		if(classifier != null)
		{
			context.trace.report(NO_CLASS_OBJECT.on(expression, classifier));
			context.trace.record(REFERENCE_TARGET, expression, classifier);
			return classifier.getDefaultType();
		}
		temporaryTrace.commit();
		return result[0];
	}

	protected boolean furtherNameLookup(@NotNull NapileSimpleNameExpression expression, @NotNull Name referencedName, @NotNull JetType[] result, ExpressionTypingContext context)
	{
		if(context.namespacesAllowed)
		{
			result[0] = lookupNamespaceType(expression, referencedName, context);
			return result[0] != null;
		}
		NamespaceType namespaceType = lookupNamespaceType(expression, referencedName, context);
		if(namespaceType != null)
		{
			context.trace.report(EXPRESSION_EXPECTED_NAMESPACE_FOUND.on(expression));
			result[0] = ErrorUtils.createErrorType("Type for " + referencedName);
		}
		return false;
	}

	@Nullable
	protected NamespaceType lookupNamespaceType(@NotNull NapileSimpleNameExpression expression, @NotNull Name referencedName, ExpressionTypingContext context)
	{
		PackageDescriptor namespace = context.scope.getPackage(referencedName);
		if(namespace == null)
		{
			return null;
		}
		context.trace.record(REFERENCE_TARGET, expression, namespace);
		return namespace.getNamespaceType();
	}

	@Override
	public JetTypeInfo visitParenthesizedExpression(NapileParenthesizedExpression expression, ExpressionTypingContext context)
	{
		return visitParenthesizedExpression(expression, context, false);
	}

	public JetTypeInfo visitParenthesizedExpression(NapileParenthesizedExpression expression, ExpressionTypingContext context, boolean isStatement)
	{
		NapileExpression innerExpression = expression.getExpression();
		if(innerExpression == null)
		{
			return JetTypeInfo.create(null, context.dataFlowInfo);
		}
		JetTypeInfo typeInfo = facade.getTypeInfo(innerExpression, context.replaceScope(context.scope), isStatement);
		return DataFlowUtils.checkType(typeInfo.getType(), expression, context, typeInfo.getDataFlowInfo());
	}

	@Override
	public JetTypeInfo visitLabelExpression(NapileLabelExpression expression, ExpressionTypingContext context)
	{
		NapileExpression blockExpression = expression.getBody();
		if(blockExpression == null)
			return JetTypeInfo.create(null, context.dataFlowInfo);

		context.labelResolver.enterLabeledElement(Name.identifier(expression.getLabelName()), expression);

		facade.getTypeInfo(blockExpression, context, false);

		context.labelResolver.exitLabeledElement(expression);

		return JetTypeInfo.create(null, context.dataFlowInfo);
	}

	@Override
	public JetTypeInfo visitClassOfExpression(NapileClassOfExpression expression, ExpressionTypingContext context)
	{
		NapileTypeReference typeReference = expression.getTypeReference();
		if(typeReference == null)
			return DataFlowUtils.checkType(ErrorUtils.createErrorType("Type expected"), expression, context, context.dataFlowInfo);

		ClassDescriptor classDescriptor = context.scope.getClass(NapileReflectPackage.CLASS);
		if(classDescriptor == null)
		{
			context.trace.report(Errors.NAPILE_LANG_CLASS_IMPORT_EXPECTED.on(expression));

			return JetTypeInfo.create(null, context.dataFlowInfo);
		}

		JetType targetType = context.expressionTypingServices.getTypeResolver().resolveType(context.scope, typeReference, context.trace, true);

		JetType returnType = new JetTypeImpl(Collections.<AnnotationDescriptor>emptyList(), classDescriptor.getTypeConstructor(), false, Collections.<JetType>singletonList(targetType), classDescriptor.getMemberScope(Collections.singletonList(targetType)));

		return DataFlowUtils.checkType(returnType, expression, context, context.dataFlowInfo);
	}

	@Override
	public JetTypeInfo visitTypeOfExpression(NapileTypeOfExpression expression, ExpressionTypingContext context)
	{
		NapileTypeReference typeReference = expression.getTypeReference();
		if(typeReference == null)
			return DataFlowUtils.checkType(ErrorUtils.createErrorType("Type expected"), expression, context, context.dataFlowInfo);

		ClassDescriptor classDescriptor = context.scope.getClass(NapileReflectPackage.TYPE);
		if(classDescriptor == null)
		{
			context.trace.report(Errors.NAPILE_LANG_TYPE_IMPORT_EXPECTED.on(expression));

			return JetTypeInfo.create(null, context.dataFlowInfo);
		}

		JetType targetType = context.expressionTypingServices.getTypeResolver().resolveType(context.scope, typeReference, context.trace, true);

		JetType returnType = new JetTypeImpl(Collections.<AnnotationDescriptor>emptyList(), classDescriptor.getTypeConstructor(), false, Collections.<JetType>singletonList(targetType), classDescriptor.getMemberScope(Collections.singletonList(targetType)));

		return DataFlowUtils.checkType(returnType, expression, context, context.dataFlowInfo);
	}

	@Override
	public JetTypeInfo visitArrayOfExpression(NapileArrayOfExpression arrayExpression, ExpressionTypingContext context)
	{
		ClassDescriptor classDescriptor = context.scope.getClass(NapileLangPackage.ARRAY);
		if(classDescriptor == null)
			return JetTypeInfo.create(null, context.dataFlowInfo);

		NapileExpression[] expressions = arrayExpression.getValues();
		Set<JetType> types = new HashSet<JetType>(expressions.length);
		for(NapileExpression exp : expressions)
			types.add(context.expressionTypingServices.safeGetType(context.scope, exp, TypeUtils.NO_EXPECTED_TYPE, context.dataFlowInfo, context.trace));

		JetType typeArgument = TypeUtils.intersect(JetTypeChecker.INSTANCE, types, context.scope);
		JetType returnType = new JetTypeImpl(Collections.<AnnotationDescriptor>emptyList(), classDescriptor.getTypeConstructor(), false, Collections.<JetType>singletonList(typeArgument), classDescriptor.getMemberScope(Collections.singletonList(typeArgument)));

		return DataFlowUtils.checkType(returnType, arrayExpression, context, context.dataFlowInfo);
	}

	@Override
	public JetTypeInfo visitInjectionExpression(NapileInjectionExpression expression, final ExpressionTypingContext context)
	{
		CodeInjection codeInjection = expression.getCodeInjection();
		if(codeInjection == null)
		{
			context.trace.report(Errors.UNKNOWN_INJECTION.on(expression));
			return JetTypeInfo.create(null, context.dataFlowInfo);
		}

		PsiElement block = expression.getBlock();
		if(block != null)
			block.acceptChildren(new NapileVisitorVoid()
			{
				@Override
				public void visitElement(PsiElement exp)
				{
					if(exp instanceof NapileElement)
					{
						ExpressionTypingContext c = ExpressionTypingContext.newContext(context.expressionTypingServices, context.trace, context.scope, context.dataFlowInfo, TypeUtils.getTypeOfClassOrErrorType(context.scope, NapileLangPackage.ANY, true), false);
						((NapileElement) exp).accept(BasicExpressionTypingVisitor.this, c);
					}
					else
						exp.acceptChildren(this);
				}
			});

		return DataFlowUtils.checkType(codeInjection.getReturnType(context.expectedType, context.trace, context.scope), expression, context, context.dataFlowInfo);
	}

	@Override
	public JetTypeInfo visitConstantExpression(NapileConstantExpression expression, ExpressionTypingContext context)
	{
		ASTNode node = expression.getNode();
		IElementType elementType = node.getElementType();
		String text = node.getText();

		CompileTimeConstantResolver compileTimeConstantResolver = context.getCompileTimeConstantResolver();

		CompileTimeConstant<?> value;
		if(elementType == NapileNodes.INTEGER_CONSTANT)
		{
			value = compileTimeConstantResolver.getIntegerValue(text, context.expectedType);
		}
		else if(elementType == NapileNodes.FLOAT_CONSTANT)
		{
			value = compileTimeConstantResolver.getFloatValue(text, context.expectedType);
		}
		else if(elementType == NapileNodes.BOOLEAN_CONSTANT)
		{
			value = compileTimeConstantResolver.getBooleanValue(text);
		}
		else if(elementType == NapileNodes.CHARACTER_CONSTANT)
		{
			value = compileTimeConstantResolver.getCharValue(text, context.expectedType);
		}
		else if(elementType == NapileNodes.STRING_CONSTANT)
			value = compileTimeConstantResolver.getStringValue(text, context.expectedType);
		else if(elementType == NapileNodes.NULL)
		{
			value = compileTimeConstantResolver.getNullValue(context.expectedType);
		}
		else
		{
			throw new IllegalArgumentException("Unsupported constant: " + expression);
		}
		if(value instanceof ErrorValue)
		{
			ErrorValue errorValue = (ErrorValue) value;
			context.trace.report(ERROR_COMPILE_TIME_VALUE.on(node.getPsi(), errorValue.getMessage()));
			return JetTypeInfo.create(ExpressionTypingUtils.getDefaultType(elementType, context.scope), context.dataFlowInfo);
		}
		else
		{
			context.trace.record(BindingContext.COMPILE_TIME_VALUE, expression, value);

			return DataFlowUtils.checkType(value.getType(context.scope), expression, context, context.dataFlowInfo);
		}
	}

	@Override
	public JetTypeInfo visitBinaryWithTypeRHSExpression(NapileBinaryExpressionWithTypeRHS expression, ExpressionTypingContext context)
	{
		NapileExpression left = expression.getLeft();
		NapileTypeReference right = expression.getRight();
		JetType result = null;
		DataFlowInfo dataFlowInfo = context.dataFlowInfo;
		if(right != null)
		{
			JetType targetType = context.expressionTypingServices.getTypeResolver().resolveType(context.scope, right, context.trace, true);
			IElementType operationType = expression.getOperationSign().getReferencedNameElementType();

			boolean tryWithNoExpectedType = true;
			if(ExpressionTypingUtils.isTypeFlexible(left) || operationType == NapileTokens.COLON)
			{
				TemporaryBindingTrace temporaryTraceWithExpectedType = TemporaryBindingTrace.create(context.trace);
				ExpressionTypingContext contextWithTemporaryTrace = context.replaceBindingTrace(temporaryTraceWithExpectedType).replaceExpectedType(targetType);
				JetTypeInfo typeInfo = facade.getTypeInfo(left, contextWithTemporaryTrace);
				if(typeInfo.getType() != null && checkBinaryWithTypeRHS(expression, contextWithTemporaryTrace, targetType, typeInfo.getType()))
				{
					temporaryTraceWithExpectedType.commit();
					dataFlowInfo = typeInfo.getDataFlowInfo();
					tryWithNoExpectedType = false;
				}
			}

			if(tryWithNoExpectedType)
			{
				ExpressionTypingContext contextWithNoExpectedType = context.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
				JetTypeInfo typeInfo = facade.getTypeInfo(left, contextWithNoExpectedType);
				if(typeInfo.getType() != null)
				{
					checkBinaryWithTypeRHS(expression, contextWithNoExpectedType, targetType, typeInfo.getType());
					dataFlowInfo = typeInfo.getDataFlowInfo();
					if(operationType == NapileTokens.AS_KEYWORD)
					{
						DataFlowValue value = DataFlowValueFactory.INSTANCE.createDataFlowValue(left, typeInfo.getType(), context.trace.getBindingContext());
						dataFlowInfo = dataFlowInfo.establishSubtyping(new DataFlowValue[]{value}, targetType);
					}
				}
			}

			result = operationType == NapileTokens.AS_SAFE ? TypeUtils.makeNullable(targetType) : targetType;
		}
		else
		{
			dataFlowInfo = facade.getTypeInfo(left, context.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE)).getDataFlowInfo();
		}
		return DataFlowUtils.checkType(result, expression, context, dataFlowInfo);
	}

	private boolean checkBinaryWithTypeRHS(NapileBinaryExpressionWithTypeRHS expression, ExpressionTypingContext context, @NotNull JetType targetType, JetType actualType)
	{
		NapileSimpleNameExpression operationSign = expression.getOperationSign();
		IElementType operationType = operationSign.getReferencedNameElementType();
		if(operationType == NapileTokens.COLON)
		{
			if(targetType != TypeUtils.NO_EXPECTED_TYPE && !JetTypeChecker.INSTANCE.isSubtypeOf(actualType, targetType))
			{
				context.trace.report(TYPE_MISMATCH.on(expression.getLeft(), targetType, actualType));
				return false;
			}
			return true;
		}
		else if(operationType == NapileTokens.AS_KEYWORD || operationType == NapileTokens.AS_SAFE)
		{
			checkForCastImpossibility(expression, actualType, targetType, context);
			return true;
		}
		else
		{
			context.trace.report(UNSUPPORTED.on(operationSign, "binary operation with type RHS"));
			return false;
		}
	}

	private void checkForCastImpossibility(NapileBinaryExpressionWithTypeRHS expression, JetType actualType, JetType targetType, ExpressionTypingContext context)
	{
		if(actualType == null || targetType == TypeUtils.NO_EXPECTED_TYPE)
			return;

		JetTypeChecker typeChecker = JetTypeChecker.INSTANCE;
		if(!typeChecker.isSubtypeOf(targetType, actualType))
		{
			if(typeChecker.isSubtypeOf(actualType, targetType))
			{
				context.trace.report(USELESS_CAST_STATIC_ASSERT_IS_FINE.on(expression.getOperationSign()));
			}
			else
			{
				// See JET-58 Make 'as never succeeds' a warning, or even never check for Java (external) types
				context.trace.report(CAST_NEVER_SUCCEEDS.on(expression.getOperationSign()));
			}
		}
		else
		{
			if(typeChecker.isSubtypeOf(actualType, targetType))
			{
				context.trace.report(USELESS_CAST.on(expression.getOperationSign()));
			}
			else
			{
				if(isCastErased(actualType, targetType, typeChecker))
				{
					context.trace.report(Errors.UNCHECKED_CAST.on(expression, actualType, targetType));
				}
			}
		}
	}

	/**
	 * Check if assignment from ActualType to TargetType is erased.
	 * It is an error in "is" statement and warning in "as".
	 */
	public static boolean isCastErased(JetType actualType, JetType targetType, JetTypeChecker typeChecker)
	{

		if(!(targetType.getConstructor().getDeclarationDescriptor() instanceof ClassDescriptor))
		{
			// TODO: what if it is TypeParameterDescriptor?
			return false;
		}

		// do not crash on error types
		if(ErrorUtils.isErrorType(actualType) || ErrorUtils.isErrorType(targetType))
		{
			return false;
		}

		{
			Multimap<TypeConstructor, JetType> typeSubstitutionMap = SubstitutionUtils.buildDeepSubstitutionMultimap(targetType);

			for(int i = 0; i < actualType.getConstructor().getParameters().size(); ++i)
			{
				JetType actualTypeParameter = actualType.getArguments().get(i);
				TypeParameterDescriptor subjectTypeParameterDescriptor = actualType.getConstructor().getParameters().get(i);

				if(subjectTypeParameterDescriptor.isReified())
				{
					continue;
				}

				Collection<JetType> subst = typeSubstitutionMap.get(subjectTypeParameterDescriptor.getTypeConstructor());
				for(JetType proj : subst)
				{
					//if (!proj.getType().equals(actualTypeParameter.getType())) {
					if(!typeChecker.isSubtypeOf(actualTypeParameter, proj))
					{
						return true;
					}
				}
			}
		}

		{
			JetType targetTypeClerared = TypeUtils.makeUnsubstitutedType((ClassDescriptor) targetType.getConstructor().getDeclarationDescriptor(), null);

			Multimap<TypeConstructor, JetType> clearTypeSubstitutionMap = SubstitutionUtils.buildDeepSubstitutionMultimap(targetTypeClerared);

			Set<JetType> clearSubstituted = new HashSet<JetType>();

			for(int i = 0; i < actualType.getConstructor().getParameters().size(); ++i)
			{
				TypeParameterDescriptor subjectTypeParameterDescriptor = actualType.getConstructor().getParameters().get(i);

				Collection<JetType> subst = clearTypeSubstitutionMap.get(subjectTypeParameterDescriptor.getTypeConstructor());
				for(JetType proj : subst)
				{
					clearSubstituted.add(proj);
				}
			}

			for(int i = 0; i < targetType.getConstructor().getParameters().size(); ++i)
			{
				TypeParameterDescriptor typeParameter = targetType.getConstructor().getParameters().get(i);
				JetType typeProjection = targetType.getArguments().get(i);

				if(typeParameter.isReified())
				{
					continue;
				}

				// if parameter is mapped to nothing then it is erased
				if(!clearSubstituted.contains(typeParameter.getDefaultType()))
				{
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public JetTypeInfo visitThisExpression(NapileThisExpression expression, ExpressionTypingContext context)
	{
		JetType result = null;
		ReceiverDescriptor thisReceiver = resolveToReceiver(expression, context, false);

		if(thisReceiver != null)
		{
			if(!thisReceiver.exists())
			{
				context.trace.report(NO_THIS.on(expression));
			}
			else
			{
				result = thisReceiver.getType();
				context.trace.record(BindingContext.EXPRESSION_TYPE, expression.getInstanceReference(), result);
			}
		}
		return DataFlowUtils.checkType(result, expression, context, context.dataFlowInfo);
	}

	@Override
	public JetTypeInfo visitSuperExpression(NapileSuperExpression expression, ExpressionTypingContext context)
	{
		if(!context.namespacesAllowed)
		{
			context.trace.report(SUPER_IS_NOT_AN_EXPRESSION.on(expression, expression.getText()));
			return JetTypeInfo.create(null, context.dataFlowInfo);
		}
		JetType result = null;

		ReceiverDescriptor thisReceiver = resolveToReceiver(expression, context, true);
		if(thisReceiver == null)
			return JetTypeInfo.create(null, context.dataFlowInfo);

		if(!thisReceiver.exists())
		{
			context.trace.report(SUPER_NOT_AVAILABLE.on(expression));
		}
		else
		{
			JetType thisType = thisReceiver.getType();
			Collection<? extends JetType> supertypes = thisType.getConstructor().getSupertypes();
			TypeSubstitutor substitutor = TypeSubstitutor.create(thisType);

			NapileTypeReference superTypeQualifier = expression.getSuperTypeQualifier();
			if(superTypeQualifier != null)
			{
				NapileTypeElement typeElement = superTypeQualifier.getTypeElement();

				DeclarationDescriptor classifierCandidate = null;
				JetType supertype = null;
				PsiElement redundantTypeArguments = null;
				if(typeElement instanceof NapileUserType)
				{
					NapileUserType userType = (NapileUserType) typeElement;
					// This may be just a superclass name even if the superclass is generic
					if(userType.getTypeArguments().isEmpty())
					{
						classifierCandidate = context.expressionTypingServices.getTypeResolver().resolveClass(context.scope, userType, context.trace);
					}
					else
					{
						supertype = context.expressionTypingServices.getTypeResolver().resolveType(context.scope, superTypeQualifier, context.trace, true);
						redundantTypeArguments = userType.getTypeArgumentList();
					}
				}
				else
				{
					supertype = context.expressionTypingServices.getTypeResolver().resolveType(context.scope, superTypeQualifier, context.trace, true);
				}

				if(supertype != null)
				{
					if(supertypes.contains(supertype))
					{
						result = supertype;
					}
				}
				else if(classifierCandidate instanceof ClassDescriptor)
				{
					ClassDescriptor superclass = (ClassDescriptor) classifierCandidate;

					for(JetType declaredSupertype : supertypes)
					{
						if(declaredSupertype.getConstructor().equals(superclass.getTypeConstructor()))
						{
							result = substitutor.safeSubstitute(declaredSupertype);
							break;
						}
					}
				}

				boolean validClassifier = classifierCandidate != null && !ErrorUtils.isError(classifierCandidate);
				boolean validType = supertype != null && !ErrorUtils.isErrorType(supertype);
				if(result == null && (validClassifier || validType))
				{
					context.trace.report(NOT_A_SUPERTYPE.on(superTypeQualifier));
				}
				else if(redundantTypeArguments != null)
				{
					context.trace.report(TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER.on(redundantTypeArguments));
				}
			}
			else
			{
				if(supertypes.size() > 1)
				{
					context.trace.report(AMBIGUOUS_SUPER.on(expression));
				}
				else
				{
					// supertypes may be empty when all the supertypes are error types (are not resolved, for example)
					JetType type = supertypes.isEmpty() ? TypeUtils.getTypeOfClassOrErrorType(context.scope, NapileLangPackage.ANY, false) : supertypes.iterator().next();
					result = substitutor.substitute(type);
				}
			}
			if(result != null)
			{
				context.trace.record(BindingContext.EXPRESSION_TYPE, expression.getInstanceReference(), result);
				context.trace.record(BindingContext.REFERENCE_TARGET, expression.getInstanceReference(), result.getConstructor().getDeclarationDescriptor());
				if(superTypeQualifier != null)
				{
					context.trace.record(BindingContext.TYPE_RESOLUTION_SCOPE, superTypeQualifier, context.scope);
				}
			}
		}
		return DataFlowUtils.checkType(result, expression, context, context.dataFlowInfo);
	}

	@Nullable // No class receivers
	private ReceiverDescriptor resolveToReceiver(NapileInstanceExpression expression, ExpressionTypingContext context, boolean onlyClassReceivers)
	{
		ReceiverDescriptor thisReceiver = null;
		if(onlyClassReceivers)
		{
			List<ReceiverDescriptor> receivers = Lists.newArrayList();
			context.scope.getImplicitReceiversHierarchy(receivers);
			for(ReceiverDescriptor receiver : receivers)
			{
				if(receiver instanceof ClassReceiver)
				{
					thisReceiver = receiver;
					break;
				}
			}
		}
		else
		{
			thisReceiver = context.scope.getImplicitReceiver();
			if(context.scope.getContainingDeclaration() instanceof DeclarationDescriptorWithVisibility && ((DeclarationDescriptorWithVisibility) context.scope.getContainingDeclaration()).isStatic())
				thisReceiver = ReceiverDescriptor.NO_RECEIVER;
		}
		if(thisReceiver instanceof ThisReceiverDescriptor)
		{
			context.trace.record(REFERENCE_TARGET, expression.getInstanceReference(), ((ThisReceiverDescriptor) thisReceiver).getDeclarationDescriptor());
		}
		return thisReceiver;
	}

	@Override
	public JetTypeInfo visitBlockExpression(NapileBlockExpression expression, ExpressionTypingContext context)
	{
		return visitBlockExpression(expression, context, false);
	}

	public JetTypeInfo visitBlockExpression(NapileBlockExpression expression, ExpressionTypingContext context, boolean isStatement)
	{
		return context.expressionTypingServices.getBlockReturnedType(context.scope, expression, isStatement ? CoercionStrategy.COERCION_TO_UNIT : CoercionStrategy.NO_COERCION, context, context.trace);
	}

	@Override
	public JetTypeInfo visitLinkMethodExpression(NapileLinkMethodExpression expression, ExpressionTypingContext context)
	{
		NapileSimpleNameExpression target = expression.getTarget();
		if(target == null)
			return JetTypeInfo.create(null, context.dataFlowInfo);

		NapileTypeList typeList = expression.getTypeList();
		List<JetType> parameterTypes = Collections.emptyList();
		if(typeList != null)
		{
			List<NapileTypeReference> typeReferences = typeList.getTypeList();
			parameterTypes = new ArrayList<JetType>(typeReferences.size());
			for(NapileTypeReference typeReference : typeReferences)
				parameterTypes.add(context.expressionTypingServices.getTypeResolver().resolveType(context.scope, typeReference, context.trace, false));
		}

		MethodDescriptor targetMethod = null;
		Collection<MethodDescriptor> methodDescriptors = context.scope.getMethods(target.getReferencedNameAsName());

		NapileDotQualifiedExpression classTarget = expression.getClassTarget();
		if(classTarget != null)
		{
			FqName fqName = new FqName(expression.getQualifiedName());

			ClassDescriptor classDescriptor = context.scope.getClass(fqName);
			NapileSimpleNameExpression[] children = classTarget.getChildExpressions();

			//TODO [VISTALL] currently we dont support linking with type parameters
			if(classDescriptor == null || !classDescriptor.getTypeConstructor().getParameters().isEmpty())
			{
				for(int i = 0; i < children.length; i++)
					context.trace.report(Errors.UNRESOLVED_REFERENCE.on(children[i]));
				methodDescriptors = Collections.emptyList();
			}
			else
			{
				List<Name> packages = fqName.parent().pathSegments();
				for(int i = 0; i < (children.length - 1); i++)
					context.trace.record(BindingContext.REFERENCE_TARGET, children[i], context.scope.getPackage(packages.get(i)));

				context.trace.record(BindingContext.REFERENCE_TARGET, children[children.length - 1], classDescriptor);
				methodDescriptors = classDescriptor.getMemberScope(Collections.<JetType>emptyList()).getMethods(target.getReferencedNameAsName());
			}
		}

		if(!parameterTypes.isEmpty())
		{
			Collection<MethodDescriptor> targets = new ArrayList<MethodDescriptor>(2);
			for(MethodDescriptor methodDescriptor : methodDescriptors)
			{
				List<CallParameterDescriptor> parameters = methodDescriptor.getValueParameters();
				if(parameters.size() != parameterTypes.size())
					continue;

				boolean find = true;
				for(int i = 0; i < parameters.size(); i++)
				{
					JetType expectedType = parameterTypes.get(i);
					JetType foundType = parameters.get(i).getType();
					if(!JetTypeChecker.INSTANCE.equalTypes(foundType, expectedType))
						find = false;
				}

				if(find)
					targets.add(methodDescriptor);
			}

			methodDescriptors = targets;
		}

		if(methodDescriptors.size() == 1)
			targetMethod = methodDescriptors.iterator().next();

		if(targetMethod != null)
		{
			Map<Name, JetType> valueParameters = new LinkedHashMap<Name, JetType>(targetMethod.getValueParameters().size());
			for(CallParameterDescriptor parameterDescriptor : targetMethod.getValueParameters())
				valueParameters.put(parameterDescriptor.getName(), parameterDescriptor.getType());

			context.trace.record(BindingContext.REFERENCE_TARGET, target, targetMethod);

			return JetTypeInfo.create(new JetTypeImpl(new MethodTypeConstructorImpl(targetMethod.getReturnType(), valueParameters), context.scope), context.dataFlowInfo);

		}
		else
		{
			if(methodDescriptors.isEmpty())
				context.trace.report(Errors.UNRESOLVED_REFERENCE.on(target));
			else if(methodDescriptors.size() > 1)
			{
				context.trace.record(BindingContext.AMBIGUOUS_REFERENCE_TARGET, target, methodDescriptors);
				context.trace.report(Errors.AMBIGUOUS_LINK_METHOD.on(target, methodDescriptors));
			}

			return JetTypeInfo.create(null, context.dataFlowInfo);
		}
	}

	@Override
	public JetTypeInfo visitQualifiedExpression(NapileQualifiedExpression expression, ExpressionTypingContext context)
	{
		NapileExpression selectorExpression = expression.getSelectorExpression();
		NapileExpression receiverExpression = expression.getReceiverExpression();
		ExpressionTypingContext contextWithNoExpectedType = context.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
		JetTypeInfo receiverTypeInfo = facade.getTypeInfo(receiverExpression, contextWithNoExpectedType.replaceNamespacesAllowed(true));
		JetType receiverType = receiverTypeInfo.getType();
		if(selectorExpression == null)
			return JetTypeInfo.create(null, context.dataFlowInfo);
		if(receiverType == null)
			receiverType = ErrorUtils.createErrorType("Type for " + expression.getText());

		context = context.replaceDataFlowInfo(receiverTypeInfo.getDataFlowInfo());

		if(selectorExpression instanceof NapileSimpleNameExpression)
		{
			propagateConstantValues(expression, context, (NapileSimpleNameExpression) selectorExpression);
		}

		JetTypeInfo selectorReturnTypeInfo = getSelectorReturnTypeInfo(new ExpressionReceiver(receiverExpression, receiverType), expression.getOperationTokenNode(), selectorExpression, context);
		JetType selectorReturnType = selectorReturnTypeInfo.getType();

		//TODO move further
		if(expression.getOperationSign() == NapileTokens.SAFE_ACCESS)
		{
			if(selectorReturnType != null && !selectorReturnType.isNullable() && !TypeUtils.isEqualFqName(selectorReturnType, NapileLangPackage.NULL))
			{
				if(receiverType.isNullable())
				{
					selectorReturnType = TypeUtils.makeNullable(selectorReturnType);
				}
			}
		}

		// TODO : this is suspicious: remove this code?
		if(selectorReturnType != null)
		{
			context.trace.record(BindingContext.EXPRESSION_TYPE, selectorExpression, selectorReturnType);
		}
		return DataFlowUtils.checkType(selectorReturnType, expression, context, selectorReturnTypeInfo.getDataFlowInfo());
	}

	private void propagateConstantValues(NapileQualifiedExpression expression, ExpressionTypingContext context, NapileSimpleNameExpression selectorExpression)
	{
		NapileExpression receiverExpression = expression.getReceiverExpression();
		CompileTimeConstant<?> receiverValue = context.trace.getBindingContext().get(BindingContext.COMPILE_TIME_VALUE, receiverExpression);
		CompileTimeConstant<?> wholeExpressionValue = context.trace.getBindingContext().get(BindingContext.COMPILE_TIME_VALUE, expression);
		DeclarationDescriptor declarationDescriptor = context.trace.getBindingContext().get(BindingContext.REFERENCE_TARGET, selectorExpression);
		if(wholeExpressionValue == null &&
				receiverValue != null &&
				!(receiverValue instanceof ErrorValue) &&
				receiverValue.getValue() instanceof Number &&
				context.scope.getClassifier(NapileLangPackage.NUMBER.shortName()) == declarationDescriptor)
		{
			Number value = (Number) receiverValue.getValue();
			Name referencedName = selectorExpression.getReferencedNameAsName();
			if(OperatorConventions.NUMBER_CONVERSIONS.contains(referencedName))
			{
				if(DOUBLE.equals(referencedName))
				{
					context.trace.record(BindingContext.COMPILE_TIME_VALUE, expression, new DoubleValue(value.doubleValue()));
				}
				else if(FLOAT.equals(referencedName))
				{
					context.trace.record(BindingContext.COMPILE_TIME_VALUE, expression, new FloatValue(value.floatValue()));
				}
				else if(LONG.equals(referencedName))
				{
					context.trace.record(BindingContext.COMPILE_TIME_VALUE, expression, new LongValue(value.longValue()));
				}
				else if(SHORT.equals(referencedName))
				{
					context.trace.record(BindingContext.COMPILE_TIME_VALUE, expression, new ShortValue(value.shortValue()));
				}
				else if(BYTE.equals(referencedName))
				{
					context.trace.record(BindingContext.COMPILE_TIME_VALUE, expression, new ByteValue(value.byteValue()));
				}
				else if(INT.equals(referencedName))
				{
					context.trace.record(BindingContext.COMPILE_TIME_VALUE, expression, new IntValue(value.intValue()));
				}
			}
		}
	}

	@Nullable
	private MethodDescriptor getFunctionDescriptor(@NotNull Call call, @NotNull NapileExpression callExpression, @NotNull ReceiverDescriptor receiver, @NotNull ExpressionTypingContext context, @NotNull boolean[] result)
	{

		OverloadResolutionResults<MethodDescriptor> results = context.resolveFunctionCall(call);
		if(!results.isNothing())
		{
			checkSuper(receiver, results, context.trace, callExpression);
			result[0] = true;
			if(results.isSingleResult())
			{
				ResolvedCall<MethodDescriptor> resultingCall = results.getResultingCall();
				if(resultingCall instanceof ResolvedCallWithTrace)
				{
					if(((ResolvedCallWithTrace) resultingCall).getStatus() == ResolutionStatus.TYPE_INFERENCE_ERROR)
					{
						return null;
					}
				}
			}
			return results.isSingleResult() ? results.getResultingDescriptor() : null;
		}
		result[0] = false;
		return null;
	}

	@Nullable
	private JetType getVariableType(@NotNull NapileSimpleNameExpression nameExpression, @NotNull ReceiverDescriptor receiver, @Nullable ASTNode callOperationNode, @NotNull ExpressionTypingContext context, @NotNull boolean[] result)
	{
		TemporaryBindingTrace traceForVariable = TemporaryBindingTrace.create(context.trace);
		OverloadResolutionResults<VariableDescriptor> resolutionResult = context.replaceBindingTrace(traceForVariable).resolveSimpleProperty(receiver, callOperationNode, nameExpression);
		if(!resolutionResult.isNothing())
		{
			traceForVariable.commit();
			checkSuper(receiver, resolutionResult, context.trace, nameExpression);
			result[0] = true;
			VariableAccessorResolver.resolveGetter(nameExpression, receiver, context);
			return resolutionResult.isSingleResult() ? resolutionResult.getResultingDescriptor().getReturnType() : null;
		}

		ExpressionTypingContext newContext = receiver.exists() ? context.replaceScope(receiver.getType().getMemberScope()) : context;
		TemporaryBindingTrace traceForNamespaceOrClassObject = TemporaryBindingTrace.create(context.trace);
		JetType jetType = lookupNamespaceOrClassObject(nameExpression, nameExpression.getReferencedNameAsName(), newContext.replaceBindingTrace(traceForNamespaceOrClassObject));
		if(jetType != null)
		{
			traceForNamespaceOrClassObject.commit();

			// Uncommitted changes in temp context
			context.trace.record(RESOLUTION_SCOPE, nameExpression, context.scope);
			if(context.dataFlowInfo.hasTypeInfoConstraints())
			{
				context.trace.record(NON_DEFAULT_EXPRESSION_DATA_FLOW, nameExpression, context.dataFlowInfo);
			}
			result[0] = true;
			return jetType;
		}
		result[0] = false;
		return null;
	}

	@NotNull
	public JetTypeInfo getSelectorReturnTypeInfo(@NotNull ReceiverDescriptor receiver, @Nullable ASTNode callOperationNode, @NotNull NapileExpression selectorExpression, @NotNull ExpressionTypingContext context)
	{
		if(selectorExpression instanceof NapileCallExpression)
		{
			return getCallExpressionTypeInfo((NapileCallExpression) selectorExpression, receiver, callOperationNode, context);
		}
		else if(selectorExpression instanceof NapileSimpleNameExpression)
		{
			return getSimpleNameExpressionTypeInfo((NapileSimpleNameExpression) selectorExpression, receiver, callOperationNode, context);
		}
		else if(selectorExpression instanceof NapileQualifiedExpression)
		{
			NapileQualifiedExpression qualifiedExpression = (NapileQualifiedExpression) selectorExpression;
			NapileExpression newReceiverExpression = qualifiedExpression.getReceiverExpression();
			JetTypeInfo newReceiverTypeInfo = getSelectorReturnTypeInfo(receiver, callOperationNode, newReceiverExpression, context.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE));
			JetType newReceiverType = newReceiverTypeInfo.getType();
			DataFlowInfo newReceiverDataFlowInfo = newReceiverTypeInfo.getDataFlowInfo();
			NapileExpression newSelectorExpression = qualifiedExpression.getSelectorExpression();
			if(newReceiverType != null && newSelectorExpression != null)
			{
				return getSelectorReturnTypeInfo(new ExpressionReceiver(newReceiverExpression, newReceiverType), qualifiedExpression.getOperationTokenNode(), newSelectorExpression, context.replaceDataFlowInfo(newReceiverDataFlowInfo));
			}
		}
		else
		{
			context.trace.report(ILLEGAL_SELECTOR.on(selectorExpression, selectorExpression.getText()));
		}
		return JetTypeInfo.create(null, context.dataFlowInfo);
	}

	@NotNull
	private JetTypeInfo getSimpleNameExpressionTypeInfo(@NotNull NapileSimpleNameExpression nameExpression, @NotNull ReceiverDescriptor receiver, @Nullable ASTNode callOperationNode, @NotNull ExpressionTypingContext context)
	{
		TemporaryBindingTrace traceForVariable = TemporaryBindingTrace.create(context.trace);

		boolean[] result = new boolean[1];

		JetType type = getVariableType(nameExpression, receiver, callOperationNode, context.replaceBindingTrace(traceForVariable), result);
		if(result[0])
		{
			traceForVariable.commit();
			return JetTypeInfo.create(type, context.dataFlowInfo);
		}

		Call call = CallMaker.makeCall(nameExpression, receiver, callOperationNode, nameExpression, Collections.<ValueArgument>emptyList());
		TemporaryBindingTrace traceForFunction = TemporaryBindingTrace.create(context.trace);
		MethodDescriptor methodDescriptor = getFunctionDescriptor(call, nameExpression, receiver, context, result);
		if(result[0])
		{
			traceForFunction.commit();
			boolean hasValueParameters = methodDescriptor == null || methodDescriptor.getValueParameters().size() > 0;
			context.trace.report(FUNCTION_CALL_EXPECTED.on(nameExpression, nameExpression, hasValueParameters));
			type = methodDescriptor != null ? methodDescriptor.getReturnType() : null;
			return JetTypeInfo.create(type, context.dataFlowInfo);
		}

		traceForVariable.commit();
		return JetTypeInfo.create(null, context.dataFlowInfo);
	}

	@NotNull
	private JetTypeInfo getCallExpressionTypeInfo(@NotNull NapileCallExpression callExpression, @NotNull ReceiverDescriptor receiver, @Nullable ASTNode callOperationNode, @NotNull ExpressionTypingContext context)
	{

		boolean[] result = new boolean[1];
		Call call = CallMaker.makeCall(receiver, callOperationNode, callExpression);

		TemporaryBindingTrace traceForFunction = TemporaryBindingTrace.create(context.trace);
		MethodDescriptor methodDescriptor = getFunctionDescriptor(call, callExpression, receiver, context.replaceBindingTrace(traceForFunction), result);
		if(result[0])
		{
			traceForFunction.commit();
			if(callExpression.getValueArgumentList() == null && callExpression.getFunctionLiteralArguments().isEmpty())
			{
				// there are only type arguments
				boolean hasValueParameters = methodDescriptor == null || methodDescriptor.getValueParameters().size() > 0;
				context.trace.report(FUNCTION_CALL_EXPECTED.on(callExpression, callExpression, hasValueParameters));
			}
			if(methodDescriptor == null)
			{
				return JetTypeInfo.create(null, context.dataFlowInfo);
			}
			JetType type = methodDescriptor.getReturnType();

			DataFlowInfo dataFlowInfo = context.dataFlowInfo;
			NapileValueArgumentList argumentList = callExpression.getValueArgumentList();
			if(argumentList != null)
			{
				for(NapileValueArgument argument : argumentList.getArguments())
				{
					NapileExpression expression = argument.getArgumentExpression();
					if(expression != null)
					{
						dataFlowInfo = dataFlowInfo.and(facade.getTypeInfo(expression, context.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE).
								replaceDataFlowInfo(dataFlowInfo)).getDataFlowInfo());
					}
				}
			}
			return JetTypeInfo.create(type, dataFlowInfo);
		}

		NapileExpression calleeExpression = callExpression.getCalleeExpression();
		if(calleeExpression instanceof NapileSimpleNameExpression && callExpression.getTypeArgumentList() == null)
		{
			TemporaryBindingTrace traceForVariable = TemporaryBindingTrace.create(context.trace);
			JetType type = getVariableType((NapileSimpleNameExpression) calleeExpression, receiver, callOperationNode, context.replaceBindingTrace(traceForVariable), result);
			if(result[0])
			{
				traceForVariable.commit();
				context.trace.report(FUNCTION_EXPECTED.on((NapileReferenceExpression) calleeExpression, calleeExpression, type != null ? type : ErrorUtils.createErrorType("")));
				return JetTypeInfo.create(null, context.dataFlowInfo);
			}
		}
		traceForFunction.commit();
		return JetTypeInfo.create(null, context.dataFlowInfo);
	}

	private static void checkSuper(@NotNull ReceiverDescriptor receiverDescriptor, @NotNull OverloadResolutionResults<? extends CallableDescriptor> results, @NotNull BindingTrace trace, @NotNull NapileExpression expression)
	{
		if(!results.isSingleResult())
			return;
		if(!(receiverDescriptor instanceof ExpressionReceiver))
			return;
		NapileExpression receiver = ((ExpressionReceiver) receiverDescriptor).getExpression();
		CallableDescriptor descriptor = results.getResultingDescriptor();
		if(receiver instanceof NapileSuperExpression)
		{
			if(descriptor.getModality() == Modality.ABSTRACT)
				trace.report(ABSTRACT_SUPER_CALL.on(expression));
		}
	}

	@Override
	public JetTypeInfo visitCallExpression(NapileCallExpression expression, ExpressionTypingContext context)
	{
		JetTypeInfo expressionTypeInfo = getCallExpressionTypeInfo(expression, ReceiverDescriptor.NO_RECEIVER, null, context);
		return DataFlowUtils.checkType(expressionTypeInfo.getType(), expression, context, expressionTypeInfo.getDataFlowInfo());
	}

	@Override
	public JetTypeInfo visitUnaryExpression(NapileUnaryExpression expression, ExpressionTypingContext context)
	{
		return visitUnaryExpression(expression, context, false);
	}

	public JetTypeInfo visitUnaryExpression(NapileUnaryExpression expression, ExpressionTypingContext context, boolean isStatement)
	{
		NapileExpression baseExpression = expression.getBaseExpression();
		if(baseExpression == null)
			return JetTypeInfo.create(null, context.dataFlowInfo);

		NapileSimpleNameExpression operationSign = expression.getOperationReference();

		IElementType operationType = operationSign.getReferencedNameElementType();

		// Special case for expr!!
		if(operationType == NapileTokens.EXCLEXCL)
			return visitExclExclExpression(expression, context);

		// Type check the base expression
		JetTypeInfo typeInfo = facade.getTypeInfo(baseExpression, context.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE));
		JetType type = typeInfo.getType();
		if(type == null)
			return typeInfo;
		DataFlowInfo dataFlowInfo = typeInfo.getDataFlowInfo();

		// Conventions for unary operations
		Name name = OperatorConventions.UNARY_OPERATION_NAMES.get(operationType);
		if(name == null)
		{
			context.trace.report(UNSUPPORTED.on(operationSign, "visitUnaryExpression"));
			return JetTypeInfo.create(null, dataFlowInfo);
		}

		// a[i]++/-- takes special treatment because it is actually let j = i, arr = a in arr.set(j, a.get(j).inc())
		if((operationType == NapileTokens.PLUSPLUS || operationType == NapileTokens.MINUSMINUS) && baseExpression instanceof NapileArrayAccessExpressionImpl)
		{
			NapileExpression stubExpression = ExpressionTypingUtils.createStubExpressionOfNecessaryType(baseExpression.getProject(), type, context.trace);
			resolveArrayAccessSetMethod((NapileArrayAccessExpressionImpl) baseExpression, stubExpression, context.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE).replaceBindingTrace(TemporaryBindingTrace.create(context.trace)), context.trace);
		}

		ExpressionReceiver receiver = new ExpressionReceiver(baseExpression, type);

		// Resolve the operation reference
		OverloadResolutionResults<MethodDescriptor> resolutionResults = context.resolveCallWithGivenName(CallMaker.makeCall(receiver, expression), expression.getOperationReference(), name);

		if(!resolutionResults.isSuccess())
		{
			return JetTypeInfo.create(null, dataFlowInfo);
		}

		// Computing the return type
		JetType returnType = resolutionResults.getResultingDescriptor().getReturnType();
		JetType result;
		if(operationType == NapileTokens.PLUSPLUS || operationType == NapileTokens.MINUSMINUS)
		{
			VariableAccessorResolver.resolveForUnaryCalL(expression, context);

			JetType receiverType = receiver.getType();
			if(!JetTypeChecker.INSTANCE.isSubtypeOf(returnType, receiverType))
				context.trace.report(RESULT_TYPE_MISMATCH.on(operationSign, name.getName(), receiverType, returnType));
			else
			{
				context.trace.record(BindingContext.VARIABLE_REASSIGNMENT, expression);

				checkLValue(context.trace, baseExpression);
			}
		}

		result = returnType;

		return DataFlowUtils.checkType(result, expression, context, dataFlowInfo);
	}

	private JetTypeInfo visitExclExclExpression(@NotNull NapileUnaryExpression expression, @NotNull ExpressionTypingContext context)
	{
		NapileExpression baseExpression = expression.getBaseExpression();
		assert baseExpression != null;
		NapileSimpleNameExpression operationSign = expression.getOperationReference();
		assert operationSign.getReferencedNameElementType() == NapileTokens.EXCLEXCL;

		JetType expectedType;
		if(context.expectedType != TypeUtils.NO_EXPECTED_TYPE)
		{
			expectedType = TypeUtils.makeNullable(context.expectedType);
		}
		else
		{
			expectedType = TypeUtils.NO_EXPECTED_TYPE;
		}
		JetTypeInfo typeInfo = facade.getTypeInfo(baseExpression, context.replaceExpectedType(expectedType));
		JetType type = typeInfo.getType();
		if(type == null)
		{
			return typeInfo;
		}
		DataFlowInfo dataFlowInfo = typeInfo.getDataFlowInfo();
		if(isKnownToBeNotNull(baseExpression, context))
		{
			context.trace.report(UNNECESSARY_NOT_NULL_ASSERTION.on(operationSign, type));
		}
		else
		{
			DataFlowValue value = DataFlowValueFactory.INSTANCE.createDataFlowValue(baseExpression, type, context.trace.getBindingContext());
			dataFlowInfo = dataFlowInfo.disequate(value, new DataFlowValue(new Object(), TypeUtils.getTypeOfClassOrErrorType(context.scope, NapileLangPackage.NULL, true), false, Nullability.NULL));
		}
		return JetTypeInfo.create(TypeUtils.makeNotNullable(type), dataFlowInfo);
	}

	private boolean isKnownToBeNotNull(NapileExpression expression, ExpressionTypingContext context)
	{
		JetType type = context.trace.get(EXPRESSION_TYPE, expression);
		assert type != null : "This method is only supposed to be called when the type is not null";
		if(!type.isNullable())
			return true;
		List<JetType> possibleTypes = context.dataFlowInfo.getPossibleTypes(DataFlowValueFactory.INSTANCE.createDataFlowValue(expression, type, context.trace.getBindingContext()));
		for(JetType possibleType : possibleTypes)
		{
			if(!possibleType.isNullable())
			{
				return true;
			}
		}
		return false;
	}

	public void checkLValue(BindingTrace trace, NapileExpression expression)
	{
		checkLValue(trace, expression, false);
	}

	private void checkLValue(BindingTrace trace, NapileExpression expressionWithParenthesis, boolean canBeThis)
	{
		NapileExpression expression = NapilePsiUtil.deparenthesize(expressionWithParenthesis);
		if(expression instanceof NapileArrayAccessExpressionImpl)
		{
			checkLValue(trace, ((NapileArrayAccessExpressionImpl) expression).getArrayExpression(), true);
			return;
		}
		if(canBeThis && expression instanceof NapileThisExpression)
			return;
		VariableDescriptor variable = BindingContextUtils.extractVariableDescriptorIfAny(trace.getBindingContext(), expression, true);
		if(variable == null)
		{
			trace.report(VARIABLE_EXPECTED.on(expression != null ? expression : expressionWithParenthesis));
		}
	}

	@Override
	public JetTypeInfo visitBinaryExpression(NapileBinaryExpression expression, ExpressionTypingContext contextWithExpectedType)
	{
		ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
		NapileSimpleNameExpression operationSign = expression.getOperationReference();

		NapileExpression left = expression.getLeft();
		NapileExpression right = expression.getRight();

		JetType result = null;
		IElementType operationType = operationSign.getReferencedNameElementType();
		if(operationType == NapileTokens.IDENTIFIER)
		{
			Name referencedName = operationSign.getReferencedNameAsName();
			if(referencedName != null)
			{
				result = getTypeForBinaryCall(context.scope, referencedName, context, expression);
			}
		}
		else if(OperatorConventions.BINARY_OPERATION_NAMES.containsKey(operationType))
		{
			result = getTypeForBinaryCall(context.scope, OperatorConventions.BINARY_OPERATION_NAMES.get(operationType), context, expression);
		}
		else if(operationType == NapileTokens.EQ)
		{
			result = visitAssignment(expression, contextWithExpectedType);
		}
		else if(OperatorConventions.ASSIGNMENT_OPERATION_COUNTERPARTS.containsKey(operationType))
		{
			result = visitAssignmentOperation(expression, contextWithExpectedType);
		}
		else if(OperatorConventions.COMPARISON_OPERATIONS.contains(operationType))
		{
			JetType compareToReturnType = getTypeForBinaryCall(context.scope, OperatorConventions.COMPARE_TO, context, expression);
			if(compareToReturnType != null)
			{
				if(TypeUtils.isEqualFqName(compareToReturnType, NapileConditionPackage.COMPARE_RESULT))
					result = TypeUtils.getTypeOfClassOrErrorType(context.scope, NapileLangPackage.BOOL, false);
				else
					context.trace.report(COMPARE_TO_TYPE_MISMATCH.on(operationSign, compareToReturnType));
			}
		}
		else
		{
			JetType booleanType = TypeUtils.getTypeOfClassOrErrorType(context.scope, NapileLangPackage.BOOL, false);
			if(OperatorConventions.EQUALS_OPERATIONS.contains(operationType))
			{
				if(right != null)
				{
					ExpressionReceiver receiver = ExpressionTypingUtils.safeGetExpressionReceiver(facade, left, context.replaceScope(context.scope));
					OverloadResolutionResults<MethodDescriptor> resolutionResults = context.resolveExactSignature(receiver, OperatorConventions.EQUALS, Collections.singletonList(TypeUtils.getTypeOfClassOrErrorType(context.scope, NapileLangPackage.ANY, true)));
					if(resolutionResults.isSuccess())
					{
						MethodDescriptor equals = resolutionResults.getResultingCall().getResultingDescriptor();
						context.trace.record(REFERENCE_TARGET, operationSign, equals);
						context.trace.record(RESOLVED_CALL, operationSign, resolutionResults.getResultingCall());
						if(ExpressionTypingUtils.ensureBooleanResult(operationSign, OperatorConventions.EQUALS, equals.getReturnType(), context))
						{
							ensureNonemptyIntersectionOfOperandTypes(expression, context);
						}
					}
					else
					{
						if(resolutionResults.isAmbiguity())
						{
							context.trace.report(OVERLOAD_RESOLUTION_AMBIGUITY.on(operationSign, resolutionResults.getResultingCalls()));
						}
						else
						{
							context.trace.report(EQUALS_MISSING.on(operationSign));
						}
					}
				}
				result = booleanType;
			}
			else if(OperatorConventions.IN_OPERATIONS.contains(operationType))
			{
				if(right == null)
					return JetTypeInfo.create(ErrorUtils.createErrorType("No right argument"), context.dataFlowInfo);
				checkInExpression(expression, expression.getOperationReference(), expression.getLeft(), expression.getRight(), context);
				result = booleanType;
			}
			else if(OperatorConventions.BOOLEAN_OPERATIONS.containsKey(operationType))
			{
				JetType leftType = facade.getTypeInfo(left, context.replaceScope(context.scope)).getType();
				WritableScopeImpl leftScope = ExpressionTypingUtils.newWritableScopeImpl(context, "Left scope of && or ||");
				DataFlowInfo flowInfoLeft = DataFlowUtils.extractDataFlowInfoFromCondition(left, operationType == NapileTokens.ANDAND, context);  // TODO: This gets computed twice: here and in extractDataFlowInfoFromCondition() for the whole condition
				WritableScopeImpl rightScope = operationType == NapileTokens.ANDAND ? leftScope : ExpressionTypingUtils.newWritableScopeImpl(context, "Right scope of && or ||");
				JetType rightType = right == null ? null : facade.getTypeInfo(right, context.replaceDataFlowInfo(flowInfoLeft).replaceScope(rightScope)).getType();
				if(leftType != null && !ExpressionTypingUtils.isBoolean(leftType))
				{
					context.trace.report(TYPE_MISMATCH.on(left, booleanType, leftType));
				}
				if(rightType != null && !ExpressionTypingUtils.isBoolean(rightType))
				{
					context.trace.report(TYPE_MISMATCH.on(right, booleanType, rightType));
				}
				result = booleanType;
			}
			else if(operationType == NapileTokens.ELVIS)
			{
				JetType leftType = facade.getTypeInfo(left, context.replaceScope(context.scope)).getType();
				JetType rightType = right == null ? null : facade.getTypeInfo(right, contextWithExpectedType.replaceScope(context.scope)).getType();
				if(leftType != null)
				{
					if(!leftType.isNullable())
					{
						context.trace.report(USELESS_ELVIS.on(left, leftType));
					}
					if(rightType != null)
					{
						DataFlowUtils.checkType(TypeUtils.makeNullableAsSpecified(leftType, rightType.isNullable()), left, contextWithExpectedType);
						return JetTypeInfo.create(TypeUtils.makeNullableAsSpecified(CommonSupertypes.commonSupertype(Arrays.asList(leftType, rightType)), rightType.isNullable()), context.dataFlowInfo);
					}
				}
			}
			else
			{
				context.trace.report(UNSUPPORTED.on(operationSign, "Unknown operation"));
			}
		}
		return DataFlowUtils.checkType(result, expression, contextWithExpectedType, context.dataFlowInfo);
	}

	public boolean checkInExpression(NapileElement callElement, @NotNull NapileSimpleNameExpression operationSign, @Nullable NapileExpression left, @NotNull NapileExpression right, ExpressionTypingContext context)
	{
		Name name = Name.identifier("contains");
		ExpressionReceiver receiver = ExpressionTypingUtils.safeGetExpressionReceiver(facade, right, context.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE));
		OverloadResolutionResults<MethodDescriptor> resolutionResult = context.resolveCallWithGivenName(CallMaker.makeCallWithExpressions(callElement, receiver, null, operationSign, Collections.singletonList(left)), operationSign, name);
		JetType containsType = OverloadResolutionResultsUtil.getResultType(resolutionResult);
		ExpressionTypingUtils.ensureBooleanResult(operationSign, name, containsType, context);
		return resolutionResult.isSuccess();
	}

	private void ensureNonemptyIntersectionOfOperandTypes(NapileBinaryExpression expression, ExpressionTypingContext context)
	{
		NapileExpression left = expression.getLeft();
		NapileExpression right = expression.getRight();

		// TODO : duplicated effort for == and !=
		JetType leftType = facade.getTypeInfo(left, context).getType();
		if(leftType != null && right != null)
		{
			JetType rightType = facade.getTypeInfo(right, context).getType();

			if(rightType != null)
			{
				if(TypeUtils.isIntersectionEmpty(leftType, rightType))
				{
					context.trace.report(EQUALITY_NOT_APPLICABLE.on(expression, expression.getOperationReference(), leftType, rightType));
				}
				checkSenselessComparisonWithNull(expression, left, right, context);
			}
		}
	}

	private void checkSenselessComparisonWithNull(@NotNull NapileBinaryExpression expression, @NotNull NapileExpression left, @NotNull NapileExpression right, @NotNull ExpressionTypingContext context)
	{
		NapileExpression expr;
		if(left instanceof NapileConstantExpression && left.getNode().getElementType() == NapileNodes.NULL)
		{
			expr = right;
		}
		else if(right instanceof NapileConstantExpression && right.getNode().getElementType() == NapileNodes.NULL)
		{
			expr = left;
		}
		else
		{
			return;
		}

		NapileSimpleNameExpression operationSign = expression.getOperationReference();
		JetType type = facade.getTypeInfo(expr, context).getType();
		DataFlowValue value = DataFlowValueFactory.INSTANCE.createDataFlowValue(expr, type, context.trace.getBindingContext());
		Nullability nullability = context.dataFlowInfo.getNullability(value);

		boolean expressionIsAlways;
		boolean equality = operationSign.getReferencedNameElementType() == NapileTokens.EQEQ;

		if(nullability == Nullability.NULL)
		{
			expressionIsAlways = equality;
		}
		else if(nullability == Nullability.NOT_NULL)
		{
			expressionIsAlways = !equality;
		}
		else
		{
			return;
		}

		context.trace.report(SENSELESS_COMPARISON.on(expression, expression, expressionIsAlways));
	}

	protected JetType visitAssignmentOperation(NapileBinaryExpression expression, ExpressionTypingContext context)
	{
		return assignmentIsNotAnExpressionError(expression, context);
	}

	protected JetType visitAssignment(NapileBinaryExpression expression, ExpressionTypingContext context)
	{
		return assignmentIsNotAnExpressionError(expression, context);
	}

	private JetType assignmentIsNotAnExpressionError(NapileBinaryExpression expression, ExpressionTypingContext context)
	{
		facade.checkStatementType(expression, context.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE));
		context.trace.report(ASSIGNMENT_IN_EXPRESSION_CONTEXT.on(expression));
		return null;
	}

	@Override
	public JetTypeInfo visitArrayAccessExpression(NapileArrayAccessExpressionImpl expression, ExpressionTypingContext context)
	{
		JetType type = resolveArrayAccessGetMethod(expression, context.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE));
		DataFlowUtils.checkType(type, expression, context);
		return JetTypeInfo.create(type, context.dataFlowInfo);
	}

	@Nullable
	public JetType getTypeForBinaryCall(JetScope scope, Name name, ExpressionTypingContext context, NapileBinaryExpression binaryExpression)
	{
		ExpressionReceiver receiver = ExpressionTypingUtils.safeGetExpressionReceiver(facade, binaryExpression.getLeft(), context.replaceScope(scope));
		return OverloadResolutionResultsUtil.getResultType(getResolutionResultsForBinaryCall(scope, name, context, binaryExpression, receiver));
	}

	@NotNull
    public OverloadResolutionResults<MethodDescriptor> getResolutionResultsForBinaryCall(JetScope scope, Name name, ExpressionTypingContext context, NapileBinaryExpression binaryExpression, ReceiverDescriptor receiver)
	{
		return context.replaceScope(scope).resolveCallWithGivenName(CallMaker.makeCall(receiver, binaryExpression), binaryExpression.getOperationReference(), name);
	}

	@Override
	public JetTypeInfo visitDeclaration(NapileDeclaration dcl, ExpressionTypingContext context)
	{
		context.trace.report(DECLARATION_IN_ILLEGAL_CONTEXT.on(dcl));
		return JetTypeInfo.create(null, context.dataFlowInfo);
	}

	@Override
	public JetTypeInfo visitJetElement(NapileElement element, ExpressionTypingContext context)
	{
		context.trace.report(UNSUPPORTED.on(element, getClass().getCanonicalName()));
		return JetTypeInfo.create(null, context.dataFlowInfo);
	}

	@Nullable
	JetType resolveArrayAccessSetMethod(@NotNull NapileArrayAccessExpressionImpl arrayAccessExpression, @NotNull NapileExpression rightHandSide, @NotNull ExpressionTypingContext context, @NotNull BindingTrace traceForResolveResult)
	{
		return resolveArrayAccessSpecialMethod(arrayAccessExpression, rightHandSide, context, traceForResolveResult, false);
	}

	@Nullable
	JetType resolveArrayAccessGetMethod(@NotNull NapileArrayAccessExpressionImpl arrayAccessExpression, @NotNull ExpressionTypingContext context)
	{
		return resolveArrayAccessSpecialMethod(arrayAccessExpression, null, context, context.trace, true);
	}

	/*MethodDescriptor resolveVariableGetMethod(@NotNull NapileSimpleNameExpression nameExpression, @NotNull ReceiverDescriptor receiver, @NotNull ExpressionTypingContext context, @NotNull BindingTrace trace)
	{
		Name name = Name.identifier(nameExpression.getReferencedName() + AsmConstants.ANONYM_SPLITTER + "get");
		OverloadResolutionResults<MethodDescriptor> results = context.replaceBindingTrace(trace).resolveCallWithGivenName(CallMaker.makeVariableCall(receiver, null, nameExpression), nameExpression, name);

		if(!results.isSuccess())
			return null;

		return results.getResultingDescriptor();
	}

	void resolveVariableSetMethod(@NotNull Project project, @NotNull NapileExpression target, @NotNull NapileExpression base, @NotNull ReceiverDescriptor receiver, ExpressionTypingContext context)
	{
		Name name = null;

		NapileSimpleNameExpression nameExpression = null;
		if(base instanceof NapileSimpleNameExpression)
		{
			name = Name.identifier(((NapileSimpleNameExpression) base).getReferencedName() + AsmConstants.ANONYM_SPLITTER + "set");
			nameExpression = (NapileSimpleNameExpression) base;
		}
		else if(base instanceof NapileDotQualifiedExpression)
		{
			NapileDotQualifiedExpression dotQualifiedExpression = ((NapileDotQualifiedExpression) base);

			if(dotQualifiedExpression.getSelectorExpression() instanceof NapileSimpleNameExpression)
			{
				name = Name.identifier(((NapileSimpleNameExpression) dotQualifiedExpression.getSelectorExpression()).getReferencedName() + AsmConstants.ANONYM_SPLITTER + "set");
				nameExpression = ((NapileSimpleNameExpression) dotQualifiedExpression.getSelectorExpression());
			}
		}
		else if(base instanceof NapileArrayAccessExpressionImpl)
			return;

		if(name == null)
			throw new IllegalArgumentException("Cant find nameExpression in " + base + " base " + base);

		NapileBinaryExpression binaryExpression = (NapileBinaryExpression) NapilePsiFactory.createExpression(project, base.getText() + " = null");

		TemporaryBindingTrace trace = TemporaryBindingTrace.create(context.trace);

		OverloadResolutionResults<MethodDescriptor> results = context.replaceBindingTrace(trace).resolveCallWithGivenName(CallMaker.makeVariableSetCall(ReceiverDescriptor.NO_RECEIVER, binaryExpression.getLeft(), binaryExpression.getRight()), nameExpression, name);

		if(results.isSingleResult())
			context.trace.record(BindingContext.VARIABLE_SET_CALL, target, results.getResultingCall());
	}

	MethodDescriptor resolveVariableSetMethod(@NotNull NapileSimpleNameExpression nameExpression, @NotNull NapileBinaryExpression binaryExpression, @NotNull ReceiverDescriptor receiver, @NotNull ExpressionTypingContext context, BindingTrace trace)
	{
		NapileExpression rightExpression = binaryExpression.getRight();
		if(rightExpression == null)
			return null;
		Name name = Name.identifier(nameExpression.getReferencedName() + AsmConstants.ANONYM_SPLITTER + "set");
		OverloadResolutionResults<MethodDescriptor> results = context.replaceBindingTrace(trace).resolveCallWithGivenName(CallMaker.makeVariableSetCall(receiver, binaryExpression.getLeft(), rightExpression), nameExpression, name);
		if(!results.isSingleResult())
			return null;

		return results.getResultingDescriptor();
	}   */

	@Nullable
	private JetType resolveArrayAccessSpecialMethod(@NotNull NapileArrayAccessExpressionImpl arrayAccessExpression, @Nullable NapileExpression rightHandSide, @NotNull ExpressionTypingContext context, @NotNull BindingTrace traceForResolveResult, boolean isGet)
	{
		JetType arrayType = facade.getTypeInfo(arrayAccessExpression.getArrayExpression(), context).getType();
		if(arrayType == null)
			return null;

		ExpressionReceiver receiver = new ExpressionReceiver(arrayAccessExpression.getArrayExpression(), arrayType);
		if(!isGet)
			assert rightHandSide != null;
		OverloadResolutionResults<MethodDescriptor> results = context.resolveCallWithGivenName(isGet ? CallMaker.makeArrayGetCall(receiver, arrayAccessExpression, Call.CallType.ARRAY_GET_METHOD) : CallMaker.makeArraySetCall(receiver, arrayAccessExpression, rightHandSide, Call.CallType.ARRAY_SET_METHOD), arrayAccessExpression, Name.identifier(isGet ? "get" : "set"));
		if(!results.isSingleResult())
		{
			traceForResolveResult.report(isGet ? NO_GET_METHOD.on(arrayAccessExpression) : NO_SET_METHOD.on(arrayAccessExpression));
			return null;
		}
		traceForResolveResult.record(isGet ? INDEXED_LVALUE_GET : INDEXED_LVALUE_SET, arrayAccessExpression, results.getResultingCall());
		return results.getResultingDescriptor().getReturnType();
	}
}
