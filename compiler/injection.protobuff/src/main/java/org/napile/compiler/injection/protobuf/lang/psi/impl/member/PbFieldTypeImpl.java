package org.napile.compiler.injection.protobuf.lang.psi.impl.member;

import com.intellij.lang.ASTNode;
import org.napile.compiler.injection.protobuf.lang.psi.api.member.PbFieldType;
import org.napile.compiler.injection.protobuf.lang.psi.api.reference.PbRef;
import org.napile.compiler.injection.protobuf.lang.psi.impl.PbPsiElementImpl;

import static org.napile.compiler.injection.protobuf.lang.psi.PbPsiEnums.*;

/**
 * @author Nikolay Matveev
 *         Date: Mar 11, 2010
 */
public class PbFieldTypeImpl extends PbPsiElementImpl implements PbFieldType
{
	public PbFieldTypeImpl(ASTNode node)
	{
		super(node);
	}

	@Override
	public FieldType getType()
	{
		if(findChildByClass(PbRef.class) != null)
		{
			return FieldType.CUSTOM_TYPE;
		}
		return FieldType.BUILT_IN_TYPE;
	}

	@Override
	public PbRef getTypeRef()
	{
		return findChildByClass(PbRef.class);
	}
}
