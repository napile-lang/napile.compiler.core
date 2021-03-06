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

package org.napile.compiler.lang.diagnostics.rendering;

import static org.napile.compiler.lang.diagnostics.rendering.DiagnosticRendererUtil.renderParameter;

import java.text.MessageFormat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.diagnostics.DiagnosticWithParameters1;

/**
 * @author Evgeny Gerashchenko
 * @since 4/12/12
 */
public class DiagnosticWithParameters1Renderer<A> implements DiagnosticRenderer<DiagnosticWithParameters1<?, A>>
{
	private final MessageFormat messageFormat;
	private final Renderer<? super A> rendererForA;

	public DiagnosticWithParameters1Renderer(@NotNull String message, @Nullable Renderer<? super A> rendererForA)
	{
		this.messageFormat = new MessageFormat(message);
		this.rendererForA = rendererForA;
	}

	@NotNull
	@Override
	public String render(@NotNull DiagnosticWithParameters1<?, A> diagnostic)
	{
		return messageFormat.format(new Object[]{renderParameter(diagnostic.getA(), rendererForA)});
	}
}
