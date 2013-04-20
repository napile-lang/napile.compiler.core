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

package org.napile.compiler.lang.psi;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.lexer.NapileTokens;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.CheckUtil;
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author abreslav
 */
public class NapilePsiUtil
{

	public static final Name NO_NAME_PROVIDED = Name.identifier("@noname");
	public static final Name ROOT_NAMESPACE_NAME = Name.identifier("@root");

	private NapilePsiUtil()
	{
	}

	@Nullable
	public static NapileExpression deparenthesize(@NotNull NapileExpression expression)
	{
		if(expression instanceof NapileBinaryExpressionWithTypeRHS)
		{
			NapileSimpleNameExpression operationSign = ((NapileBinaryExpressionWithTypeRHS) expression).getOperationSign();
			if(NapileTokens.COLON.equals(operationSign.getReferencedNameElementType()))
			{
				expression = ((NapileBinaryExpressionWithTypeRHS) expression).getLeft();
			}
		}
		return expression;
	}

	@NotNull
	public static Name safeName(@Nullable String name)
	{
		return name == null ? NO_NAME_PROVIDED : Name.identifier(name);
	}

	@NotNull
	public static Set<NapileElement> findRootExpressions(@NotNull Collection<NapileElement> unreachableElements)
	{
		Set<NapileElement> rootElements = new HashSet<NapileElement>();
		final Set<NapileElement> shadowedElements = new HashSet<NapileElement>();
		NapileVisitorVoid shadowAllChildren = new NapileVisitorVoid()
		{
			@Override
			public void visitJetElement(NapileElement element)
			{
				if(shadowedElements.add(element))
				{
					element.acceptChildren(this);
				}
			}
		};

		for(NapileElement element : unreachableElements)
		{
			if(shadowedElements.contains(element))
				continue;
			element.acceptChildren(shadowAllChildren);

			rootElements.removeAll(shadowedElements);
			rootElements.add(element);
		}
		return rootElements;
	}


	@NotNull
	@Deprecated
	public static FqName getFQName(@NotNull NapileNamedDeclaration namedDeclaration)
	{
		return namedDeclaration.getFqName();
	}


	@Nullable
	public static <T extends PsiElement> T getDirectParentOfTypeForBlock(@NotNull NapileBlockExpression block, @NotNull Class<T> aClass)
	{
		T parent = PsiTreeUtil.getParentOfType(block, aClass);
		if(parent instanceof NapileIfExpression)
		{
			NapileIfExpression ifExpression = (NapileIfExpression) parent;
			if(ifExpression.getElse() == block || ifExpression.getThen() == block)
			{
				return parent;
			}
		}
		if(parent instanceof NapileWhenExpression)
		{
			NapileWhenExpression whenExpression = (NapileWhenExpression) parent;
			for(NapileWhenEntry whenEntry : whenExpression.getEntries())
			{
				if(whenEntry.getExpression() == block)
				{
					return parent;
				}
			}
		}
		if(parent instanceof NapileAnonymMethod)
		{
			NapileAnonymMethod functionLiteral = (NapileAnonymMethod) parent;
			if(functionLiteral.getBodyExpression() == block)
			{
				return parent;
			}
		}
		if(parent instanceof NapileTryExpression)
		{
			NapileTryExpression tryExpression = (NapileTryExpression) parent;
			if(tryExpression.getTryBlock() == block)
			{
				return parent;
			}
			for(NapileCatchClause clause : tryExpression.getCatchClauses())
			{
				if(clause.getCatchBody() == block)
				{
					return parent;
				}
			}
		}
		return null;
	}

	public static boolean isImplicitlyUsed(@NotNull NapileElement element)
	{
		PsiElement parent = element.getParent();
		if(!(parent instanceof NapileBlockExpression))
			return true;
		NapileBlockExpression block = (NapileBlockExpression) parent;
		NapileElement[] statements = block.getStatements();
		if(statements[statements.length - 1] == element)
		{
			NapileExpression expression = getDirectParentOfTypeForBlock(block, NapileIfExpression.class);
			if(expression == null)
			{
				expression = getDirectParentOfTypeForBlock(block, NapileWhenExpression.class);
			}
			if(expression == null)
			{
				expression = getDirectParentOfTypeForBlock(block, NapileAnonymMethod.class);
			}
			if(expression == null)
			{
				expression = getDirectParentOfTypeForBlock(block, NapileTryExpression.class);
			}
			if(expression != null)
			{
				return isImplicitlyUsed(expression);
			}
		}
		return false;
	}

	public static void deleteClass(@NotNull NapileClassLike clazz)
	{
		CheckUtil.checkWritable(clazz);
		NapileFile file = clazz.getContainingFile();
		NapileClass[] declarations = file.getDeclarations();
		if(declarations.length == 1)
		{
			file.delete();
		}
		else
		{
			PsiElement parent = clazz.getParent();
			CodeEditUtil.removeChild(parent.getNode(), clazz.getNode());
		}
	}

	@Nullable
	public static Name getAliasName(@NotNull NapileImportDirective importDirective)
	{
		String aliasName = importDirective.getAliasName();
		NapileExpression importedReference = importDirective.getImportedReference();
		if(importedReference == null)
		{
			return null;
		}
		NapileSimpleNameExpression referenceExpression = getLastReference(importedReference);
		if(aliasName == null)
		{
			aliasName = referenceExpression != null ? referenceExpression.getReferencedName() : null;
		}

		//noinspection ConstantConditions
		return StringUtil.isNotEmpty(aliasName) ? Name.identifier(aliasName) : null;
	}

	@Nullable
	public static NapileSimpleNameExpression getLastReference(@NotNull NapileExpression importedReference)
	{
		if(importedReference instanceof NapileDotQualifiedExpressionImpl)
		{
			NapileExpression selectorExpression = ((NapileDotQualifiedExpressionImpl) importedReference).getSelectorExpression();
			return (selectorExpression instanceof NapileSimpleNameExpression) ? (NapileSimpleNameExpression) selectorExpression : null;
		}
		if(importedReference instanceof NapileSimpleNameExpression)
		{
			return (NapileSimpleNameExpression) importedReference;
		}
		return null;
	}

	public static boolean isSafeCall(@NotNull Call call)
	{
		ASTNode callOperationNode = call.getCallOperationNode();
		return callOperationNode != null && callOperationNode.getElementType() == NapileTokens.SAFE_ACCESS;
	}

	public static boolean isFunctionLiteralWithoutDeclaredParameterTypes(NapileExpression expression)
	{
		if(!(expression instanceof NapileAnonymMethodExpression))
			return false;
		NapileAnonymMethodExpression functionLiteral = (NapileAnonymMethodExpression) expression;
		for(NapileElement parameter : functionLiteral.getAnonymMethod().getCallParameters())
		{
			if(parameter instanceof NapileCallParameterAsVariable && ((NapileCallParameterAsVariable) parameter).getTypeReference() != null)
				return false;
		}
		return true;
	}

	public static boolean isStatic(@NotNull NapileModifierListOwner owner)
	{
		if(owner instanceof NapileClass)
			if(owner.getParent() instanceof NapileFile)
				return true;
		NapileModifierList modifierList = owner.getModifierList();
		return modifierList != null && modifierList.hasModifier(NapileTokens.STATIC_KEYWORD);
	}

	@NotNull
	public static FqName getFQNameImpl(NapileNamedDeclaration namedDeclaration)
	{
		Name name = namedDeclaration.getNameAsName();
		if(name == null)
		{
			return FqName.ROOT;
		}

		PsiElement parent = namedDeclaration.getParent();
		if(parent instanceof NapileClassBody)
		{
			// One nesting to NapileClassBody doesn't affect to qualified name
			parent = parent.getParent();
		}

		FqName firstPart = null;
		if(parent instanceof NapileFile)
		{
			firstPart = ((NapileFile) parent).getPackage().getFqName();
		}
		else if(parent instanceof NapileNamedMethodOrMacro || parent instanceof NapileClass || parent instanceof NapileAnonymClass)
		{
			firstPart = getFQNameImpl((NapileNamedDeclaration) parent);
		}

		if(firstPart == null)
		{
			return FqName.ROOT;
		}

		return firstPart.child(name);
	}
}
