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
import com.intellij.psi.tree.IElementType;

import org.jetbrains.annotations.NotNull;

import org.napile.compiler.injection.regexp.lang.parser.RegExpTokens;
import org.napile.compiler.injection.regexp.lang.psi.RegExpElementVisitor;
import org.napile.compiler.injection.regexp.lang.psi.RegExpBoundary;

public class RegExpBoundaryImpl extends RegExpElementImpl implements RegExpBoundary
{
	public RegExpBoundaryImpl(ASTNode astNode)
	{
		super(astNode);
	}

	@NotNull
	public Type getType()
	{
		final IElementType type = getNode().getElementType();
		if(type == RegExpTokens.CARET)
		{
			return Type.LINE_START;
		}
		else if(type == RegExpTokens.DOLLAR)
		{
			return Type.LINE_END;
		}
		else
		{
			assert type == RegExpTokens.BOUNDARY;
			final String s = getUnescapedText();
			if(s.equals("\\b"))
			{
				return Type.WORD;
			}
			else if(s.equals("\\B))"))
			{
				return Type.NON_WORD;
			}
			else if(s.equals("\\A))"))
			{
				return Type.BEGIN;
			}
			else if(s.equals("\\Z))"))
			{
				return Type.END_NO_LINE_TERM;
			}
			else if(s.equals("\\z))"))
			{
				return Type.END;
			}
			else if(s.equals("\\G))"))
			{
				return Type.PREVIOUS_MATCH;
			}
		}
		assert false;
		return null;
	}

	public void accept(RegExpElementVisitor visitor)
	{
		visitor.visitRegExpBoundary(this);
	}
}
