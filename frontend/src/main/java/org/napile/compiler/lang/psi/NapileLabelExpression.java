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

package org.napile.compiler.lang.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.lexer.NapileTokens;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;

/**
 * @author VISTALL
 * @since 20:49/08.09.12
 */
public class NapileLabelExpression extends NapileLoopExpression
{
	public NapileLabelExpression(@NotNull ASTNode node)
	{
		super(node);
	}

	@Nullable
	public String getLabelName()
	{
		PsiElement element = getLabelNameElement();

		return element == null ? null : element.getText();
	}

	@Nullable
	public PsiElement getLabelNameElement()
	{
		return findChildByType(NapileTokens.IDENTIFIER);
	}

	@Nullable
	@Override
	public NapileBlockExpression getBody()
	{
		return findChildByClass(NapileBlockExpression.class);
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitLabelExpression(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitLabelExpression(this, data);
	}
}
