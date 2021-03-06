/*
 * Copyright 2010-2013 napile.org
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

package org.napile.doc.lang.lexer;

import java.lang.reflect.Constructor;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.napile.doc.lang.NapileDocLanguage;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;

/**
 * @author VISTALL
 * @since 9:22/31.01.13
 */
public class NapileDocNode extends IElementType
{
	private final Constructor<? extends PsiElement> constructor;

	public NapileDocNode(@NotNull @NonNls String debugName, Class<? extends PsiElement> psiClass)
	{
		super(debugName, NapileDocLanguage.INSTANCE);
		try
		{
			constructor = psiClass != null ? psiClass.getConstructor(ASTNode.class) : null;
		}
		catch(NoSuchMethodException e)
		{
			throw new RuntimeException("Must have a constructor with ASTNode");
		}
	}

	public PsiElement createPsi(ASTNode node)
	{
		assert node.getElementType() == this;

		try
		{
			return constructor.newInstance(node);
		}
		catch(Exception e)
		{
			throw new RuntimeException("Error creating psi element for node", e);
		}
	}
}
