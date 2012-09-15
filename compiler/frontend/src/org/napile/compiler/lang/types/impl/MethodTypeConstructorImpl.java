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

package org.napile.compiler.lang.types.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.descriptors.ClassifierDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.MethodTypeConstructor;

/**
 * @author VISTALL
 * @date 12:17/15.09.12
 */
public class MethodTypeConstructorImpl implements MethodTypeConstructor
{
	@NotNull
	@Override
	public List<TypeParameterDescriptor> getParameters()
	{
		return Collections.emptyList();
	}

	@NotNull
	@Override
	public Collection<? extends JetType> getSupertypes()
	{
		return Collections.emptyList();
	}

	@Override
	public boolean isSealed()
	{
		return false;
	}

	@Nullable
	@Override
	public ClassifierDescriptor getDeclarationDescriptor()
	{
		return null;
	}

	@Override
	public List<AnnotationDescriptor> getAnnotations()
	{
		return Collections.emptyList();
	}
}
