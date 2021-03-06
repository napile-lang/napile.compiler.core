/*
 * Copyright 2010-2012 napile.org
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

import java.util.List;

import org.jetbrains.annotations.NotNull;
import com.intellij.psi.PsiElement;
import com.intellij.util.ArrayFactory;

/**
 * @author VISTALL
 * @since 15:11/19.10.12
 */
public interface NapileConstructor extends NapileDeclarationWithBody, NapileStatementExpression, NapileDelegationSpecifierListOwner, NapileNamedDeclaration
{
	NapileConstructor[] EMPTY_ARRAY = new NapileConstructor[0];

	ArrayFactory<NapileConstructor> ARRAY_FACTORY = new ArrayFactory<NapileConstructor>()
	{
		@Override
		public NapileConstructor[] create(int count)
		{
			return count == 0 ? EMPTY_ARRAY : new NapileConstructor[count];
		}
	};

	@Override
	@NotNull
	PsiElement getNameIdentifier();

	@NotNull
	List<NapileTypeReference> getSuperCallTypeList();
}
