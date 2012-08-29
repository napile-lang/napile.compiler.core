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

package org.napile.compiler.lang.resolve.lazy;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NapileLikeClass;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileNamedFunction;
import org.napile.compiler.lang.psi.NapileProperty;
import org.napile.compiler.lang.resolve.name.FqName;
import org.napile.compiler.lang.resolve.name.Name;

/**
 * @author abreslav
 */
public class EmptyPackageMemberDeclarationProvider implements PackageMemberDeclarationProvider
{

	public static final EmptyPackageMemberDeclarationProvider INSTANCE = new EmptyPackageMemberDeclarationProvider();

	private EmptyPackageMemberDeclarationProvider()
	{
	}

	@Override
	public boolean isPackageDeclared(@NotNull Name name)
	{
		return false;
	}

	@Override
	public Collection<FqName> getAllDeclaredPackages()
	{
		return Collections.emptyList();
	}

	@Override
	public List<NapileDeclaration> getAllDeclarations()
	{
		return Collections.emptyList();
	}

	@NotNull
	@Override
	public Collection<NapileNamedFunction> getFunctionDeclarations(@NotNull Name name)
	{
		return Collections.emptyList();
	}

	@NotNull
	@Override
	public Collection<NapileProperty> getPropertyDeclarations(@NotNull Name name)
	{
		return Collections.emptyList();
	}

	@Override
	public NapileLikeClass getClassOrObjectDeclaration(@NotNull Name name)
	{
		return null;
	}
}
