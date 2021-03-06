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

package org.napile.compiler.lang.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.StubBasedPsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;

/**
 * @author VISTALL
 * @since 13:57/16.02.13
 */
public abstract class NXmlStubElementBase<S extends StubElement> extends NXmlElementBase implements StubBasedPsiElement<S>
{
	private final S stub;

	protected NXmlStubElementBase(S stub)
	{
		this.stub = stub;
	}

	@Override
	public PsiElement getParent()
	{
		return stub.getParentStub().getPsi();
	}

	@Override
	public abstract IStubElementType<S, ?> getElementType();

	@Override
	public S getStub()
	{
		return stub;
	}
}
