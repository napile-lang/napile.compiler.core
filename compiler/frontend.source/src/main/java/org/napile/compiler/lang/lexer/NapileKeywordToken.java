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

/*
 * @author max
 */
package org.napile.compiler.lang.lexer;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class NapileKeywordToken extends NapileToken
{

	/**
	 * Generate soft keyword (identifier that has a keyword meaning only in some contexts)
	 */
	public static NapileKeywordToken keyword(String value)
	{
		return new NapileKeywordToken(value, false);
	}

	/**
	 * Generate keyword (identifier that has a keyword meaning in all possible contexts)
	 */
	public static NapileKeywordToken softKeyword(String value)
	{
		return new NapileKeywordToken(value, true);
	}

	private final String myValue;
	private final boolean myIsSoft;

	protected NapileKeywordToken(@NotNull @NonNls String value, boolean isSoft)
	{
		super(value);
		myValue = value;
		myIsSoft = isSoft;
	}

	public String getValue()
	{
		return myValue;
	}


	public boolean isSoft()
	{
		return myIsSoft;
	}
}
