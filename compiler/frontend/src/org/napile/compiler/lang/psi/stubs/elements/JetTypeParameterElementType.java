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

package org.napile.compiler.lang.psi.stubs.elements;

import java.io.IOException;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NapileTypeParameter;
import org.napile.compiler.lang.psi.NapileTypeReference;
import org.napile.compiler.lang.psi.stubs.PsiJetTypeParameterStub;
import org.napile.compiler.lang.psi.stubs.impl.PsiJetTypeParameterStubImpl;
import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;

/**
 * @author Nikolay Krasko
 */
public class JetTypeParameterElementType extends JetStubElementType<PsiJetTypeParameterStub, NapileTypeParameter>
{
	public JetTypeParameterElementType(@NotNull @NonNls String debugName)
	{
		super(debugName);
	}

	@Override
	public NapileTypeParameter createPsiFromAst(@NotNull ASTNode node)
	{
		return new NapileTypeParameter(node);
	}

	@Override
	public NapileTypeParameter createPsi(@NotNull PsiJetTypeParameterStub stub)
	{
		return new NapileTypeParameter(stub, JetStubElementTypes.TYPE_PARAMETER);
	}

	@Override
	public PsiJetTypeParameterStub createStub(@NotNull NapileTypeParameter psi, StubElement parentStub)
	{
		NapileTypeReference[] extendsBound = psi.getExtendsBound();
		StringRef[] stringRefs = StringRef.createArray(extendsBound.length);
		for(int i = 0; i < extendsBound.length; i++)
			stringRefs[i] = StringRef.fromString(extendsBound[i].getText());

		return new PsiJetTypeParameterStubImpl(JetStubElementTypes.TYPE_PARAMETER, parentStub, psi.getName(), stringRefs);
	}

	@Override
	public void serialize(PsiJetTypeParameterStub stub, StubOutputStream dataStream) throws IOException
	{
		dataStream.writeName(stub.getName());
		StringRef[] refs = stub.getExtendBoundTypeText();
		dataStream.writeInt(refs.length);
		for(StringRef stringRef : refs)
			dataStream.writeName(stringRef.getString());
	}

	@Override
	public PsiJetTypeParameterStub deserialize(StubInputStream dataStream, StubElement parentStub) throws IOException
	{
		StringRef name = dataStream.readName();
		int count = dataStream.readInt();

		StringRef[] refs = new StringRef[count];
		for(int i = 0; i < count; i++)
			refs[i] = dataStream.readName();

		return new PsiJetTypeParameterStubImpl(JetStubElementTypes.TYPE_PARAMETER, parentStub, name, refs);
	}

	@Override
	public void indexStub(PsiJetTypeParameterStub stub, IndexSink sink)
	{
		// No index
	}
}
