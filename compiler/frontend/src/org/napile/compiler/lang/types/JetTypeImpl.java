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

package org.napile.compiler.lang.types;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.annotations.AnnotatedImpl;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.types.checker.JetTypeChecker;

/**
 * @author abreslav
 */
public final class JetTypeImpl extends AnnotatedImpl implements JetType
{

	private final TypeConstructor constructor;
	private final List<JetType> arguments;
	private final boolean nullable;
	private JetScope memberScope;

	public JetTypeImpl(List<AnnotationDescriptor> annotations, TypeConstructor constructor, boolean nullable, @NotNull List<JetType> arguments, JetScope memberScope)
	{
		super(annotations);

		if(memberScope instanceof ErrorUtils.ErrorScope)
		{
			throw new IllegalStateException();
		}

		this.constructor = constructor;
		this.nullable = nullable;
		this.arguments = arguments;
		this.memberScope = memberScope;
	}

	public JetTypeImpl(TypeConstructor constructor, JetScope memberScope)
	{
		this(Collections.<AnnotationDescriptor>emptyList(), constructor, false, Collections.<JetType>emptyList(), memberScope);
	}

	public JetTypeImpl(@NotNull ClassDescriptor classDescriptor)
	{
		this(Collections.<AnnotationDescriptor>emptyList(), classDescriptor.getTypeConstructor(), false, Collections.<JetType>emptyList(), classDescriptor.getMemberScope(Collections.<JetType>emptyList()));
	}

	@NotNull
	@Override
	public TypeConstructor getConstructor()
	{
		return constructor;
	}

	@NotNull
	@Override
	public List<JetType> getArguments()
	{
		return arguments;
	}

	@Override
	public boolean isNullable()
	{
		return nullable;
	}

	@NotNull
	@Override
	public JetScope getMemberScope()
	{
		if(memberScope == null)
		{
			// TODO : this was supposed to mean something...
			throw new IllegalStateException(this.toString());
		}
		return memberScope;
	}

	@Override
	public String toString()
	{
		return constructor + (arguments.isEmpty() ? "" : "<" + argumentsToString() + ">") + (isNullable() ? "?" : "");
	}

	private StringBuilder argumentsToString()
	{
		StringBuilder stringBuilder = new StringBuilder();
		for(Iterator<JetType> iterator = arguments.iterator(); iterator.hasNext(); )
		{
			JetType argument = iterator.next();
			stringBuilder.append(argument);
			if(iterator.hasNext())
			{
				stringBuilder.append(", ");
			}
		}
		return stringBuilder;
	}

	@Override
	public boolean equals(Object o)
	{
		if(this == o)
			return true;
		if(o == null || getClass() != o.getClass())
			return false;

		JetTypeImpl type = (JetTypeImpl) o;

		// TODO
		return nullable == type.nullable && JetTypeChecker.INSTANCE.equalTypes(this, type);
		//        if (nullable != type.nullable) return false;
		//        if (arguments != null ? !arguments.equals(type.arguments) : type.arguments != null) return false;
		//        if (constructor != null ? !constructor.equals(type.constructor) : type.constructor != null) return false;
		//        if (memberScope != null ? !memberScope.equals(type.memberScope) : type.memberScope != null) return false;

		//        return true;
	}

	@Override
	public int hashCode()
	{
		int result = constructor != null ? constructor.hashCode() : 0;
		result = 31 * result + (arguments != null ? arguments.hashCode() : 0);
		result = 31 * result + (nullable ? 1 : 0);
		return result;
	}
}
