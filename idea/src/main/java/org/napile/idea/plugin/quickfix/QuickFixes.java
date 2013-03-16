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

package org.napile.idea.plugin.quickfix;

import static org.napile.compiler.lang.diagnostics.Errors.*;
import static org.napile.compiler.lang.lexer.NapileTokens.ABSTRACT_KEYWORD;
import static org.napile.compiler.lang.lexer.NapileTokens.LOCAL_KEYWORD;
import static org.napile.compiler.lang.lexer.NapileTokens.OVERRIDE_KEYWORD;

import java.util.Collection;

import org.napile.compiler.lang.diagnostics.AbstractDiagnosticFactory;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileNamedMethodOrMacro;
import org.napile.idea.plugin.codeInsight.ImplementMethodsHandler;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.intellij.codeInsight.intention.IntentionAction;

/**
 * @author svtk
 */
@Deprecated
public class QuickFixes
{

	private static final Multimap<AbstractDiagnosticFactory, JetIntentionActionFactory> factories = HashMultimap.create();
	private static final Multimap<AbstractDiagnosticFactory, IntentionAction> actions = HashMultimap.create();

	public static Collection<JetIntentionActionFactory> getActionFactories(AbstractDiagnosticFactory diagnosticFactory)
	{
		return factories.get(diagnosticFactory);
	}

	public static Collection<IntentionAction> getActions(AbstractDiagnosticFactory diagnosticFactory)
	{
		return actions.get(diagnosticFactory);
	}

	private QuickFixes()
	{
	}

	static
	{
		JetIntentionActionFactory removeAbstractModifierFactory = RemoveModifierFix.createRemoveModifierFromListOwnerFactory(ABSTRACT_KEYWORD);
		JetIntentionActionFactory addAbstractModifierFactory = AddModifierFix.createFactory(ABSTRACT_KEYWORD);

		factories.put(MUST_BE_INITIALIZED_OR_BE_ABSTRACT, addAbstractModifierFactory);

		JetIntentionActionFactory addAbstractToClassFactory = AddModifierFix.createFactory(ABSTRACT_KEYWORD, NapileClass.class);
		factories.put(ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS, removeAbstractModifierFactory);
		factories.put(ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS, addAbstractToClassFactory);

		JetIntentionActionFactory removeFunctionBodyFactory = RemoveFunctionBodyFix.createFactory();
		factories.put(ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS, removeAbstractModifierFactory);
		factories.put(ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS, addAbstractToClassFactory);

		factories.put(NATIVE_OR_ABSTRACT_METHOD_WITH_BODY, removeAbstractModifierFactory);
		factories.put(NATIVE_OR_ABSTRACT_METHOD_WITH_BODY, removeFunctionBodyFactory);

		JetIntentionActionFactory addFunctionBodyFactory = AddFunctionBodyFix.createFactory();
		factories.put(NON_ABSTRACT_OR_NATIVE_METHOD_WITH_NO_BODY, addAbstractModifierFactory);
		factories.put(NON_ABSTRACT_OR_NATIVE_METHOD_WITH_NO_BODY, addFunctionBodyFactory);

		factories.put(MACRO_MUST_BE_DECLARATED_AS_LOCAL, AddModifierFix.createFactory(LOCAL_KEYWORD, NapileNamedMethodOrMacro.class));
		factories.put(NON_MEMBER_ABSTRACT_FUNCTION, removeAbstractModifierFactory);
		factories.put(NON_MEMBER_FUNCTION_NO_BODY, addFunctionBodyFactory);

		factories.put(NOTHING_TO_OVERRIDE, RemoveModifierFix.createRemoveModifierFromListOwnerFactory(OVERRIDE_KEYWORD));
		factories.put(VIRTUAL_MEMBER_HIDDEN, AddModifierFix.createFactory(OVERRIDE_KEYWORD));

		factories.put(USELESS_CAST_STATIC_ASSERT_IS_FINE, ReplaceOperationInBinaryExpressionFix.createChangeCastToStaticAssertFactory());
		factories.put(USELESS_CAST, RemoveRightPartOfBinaryExpressionFix.createRemoveCastFactory());


		factories.put(USELESS_ELVIS, RemoveRightPartOfBinaryExpressionFix.createRemoveElvisOperatorFactory());

		JetIntentionActionFactory removeRedundantModifierFactory = RemoveModifierFix.createRemoveModifierFactory(true);
		factories.put(REDUNDANT_MODIFIER_WITH_MODIFIER, removeRedundantModifierFactory);

		JetIntentionActionFactory removeModifierFactory = RemoveModifierFix.createRemoveModifierFactory();

		factories.put(REDUNDANT_MODIFIER_IN_GETTER, removeRedundantModifierFactory);
		factories.put(ILLEGAL_MODIFIER, removeModifierFactory);

		JetIntentionActionFactory changeToBackingFieldFactory = ChangeToBackingFieldFix.createFactory();
		factories.put(INITIALIZATION_USING_BACKING_FIELD_CUSTOM_SETTER, changeToBackingFieldFactory);
		factories.put(INITIALIZATION_USING_BACKING_FIELD_OPEN_SETTER, changeToBackingFieldFactory);

		JetIntentionActionFactory unresolvedReferenceFactory = ImportClassAndFunFix.createFactory();
		factories.put(UNRESOLVED_REFERENCE, unresolvedReferenceFactory);

		factories.put(FUNCTION_CALL_EXPECTED, ChangeToFunctionInvocationFix.createFactory());

		factories.put(CANNOT_CHANGE_ACCESS_PRIVILEGE, ChangeVisibilityModifierFix.createFactory());
		factories.put(CANNOT_WEAKEN_ACCESS_PRIVILEGE, ChangeVisibilityModifierFix.createFactory());

		ImplementMethodsHandler implementMethodsHandler = new ImplementMethodsHandler();
		actions.put(ABSTRACT_MEMBER_NOT_IMPLEMENTED, implementMethodsHandler);
		actions.put(MANY_IMPL_MEMBER_NOT_IMPLEMENTED, implementMethodsHandler);

		actions.put(UNNECESSARY_SAFE_CALL, ReplaceCallFix.toDotCallFromSafeCall());
		actions.put(UNSAFE_CALL, ReplaceCallFix.toSafeCall());

		actions.put(UNSAFE_CALL, ExclExclCallFix.introduceExclExclCall());
		actions.put(UNNECESSARY_NOT_NULL_ASSERTION, ExclExclCallFix.removeExclExclCall());

		actions.put(PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE, new SpecifyTypeExplicitlyFix());
	}
}
