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

/*
 * @author max
 */
package org.napile.compiler.lang.lexer;

import org.napile.compiler.lang.NapileLanguage;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.psi.impl.NapileArrayOfExpressionImpl;
import org.napile.compiler.lang.psi.impl.NapileInjectionExpressionImpl;
import org.napile.compiler.lang.psi.impl.NapileModifierListImpl;
import org.napile.compiler.lang.psi.stubs.elements.NapileStubElementTypes;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;

public interface NapileNodes
{
	IFileElementType NAPILE_FILE = new IFileElementType(NapileLanguage.INSTANCE);

	IElementType CLASS = NapileStubElementTypes.CLASS;
	IElementType METHOD = NapileStubElementTypes.METHOD;
	IElementType VARIABLE = NapileStubElementTypes.VARIABLE;

	NapileNode ANONYM_CLASS = new NapileNode("ANONYM_CLASS", NapileAnonymClass.class);

	NapileNode CONSTRUCTOR = new NapileNode("CONSTRUCTOR", NapileConstructorImpl.class);
	NapileNode STATIC_CONSTRUCTOR = new NapileNode("STATIC_CONSTRUCTOR", NapileStaticConstructor.class);

	IElementType TYPE_PARAMETER_LIST = NapileStubElementTypes.TYPE_PARAMETER_LIST;
	IElementType TYPE_PARAMETER = NapileStubElementTypes.TYPE_PARAMETER;

	NapileNode DELEGATION_SPECIFIER_LIST = new NapileNode("DELEGATION_SPECIFIER_LIST", NapileDelegationSpecifierList.class);

	NapileNode DELEGATOR_SUPER_CALL = new NapileNode("DELEGATOR_SUPER_CALL", NapileDelegationToSuperCall.class);
	NapileNode CONSTRUCTOR_CALLEE = new NapileNode("CONSTRUCTOR_CALLEE", NapileConstructorCalleeExpression.class);
	IElementType VALUE_PARAMETER_LIST = NapileStubElementTypes.VALUE_PARAMETER_LIST;
	IElementType VALUE_PARAMETER = NapileStubElementTypes.VALUE_PARAMETER;
	IElementType REFERENCE_PARAMETER = new NapileNode("REFERENCE_PARAMETER", NapileReferenceParameter.class);

	NapileNode CLASS_BODY = new NapileNode("CLASS_BODY", NapileClassBody.class);
	NapileNode IMPORT_DIRECTIVE = new NapileNode("IMPORT_DIRECTIVE", NapileImportDirective.class);
	NapileNode MODIFIER_LIST = new NapileNode("MODIFIER_LIST", NapileModifierListImpl.class);
	NapileNode ANNOTATION = new NapileNode("ANNOTATION", NapileAnnotation.class);

	NapileNode EXTEND_TYPE_LIST = new NapileNode("EXTEND_TYPE_LIST", NapileTypeListImpl.class);
	NapileNode LINK_METHOD_TYPE_LIST = new NapileNode("LINK_METHOD_TYPE_LIST", NapileTypeListImpl.class);
	NapileNode TYPE_ARGUMENT_LIST = new NapileNode("TYPE_ARGUMENT_LIST", NapileTypeArgumentListImpl.class);
	NapileNode VALUE_ARGUMENT_LIST = new NapileNode("VALUE_ARGUMENT_LIST", NapileValueArgumentList.class);
	NapileNode VALUE_ARGUMENT = new NapileNode("VALUE_ARGUMENT", NapileValueArgument.class);
	NapileNode VALUE_ARGUMENT_NAME = new NapileNode("VALUE_ARGUMENT_NAME", NapileValueArgumentName.class);
	NapileNode TYPE_REFERENCE = new NapileNode("TYPE_REFERENCE", NapileTypeReferenceImpl.class);

	NapileNode USER_TYPE = new NapileNode("USER_TYPE", NapileUserTypeImpl.class);
	NapileNode FUNCTION_TYPE = new NapileNode("METHOD_TYPE", NapileMethodTypeImpl.class);
	NapileNode SELF_TYPE = new NapileNode("SELF_TYPE", NapileSelfTypeImpl.class);
	NapileNode NULLABLE_TYPE = new NapileNode("NULLABLE_TYPE", NapileNullableTypeImpl.class);

	NapileNode NULL = new NapileNode("NULL", NapileConstantExpression.class);
	NapileNode BOOLEAN_CONSTANT = new NapileNode("BOOLEAN_CONSTANT", NapileConstantExpression.class);
	NapileNode FLOAT_CONSTANT = new NapileNode("FLOAT_CONSTANT", NapileConstantExpression.class);
	NapileNode CHARACTER_CONSTANT = new NapileNode("CHARACTER_CONSTANT", NapileConstantExpression.class);
	NapileNode INTEGER_CONSTANT = new NapileNode("INTEGER_CONSTANT", NapileConstantExpression.class);

	NapileNode STRING_TEMPLATE = new NapileNode("STRING_TEMPLATE", NapileStringTemplateExpression.class);
	NapileNode LONG_STRING_TEMPLATE_ENTRY = new NapileNode("LONG_STRING_TEMPLATE_ENTRY", NapileBlockStringTemplateEntry.class);
	NapileNode SHORT_STRING_TEMPLATE_ENTRY = new NapileNode("SHORT_STRING_TEMPLATE_ENTRY", NapileSimpleNameStringTemplateEntry.class);
	NapileNode LITERAL_STRING_TEMPLATE_ENTRY = new NapileNode("LITERAL_STRING_TEMPLATE_ENTRY", NapileLiteralStringTemplateEntry.class);
	NapileNode ESCAPE_STRING_TEMPLATE_ENTRY = new NapileNode("ESCAPE_STRING_TEMPLATE_ENTRY", NapileEscapeStringTemplateEntry.class);

	NapileNode CLASS_OF = new NapileNode("CLASS_OF", NapileClassOfExpression.class);
	NapileNode TYPE_OF = new NapileNode("TYPE_OF", NapileTypeOfExpression.class);
	NapileNode PARENTHESIZED = new NapileNode("PARENTHESIZED", NapileParenthesizedExpression.class);
	NapileNode RETURN = new NapileNode("RETURN", NapileReturnExpression.class);
	NapileNode THROW = new NapileNode("THROW", NapileThrowExpression.class);
	NapileNode CONTINUE = new NapileNode("CONTINUE", NapileContinueExpression.class);
	NapileNode BREAK = new NapileNode("BREAK", NapileBreakExpression.class);
	NapileNode IF = new NapileNode("IF", NapileIfExpression.class);
	NapileNode CONDITION = new NapileNode("CONDITION", NapileContainerNode.class);
	NapileNode THEN = new NapileNode("THEN", NapileContainerNode.class);
	NapileNode ELSE = new NapileNode("ELSE", NapileContainerNode.class);
	NapileNode TRY = new NapileNode("TRY", NapileTryExpression.class);
	NapileNode CATCH = new NapileNode("CATCH", NapileCatchClause.class);
	NapileNode FINALLY = new NapileNode("FINALLY", NapileFinallySection.class);
	NapileNode FOR = new NapileNode("FOR", NapileForExpression.class);
	NapileNode ARRAY = new NapileNode("ARRAY", NapileArrayOfExpressionImpl.class);
	NapileNode WHILE = new NapileNode("WHILE", NapileWhileExpression.class);
	NapileNode DO_WHILE = new NapileNode("DO_WHILE", NapileDoWhileExpression.class);
	NapileNode LOOP_RANGE = new NapileNode("LOOP_RANGE", NapileContainerNode.class);
	NapileNode BODY = new NapileNode("BODY", NapileContainerNode.class);
	NapileNode BLOCK = new NapileNode("BLOCK", NapileBlockExpression.class);
	NapileNode FUNCTION_LITERAL_EXPRESSION = new NapileNode("ANONYM_METHOD_EXPRESSION", NapileFunctionLiteralExpression.class);
	NapileNode FUNCTION_LITERAL = new NapileNode("ANONYM_METHOD", NapileAnonymMethodImpl.class);
	NapileNode REFERENCE_EXPRESSION = new NapileNode("REFERENCE_EXPRESSION", NapileSimpleNameExpressionImpl.class);

	NapileNode OPERATION_REFERENCE = new NapileNode("OPERATION_REFERENCE", NapileSimpleNameExpressionImpl.class);
	NapileNode LABEL_REFERENCE = new NapileNode("LABEL_REFERENCE", NapileSimpleNameExpressionImpl.class);
	NapileNode VARIABLE_REFERENCE = new NapileNode("VARIABLE_REFERENCE", NapileSimpleNameExpressionImpl.class);
	NapileNode THIS_EXPRESSION = new NapileNode("THIS_EXPRESSION", NapileThisExpression.class);

	NapileNode INJECTION_EXPRESSION = new NapileNode("INJECTION_EXPRESSION", NapileInjectionExpressionImpl.class);
	NapileNode LINK_METHOD_EXPRESSION = new NapileNode("LINK_METHOD_EXPRESSION", NapileLinkMethodExpressionImpl.class);
	NapileNode SUPER_EXPRESSION = new NapileNode("SUPER_EXPRESSION", NapileSuperExpression.class);
	NapileNode BINARY_EXPRESSION = new NapileNode("BINARY_EXPRESSION", NapileBinaryExpression.class);
	NapileNode BINARY_WITH_TYPE = new NapileNode("BINARY_WITH_TYPE", NapileBinaryExpressionWithTypeRHS.class);
	NapileNode IS_EXPRESSION = new NapileNode("IS_EXPRESSION", NapileIsExpression.class);
	NapileNode PREFIX_EXPRESSION = new NapileNode("PREFIX_EXPRESSION", NapilePrefixExpression.class);
	NapileNode POSTFIX_EXPRESSION = new NapileNode("POSTFIX_EXPRESSION", NapilePostfixExpression.class);
	NapileNode CALL_EXPRESSION = new NapileNode("CALL_EXPRESSION", NapileCallExpression.class);
	NapileNode LABEL_EXPRESSION = new NapileNode("LABEL_EXPRESSION", NapileLabelExpression.class);
	NapileNode ARRAY_ACCESS_EXPRESSION = new NapileNode("ARRAY_ACCESS_EXPRESSION", NapileArrayAccessExpressionImpl.class);
	NapileNode INDICES = new NapileNode("INDICES", NapileContainerNode.class);
	NapileNode DOT_QUALIFIED_EXPRESSION = new NapileNode("DOT_QUALIFIED_EXPRESSION", NapileDotQualifiedExpression.class);
	NapileNode SAFE_ACCESS_EXPRESSION = new NapileNode("SAFE_ACCESS_EXPRESSION", NapileSafeQualifiedExpression.class);

	NapileNode OBJECT_LITERAL = new NapileNode("OBJECT_LITERAL", NapileObjectLiteralExpression.class);
	NapileNode ROOT_NAMESPACE = new NapileNode("ROOT_NAMESPACE", NapileRootNamespaceExpression.class);

	NapileNode WHEN = new NapileNode("WHEN", NapileWhenExpression.class);
	NapileNode WHEN_ENTRY = new NapileNode("WHEN_ENTRY", NapileWhenEntry.class);

	NapileNode WHEN_CONDITION_IN_RANGE = new NapileNode("WHEN_CONDITION_IN_RANGE", NapileWhenConditionInRange.class);
	NapileNode WHEN_CONDITION_IS_PATTERN = new NapileNode("WHEN_CONDITION_IS_PATTERN", NapileWhenConditionIsPattern.class);
	NapileNode WHEN_CONDITION_EXPRESSION = new NapileNode("WHEN_CONDITION_WITH_EXPRESSION", NapileWhenConditionWithExpression.class);

	NapileNode PACKAGE = new NapileNode("PACKAGE", NapilePackageImplImpl.class);

	NapileNode IDE_TEMPLATE_EXPRESSION = new NapileNode("IDE_TEMPLATE_EXPRESSION", NapileIdeTemplate.class);
}