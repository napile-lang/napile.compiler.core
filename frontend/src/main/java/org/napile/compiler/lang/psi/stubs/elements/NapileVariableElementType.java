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
import org.napile.compiler.lang.psi.NapileVariable;
import org.napile.compiler.lang.psi.impl.NapileVariableImpl;
import org.napile.compiler.lang.psi.stubs.NapilePsiVariableStub;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;

/**
 * @author Nikolay Krasko
 */
public class NapileVariableElementType extends NapileStubElementType<NapilePsiVariableStub, NapileVariable>
{
	public NapileVariableElementType(@NotNull @NonNls String debugName)
	{
		super(debugName);
	}

	@Override
	public NapileVariable createPsiFromAst(@NotNull ASTNode node)
	{
		return new NapileVariableImpl(node);
	}

	@Override
	public NapileVariable createPsi(@NotNull NapilePsiVariableStub stub)
	{
		return getPsiFactory(stub).createVariable(stub);
	}

	@Override
	public boolean shouldCreateStub(ASTNode node)
	{
		if(super.shouldCreateStub(node))
		{
			PsiElement psi = node.getPsi();
			if(psi instanceof NapileVariable)
			{
				NapileVariable property = (NapileVariable) psi;
				return property.getName() != null;
			}
		}

		return false;
	}

	@Override
	public NapilePsiVariableStub createStub(@NotNull NapileVariable psi, StubElement parentStub)
	{
		return new NapilePsiVariableStub(parentStub, psi.getName());
	}

	@Override
	public void serialize(NapilePsiVariableStub stub, StubOutputStream dataStream) throws IOException
	{
		dataStream.writeName(stub.getName());
	}

	@Override
	public NapilePsiVariableStub deserialize(StubInputStream dataStream, StubElement parentStub) throws IOException
	{
		StringRef name = dataStream.readName();
		return new NapilePsiVariableStub(parentStub, name);
	}

	@Override
	public void indexStub(NapilePsiVariableStub stub, IndexSink sink)
	{
		StubIndexServiceFactory.getInstance().indexVariable(stub, sink);
	}
}
