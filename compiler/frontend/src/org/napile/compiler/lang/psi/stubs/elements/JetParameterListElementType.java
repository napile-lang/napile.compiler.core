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
import org.napile.compiler.lang.psi.NapileParameterList;
import org.napile.compiler.lang.psi.stubs.PsiJetParameterListStub;
import org.napile.compiler.lang.psi.stubs.impl.PsiJetParameterListStubImpl;
import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;

/**
 * @author Nikolay Krasko
 */
public class JetParameterListElementType extends JetStubElementType<PsiJetParameterListStub, NapileParameterList>
{
	public JetParameterListElementType(@NotNull @NonNls String debugName)
	{
		super(debugName);
	}

	@Override
	public NapileParameterList createPsiFromAst(@NotNull ASTNode node)
	{
		return new NapileParameterList(node);
	}

	@Override
	public NapileParameterList createPsi(@NotNull PsiJetParameterListStub stub)
	{
		return new NapileParameterList(stub, JetStubElementTypes.VALUE_PARAMETER_LIST);
	}

	@Override
	public PsiJetParameterListStub createStub(@NotNull NapileParameterList psi, StubElement parentStub)
	{
		return new PsiJetParameterListStubImpl(JetStubElementTypes.VALUE_PARAMETER_LIST, parentStub);
	}

	@Override
	public void serialize(PsiJetParameterListStub stub, StubOutputStream dataStream) throws IOException
	{
		// Nothing to serialize
	}

	@Override
	public PsiJetParameterListStub deserialize(StubInputStream dataStream, StubElement parentStub) throws IOException
	{
		return new PsiJetParameterListStubImpl(JetStubElementTypes.VALUE_PARAMETER_LIST, parentStub);
	}

	@Override
	public void indexStub(PsiJetParameterListStub stub, IndexSink sink)
	{
		// No index
	}
}
