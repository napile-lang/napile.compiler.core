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

package org.napile.compiler.lang.psi;

import com.intellij.util.ArrayFactory;

/**
 * @author Nikolay Krasko
 */
public interface NapileDeclaration extends NapileExpression, NapileModifierListOwner
{
	NapileDeclaration[] EMPTY_ARRAY = new NapileDeclaration[0];

	ArrayFactory<NapileDeclaration> ARRAY_FACTORY = new ArrayFactory<NapileDeclaration>()
	{
		@Override
		public NapileDeclaration[] create(int count)
		{
			return count == 0 ? EMPTY_ARRAY : new NapileDeclaration[count];
		}
	};
}
