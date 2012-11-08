/*
 * Copyright 2006 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.napile.compiler.injection.regexp.lang.psi.impl;

import com.intellij.lang.ASTNode;

import org.jetbrains.annotations.NotNull;

import org.napile.compiler.injection.regexp.lang.parser.RegExpTokens;
import org.napile.compiler.injection.regexp.lang.psi.RegExpClass;
import org.napile.compiler.injection.regexp.lang.psi.RegExpElementVisitor;
import org.napile.compiler.injection.regexp.lang.psi.RegExpClassElement;
import org.napile.compiler.injection.regexp.lang.parser.RegExpElementTypes;

public class RegExpClassImpl extends RegExpElementImpl implements RegExpClass
{

	public RegExpClassImpl(ASTNode astNode)
	{
		super(astNode);
	}

	public boolean isNegated()
	{
		final ASTNode node = getNode().getFirstChildNode();
		return node != null && node.getElementType() == RegExpTokens.CARET;
	}

	@NotNull
	public RegExpClassElement[] getElements()
	{
		final ASTNode[] nodes = getNode().getChildren(RegExpElementTypes.CLASS_ELEMENTS);
		RegExpClassElement[] e = new RegExpClassElement[nodes.length];
		for(int i = 0; i < e.length; i++)
		{
			e[i] = (RegExpClassElement) nodes[i].getPsi();
		}
		return e;
	}

	public void accept(RegExpElementVisitor visitor)
	{
		visitor.visitRegExpClass(this);
	}
}
