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

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.NapileNodeTypes;
import com.intellij.lang.ASTNode;

/**
 * @author max
 */
public class NapileDelegatorToSuperCall extends NapileDelegationSpecifier implements NapileCallElement
{
	public NapileDelegatorToSuperCall(@NotNull ASTNode node)
	{
		super(node);
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitDelegationToSuperCallSpecifier(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitDelegationToSuperCallSpecifier(this, data);
	}

	@NotNull
	@Override
	public NapileConstructorCalleeExpression getCalleeExpression()
	{
		return (NapileConstructorCalleeExpression) findChildByType(NapileNodeTypes.CONSTRUCTOR_CALLEE);
	}

	@Nullable
	public NapileValueArgumentList getValueArgumentList()
	{
		return (NapileValueArgumentList) findChildByType(NapileNodeTypes.VALUE_ARGUMENT_LIST);
	}

	@NotNull
	public List<? extends ValueArgument> getValueArguments()
	{
		NapileValueArgumentList list = getValueArgumentList();
		return list != null ? list.getArguments() : Collections.<NapileValueArgument>emptyList();
	}

	@NotNull
	@Override
	public List<NapileExpression> getFunctionLiteralArguments()
	{
		return Collections.emptyList();
	}

	@Override
	public NapileTypeReference getTypeReference()
	{
		return getCalleeExpression().getTypeReference();
	}

	@NotNull
	@Override
	public List<NapileTypeReference> getTypeArguments()
	{
		NapileTypeArgumentList typeArgumentList = getTypeArgumentList();
		if(typeArgumentList == null)
		{
			return Collections.emptyList();
		}
		return typeArgumentList.getArguments();
	}

	@Override
	public NapileTypeArgumentList getTypeArgumentList()
	{
		final NapileUserType userType = getTypeAsUserType();
		return userType != null ? userType.getTypeArgumentList() : null;
	}
}