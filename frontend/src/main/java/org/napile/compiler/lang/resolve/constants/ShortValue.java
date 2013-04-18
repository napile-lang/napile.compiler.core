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

package org.napile.compiler.lang.resolve.constants;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.compiler.lang.resolve.scopes.NapileScope;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.lang.types.TypeUtils;
import com.google.common.base.Function;

/**
 * @author abreslav
 */
public class ShortValue implements CompileTimeConstant<Short>
{
	public static final Function<Long, ShortValue> CREATE = new Function<Long, ShortValue>()
	{
		@Override
		public ShortValue apply(@Nullable Long input)
		{
			assert input != null;
			return new ShortValue(input.shortValue());
		}
	};

	private final short value;

	public ShortValue(short value)
	{
		this.value = value;
	}

	@Override
	public Short getValue()
	{
		return value;
	}

	@Nullable
	@Override
	public NapileType getType(@NotNull NapileScope napileScope)
	{
		return TypeUtils.getTypeOfClassOrErrorType(napileScope, NapileLangPackage.SHORT, false);
	}

	@Override
	public String toString()
	{
		return value + ".toShort()";
	}
}
