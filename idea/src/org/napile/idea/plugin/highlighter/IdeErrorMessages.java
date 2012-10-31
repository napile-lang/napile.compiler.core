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

package org.napile.idea.plugin.highlighter;

import static org.napile.compiler.lang.diagnostics.Errors.*;
import static org.napile.compiler.lang.diagnostics.rendering.Renderers.RENDER_CLASS_OR_OBJECT;
import static org.napile.compiler.lang.diagnostics.rendering.Renderers.TO_STRING;
import static org.napile.compiler.lang.diagnostics.rendering.TabledDescriptorRenderer.TextElementType;
import static org.napile.idea.plugin.highlighter.HtmlTabledDescriptorRenderer.tableForTypes;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.CallableMemberDescriptor;
import org.napile.compiler.lang.diagnostics.Diagnostic;
import org.napile.compiler.lang.diagnostics.rendering.DefaultErrorMessages;
import org.napile.compiler.lang.diagnostics.rendering.DiagnosticFactoryToRendererMap;
import org.napile.compiler.lang.diagnostics.rendering.DiagnosticRenderer;
import org.napile.compiler.lang.diagnostics.rendering.DispatchingDiagnosticRenderer;
import org.napile.compiler.lang.diagnostics.rendering.Renderer;
import org.napile.compiler.resolve.DescriptorRenderer;


/**
 * @author Evgeny Gerashchenko
 * @see DefaultErrorMessages
 * @since 4/13/12
 */
public class IdeErrorMessages
{
	public static final DiagnosticFactoryToRendererMap MAP = new DiagnosticFactoryToRendererMap();
	public static final DiagnosticRenderer<Diagnostic> RENDERER = new DispatchingDiagnosticRenderer(MAP, DefaultErrorMessages.MAP);

	static
	{
		MAP.put(TYPE_MISMATCH, "<html>Type mismatch.<table><tr><td>Required:</td><td>{0}</td></tr><tr><td>Found:</td><td>{1}</td></tr></table></html>", IdeRenderers.HTML_RENDER_TYPE, IdeRenderers.HTML_RENDER_TYPE);

		MAP.put(TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS, "<html>Type inference failed: {0}</html>", IdeRenderers.HTML_TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS_RENDERER);
		MAP.put(TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER, "<html>Type inference failed: {0}</html>", IdeRenderers.HTML_TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER_RENDERER);
		MAP.put(TYPE_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH, "<html>Type inference failed: {0}</html>", IdeRenderers.HTML_TYPE_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH_RENDERER);
		MAP.put(TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH, tableForTypes("Type inference failed. Expected type mismatch: ", "found: ", TextElementType.ERROR, "required: ", TextElementType.STRONG), IdeRenderers.HTML_RENDER_TYPE, IdeRenderers.HTML_RENDER_TYPE);
		MAP.put(TYPE_INFERENCE_UPPER_BOUND_VIOLATED, "<html>{0}</html>", IdeRenderers.HTML_TYPE_INFERENCE_UPPER_BOUND_VIOLATED_RENDERER);

		MAP.put(WRONG_SETTER_PARAMETER_TYPE, "<html>Setter parameter type must be equal to the type of the property." +
				"<table><tr><td>Expected:</td><td>{0}</td></tr>" +
				"<tr><td>Found:</td><td>{1}</td></tr></table></html>", IdeRenderers.HTML_RENDER_TYPE, IdeRenderers.HTML_RENDER_TYPE);
		MAP.put(WRONG_GETTER_RETURN_TYPE, "<html>Getter return type must be equal to the type of the property." +
				"<table><tr><td>Expected:</td><td>{0}</td></tr>" +
				"<tr><td>Found:</td><td>{1}</td></tr></table></html>", IdeRenderers.HTML_RENDER_TYPE, IdeRenderers.HTML_RENDER_TYPE);

		MAP.put(ITERATOR_AMBIGUITY, "<html>Method ''iterator()'' is ambiguous for this expression.<ul>{0}</ul></html>", IdeRenderers.HTML_AMBIGUOUS_CALLS);

		MAP.put(UPPER_BOUND_VIOLATED, "<html>Type argument is not within its bounds." +
				"<table><tr><td>Expected:</td><td>{0}</td></tr>" +
				"<tr><td>Found:</td><td>{1}</td></tr></table></html>", IdeRenderers.HTML_RENDER_TYPE, IdeRenderers.HTML_RENDER_TYPE);

		MAP.put(TYPE_MISMATCH_IN_FOR_LOOP, "<html>Loop parameter type mismatch." +
				"<table><tr><td>Iterated values:</td><td>{0}</td></tr>" +
				"<tr><td>Parameter:</td><td>{1}</td></tr></table></html>", IdeRenderers.HTML_RENDER_TYPE, IdeRenderers.HTML_RENDER_TYPE);

		MAP.put(RETURN_TYPE_MISMATCH_ON_OVERRIDE, "<html>Return type is ''{0}'', which is not a subtype of overridden<br/>" + "{1}</html>", new Renderer<CallableMemberDescriptor>()
		{
			@NotNull
			@Override
			public String render(@NotNull CallableMemberDescriptor object)
			{
				return DescriptorRenderer.HTML.renderType(object.getReturnType());
			}
		}, DescriptorRenderer.HTML);

		MAP.put(VAR_OVERRIDDEN_BY_VAL, "<html>Val-property cannot override var-property<br />" + "{1}</html>", DescriptorRenderer.HTML, DescriptorRenderer.HTML);

		MAP.put(ABSTRACT_MEMBER_NOT_IMPLEMENTED, "<html>{0} must be declared abstract or implement abstract member<br/>" + "{1}</html>", RENDER_CLASS_OR_OBJECT, DescriptorRenderer.HTML);

		MAP.put(MANY_IMPL_MEMBER_NOT_IMPLEMENTED, "<html>{0} must override {1}<br />because it inherits many implementations of it</html>", RENDER_CLASS_OR_OBJECT, DescriptorRenderer.HTML);
		MAP.put(CONFLICTING_OVERLOADS, "<html>{1}<br />is already defined in ''{0}''</html>", DescriptorRenderer.HTML, TO_STRING);

		MAP.put(RESULT_TYPE_MISMATCH, "<html>Method return type mismatch." +
				"<table><tr><td>Expected:</td><td>{1}</td></tr>" +
				"<tr><td>Found:</td><td>{2}</td></tr></table></html>", TO_STRING, IdeRenderers.HTML_RENDER_TYPE, IdeRenderers.HTML_RENDER_TYPE);

		MAP.put(OVERLOAD_RESOLUTION_AMBIGUITY, "<html>Overload resolution ambiguity. All these functions match. <ul>{0}</ul></html>", IdeRenderers.HTML_AMBIGUOUS_CALLS);
		MAP.put(NONE_APPLICABLE, "<html>None of the following functions can be called with the arguments supplied. <ul>{0}</ul></html>", new IdeRenderers.NoneApplicableCallsRenderer());

		MAP.setImmutable();
	}

	private IdeErrorMessages()
	{
	}
}
