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

package org.napile.idea.plugin.injection.text;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.injection.text.TextCodeInjection;
import org.napile.idea.plugin.IdeaInjectionSupport;
import org.napile.idea.plugin.highlighter.InjectionSyntaxHighlighter;

/**
 * @author VISTALL
 * @date 21:28/09.11.12
 */
public class TextIdeaInjectionSupport implements IdeaInjectionSupport<TextCodeInjection>
{
	@NotNull
	@Override
	public InjectionSyntaxHighlighter createSyntaxHighlighter()
	{
		return InjectionSyntaxHighlighter.EMPTY;
	}

	@NotNull
	@Override
	public Class<TextCodeInjection> getInjectionType()
	{
		return TextCodeInjection.class;
	}
}
