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

package org.napile.compiler.lang.psi.stubs;

import org.napile.compiler.lang.psi.NapileTypeParameterList;
import org.napile.compiler.lang.psi.stubs.elements.NapileStubElementTypes;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;

/**
 * @author Nikolay Krasko
 */
public class NapilePsiTypeParameterListStub extends StubBase<NapileTypeParameterList>
{
	public NapilePsiTypeParameterListStub(final StubElement parent)
	{
		super(parent, NapileStubElementTypes.TYPE_PARAMETER_LIST);
	}

	@Override
	public String toString()
	{
		return "NapilePsiTypeParameterListStub";
	}
}
