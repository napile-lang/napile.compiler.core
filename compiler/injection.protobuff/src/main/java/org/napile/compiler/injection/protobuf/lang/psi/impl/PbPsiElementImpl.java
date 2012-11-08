package org.napile.compiler.injection.protobuf.lang.psi.impl;

import org.jetbrains.annotations.NotNull;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.napile.compiler.injection.protobuf.lang.psi.PbPsiElementVisitor;
import org.napile.compiler.injection.protobuf.lang.psi.api.PbFile;
import org.napile.compiler.injection.protobuf.lang.psi.api.PbPsiElement;
import org.napile.compiler.injection.protobuf.lang.psi.api.declaration.PbEnumDef;
import org.napile.compiler.injection.protobuf.lang.psi.api.declaration.PbExtendDef;
import org.napile.compiler.injection.protobuf.lang.psi.api.declaration.PbExtensionsDef;
import org.napile.compiler.injection.protobuf.lang.psi.api.declaration.PbFieldDef;
import org.napile.compiler.injection.protobuf.lang.psi.api.declaration.PbGroupDef;
import org.napile.compiler.injection.protobuf.lang.psi.api.declaration.PbMessageDef;
import org.napile.compiler.injection.protobuf.lang.psi.api.declaration.PbServiceMethodDef;
import org.napile.compiler.injection.protobuf.lang.psi.api.member.PbOptionList;
import org.napile.compiler.injection.protobuf.lang.psi.impl.member.PbOptionAssignmentImpl;

/**
 * @author Nikolay Matveev
 */

public class PbPsiElementImpl extends ASTWrapperPsiElement implements PbPsiElement
{

	public PbPsiElementImpl(ASTNode node)
	{
		super(node);
	}

	@Override
	public String toString()
	{
		return this.getNode().getElementType().toString();
	}

	@Override
	public final void accept(@NotNull PsiElementVisitor visitor)
	{
		if(visitor instanceof PbPsiElementVisitor)
		{
			accept((PbPsiElementVisitor) visitor);
		}
		else
		{
			super.accept(visitor);
		}
	}

	@Override
	public void accept(@NotNull PbPsiElementVisitor visitor)
	{
		visitor.visitPbElement(this);
	}

	@Override
	public PsiElement getContext()
	{
		//common types
		if(this instanceof PbMessageDef || this instanceof PbEnumDef || this instanceof PbExtendDef)
		{
			PsiElement parent = getParent();
			return parent;
		}
		//only nested types
		if(this instanceof PbServiceMethodDef || this instanceof PbFieldDef || this instanceof PbGroupDef || this instanceof PbExtensionsDef)
		{
			return getParent().getParent();
		}
		if(this instanceof PbOptionAssignmentImpl)
		{
			PsiElement parent = getParent();
			if(parent instanceof PbFile || parent instanceof PbOptionList)
			{
				return parent;
			}
			return parent.getParent();
		}
		return super.getContext();    //To change body of overridden methods use File | Settings | File Templates.
	}
}
