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

package org.napile.compiler.lang.psi.impl;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NXmlParentedElementBase;
import org.napile.compiler.lang.psi.NapileNullableType;
import org.napile.compiler.lang.psi.NapileTypeElement;
import org.napile.compiler.lang.psi.NapileTypeReference;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import org.napile.compiler.util.NXmlMirrorUtil;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeElement;

/**
 * @author VISTALL
 * @since 17:04/16.02.13
 */
public class NXmlNullableTypeImpl extends NXmlParentedElementBase implements NapileNullableType
{
	private NapileTypeElement innerType;

	public NXmlNullableTypeImpl(PsiElement parent, PsiElement mirror)
	{
		super(parent, mirror);
	}

	@Override
	public void setMirror(@NotNull TreeElement element) throws InvalidMirrorException
	{
		setMirrorCheckingType(element, null);

		NapileNullableType mirror = SourceTreeToPsiMap.treeToPsiNotNull(element);

		innerType = NXmlMirrorUtil.mirrorTypeElement(this, mirror.getInnerType());
	}

	@NotNull
	@Override
	public PsiElement[] getChildren()
	{
		return new PsiElement[] {innerType};
	}

	@NotNull
	@Override
	public ASTNode getQuestionMarkNode()
	{
		return null;
	}

	@NotNull
	@Override
	public NapileTypeElement getInnerType()
	{
		return innerType;
	}

	@NotNull
	@Override
	public List<? extends NapileTypeReference> getTypeArguments()
	{
		return getInnerType().getTypeArguments();
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitNullableType(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitNullableType(this, data);
	}
}
