/*
 * Copyright 2010-2013 napile.org
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

package org.napile.doc.compiler.info;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NapileNamedDeclaration;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.doc.lang.psi.NapileDoc;
import org.napile.doc.lang.psi.NapileDocLine;

/**
 * @author VISTALL
 * @since 18:18/01.02.13
 */
public abstract class NamedDocableInfo<E extends NapileNamedDeclaration> extends DocableInfo<E>
{
	public NamedDocableInfo(BindingTrace bindingContext, E element)
	{
		super(bindingContext, element);
	}

	@NotNull
	public abstract String getDeclaration();

	@NotNull
	@Override
	public String getName()
	{
		return element.getName();
	}

	@NotNull
	@Override
	public String getDoc()
	{
		NapileDoc napileDoc = element.getDocComment();
		if(napileDoc == null)
		{
			return "";
		}

		StringBuilder builder = new StringBuilder();
		for(NapileDocLine line : napileDoc.getLines())
		{
			builder.append(line.getText()).append("\n\r");
		}

		return PEG_DOWN_PROCESSOR.markdownToHtml(builder.toString());
	}
}
