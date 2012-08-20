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

package org.napile.compiler.lang.psi.stubs.impl;

import org.napile.compiler.lang.psi.NapileTypeParameter;
import org.napile.compiler.lang.psi.stubs.PsiJetTypeParameterStub;
import org.napile.compiler.lang.psi.stubs.elements.JetTypeParameterElementType;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.io.StringRef;

/**
 * @author Nikolay Krasko
 */
public class PsiJetTypeParameterStubImpl extends StubBase<NapileTypeParameter> implements PsiJetTypeParameterStub
{
	private final StringRef name;
	private final StringRef extendBoundTypeText;

	public PsiJetTypeParameterStubImpl(JetTypeParameterElementType type, final StubElement parent, StringRef name, StringRef extendBoundTypeText)
	{
		super(parent, type);

		this.name = name;
		this.extendBoundTypeText = extendBoundTypeText;
	}

	public PsiJetTypeParameterStubImpl(JetTypeParameterElementType type, final StubElement parent, String name, String extendBoundTypeText)
	{
		this(type, parent, StringRef.fromString(name), StringRef.fromString(extendBoundTypeText));
	}

	@Override
	public String getExtendBoundTypeText()
	{
		return StringRef.toString(extendBoundTypeText);
	}

	@Override
	public String getName()
	{
		return StringRef.toString(name);
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("PsiJetTypeParameterStubImpl[");

		builder.append("name=").append(getName());
		builder.append(" extendText=").append(getExtendBoundTypeText());

		builder.append("]");

		return builder.toString();
	}
}
