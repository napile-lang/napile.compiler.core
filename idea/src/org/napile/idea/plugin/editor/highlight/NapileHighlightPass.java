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

package org.napile.idea.plugin.editor.highlight;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.analyzer.AnalyzeExhaust;
import org.napile.compiler.lang.diagnostics.AbstractDiagnosticFactory;
import org.napile.compiler.lang.diagnostics.Diagnostic;
import org.napile.compiler.lang.diagnostics.Errors;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.psi.NapileReferenceExpression;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.idea.plugin.editor.highlight.postHighlight.InjectionHighlightingVisitor;
import org.napile.idea.plugin.editor.highlight.postHighlight.LabelsHighlightingVisitor;
import org.napile.idea.plugin.editor.highlight.postHighlight.MethodssHighlightingVisitor;
import org.napile.idea.plugin.editor.highlight.postHighlight.PostHighlightVisitor;
import org.napile.idea.plugin.editor.highlight.postHighlight.SoftKeywordPostHighlightVisitor;
import org.napile.idea.plugin.editor.highlight.postHighlight.TypeKindHighlightingVisitor;
import org.napile.idea.plugin.editor.highlight.postHighlight.VariablesHighlightingVisitor;
import org.napile.idea.plugin.highlighter.JetPsiChecker;
import org.napile.idea.plugin.module.ModuleAnalyzerUtil;
import com.google.common.collect.ImmutableSet;
import com.intellij.codeHighlighting.TextEditorHighlightingPass;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.codeInsight.daemon.impl.UpdateHighlightersUtil;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.MultiRangeReference;
import com.intellij.psi.PsiReference;

/**
 * @author VISTALL
 * @date 19:21/26.02.13
 */
public class NapileHighlightPass extends TextEditorHighlightingPass
{
	public static final Set<? extends AbstractDiagnosticFactory> WARNINGS_LIKE_UNUSED = ImmutableSet.<AbstractDiagnosticFactory>builder().add(Errors.UNUSED_VARIABLE, Errors.UNUSED_PARAMETER, Errors.ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE).build();
	public static final Set<? extends AbstractDiagnosticFactory> UNRESOLVED_REFERENCES = ImmutableSet.<AbstractDiagnosticFactory>builder().add(Errors.NAMED_PARAMETER_NOT_FOUND, Errors.UNRESOLVED_REFERENCE).build();
	public static final Set<? extends AbstractDiagnosticFactory> REDECLARATION = ImmutableSet.<AbstractDiagnosticFactory>builder().add(Errors.REDECLARATION, Errors.NAME_SHADOWING).build();

	private final NapileFile file;
	private List<HighlightInfo> infos;

	protected NapileHighlightPass(@NotNull NapileFile file, @Nullable Document document)
	{
		super(file.getProject(), document);

		this.file = file;
	}

	@Override
	public void doCollectInformation(@NotNull ProgressIndicator progress)
	{
		infos = new ArrayList<HighlightInfo>();

		final AnalyzeExhaust analyze = ModuleAnalyzerUtil.analyze(file);
		final BindingContext bindingContext = analyze.getBindingContext();

		for(Diagnostic diagnostic : bindingContext.getDiagnostics())
		{
			if(!diagnostic.isValid() || diagnostic.getPsiFile() != file)
				continue;

			final List<TextRange> textRanges = diagnostic.getTextRanges();

			switch(diagnostic.getSeverity())
			{
				case INFO:
					// Generic annotation
					for(TextRange textRange : textRanges)
					{
						final HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(HighlightInfoType.INFORMATION);
						builder.range(textRange);
						builder.description(JetPsiChecker.getDefaultMessage(diagnostic));
						builder.escapedToolTip(JetPsiChecker.getTooltipMessage(diagnostic));
						if(diagnostic.getFactory() == Errors.VALID_STRING_ESCAPE)
						{
							builder.textAttributes(NapileHighlightingColors.VALID_STRING_ESCAPE);
						}

						final HighlightInfo e = builder.create();
						if(e != null)
						{
							NapileQuickFixProviderEP.callRegisterFor(diagnostic, e);
							infos.add(e);
						}
					}
					break;
				case ERROR:
					if(UNRESOLVED_REFERENCES.contains(diagnostic.getFactory()))
					{
						NapileReferenceExpression referenceExpression = (NapileReferenceExpression) diagnostic.getPsiElement();
						PsiReference reference = referenceExpression.getReference();
						if(reference instanceof MultiRangeReference)
						{
							MultiRangeReference mrr = (MultiRangeReference) reference;
							for(TextRange range : mrr.getRanges())
							{
								final HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(HighlightInfoType.ERROR);
								builder.range(range.shiftRight(referenceExpression.getTextOffset()));
								builder.description(JetPsiChecker.getDefaultMessage(diagnostic));
								builder.escapedToolTip(JetPsiChecker.getTooltipMessage(diagnostic));
								builder.textAttributes(CodeInsightColors.WRONG_REFERENCES_ATTRIBUTES);

								final HighlightInfo e = builder.create();
								if(e != null)
								{
									NapileQuickFixProviderEP.callRegisterFor(diagnostic, e);
									infos.add(e);
								}
							}
						}
						else
						{
							for(TextRange textRange : textRanges)
							{
								final HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(HighlightInfoType.ERROR);
								builder.range(textRange);
								builder.description(JetPsiChecker.getDefaultMessage(diagnostic));
								builder.escapedToolTip(JetPsiChecker.getTooltipMessage(diagnostic));
								builder.textAttributes(CodeInsightColors.WRONG_REFERENCES_ATTRIBUTES);

								final HighlightInfo e = builder.create();
								if(e != null)
								{
									NapileQuickFixProviderEP.callRegisterFor(diagnostic, e);
									infos.add(e);
								}
							}
						}

						continue;
					}

					if(REDECLARATION.contains(diagnostic.getFactory()))
					{
						//registerQuickFix(markRedeclaration(redeclarations, diagnostic, holder), diagnostic);
						continue;
					}

					// Generic annotation
					for(TextRange textRange : textRanges)
					{
						final HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(HighlightInfoType.ERROR);
						builder.range(textRange);
						builder.description(JetPsiChecker.getDefaultMessage(diagnostic));
						builder.escapedToolTip(JetPsiChecker.getTooltipMessage(diagnostic));
						if(diagnostic.getFactory() == Errors.INVALID_STRING_ESCAPE)
						{
							builder.textAttributes(NapileHighlightingColors.INVALID_STRING_ESCAPE);
						}

						final HighlightInfo e = builder.create();
						if(e != null)
						{
							NapileQuickFixProviderEP.callRegisterFor(diagnostic, e);
							infos.add(e);
						}
					}
					break;
				case WARNING:
					for(TextRange textRange : textRanges)
					{
						final HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(WARNINGS_LIKE_UNUSED.contains(diagnostic.getFactory()) ? HighlightInfoType.UNUSED_SYMBOL : HighlightInfoType.WARNING);
						builder.range(textRange);
						builder.description(JetPsiChecker.getDefaultMessage(diagnostic));
						builder.escapedToolTip(JetPsiChecker.getTooltipMessage(diagnostic));

						final HighlightInfo e = builder.create();
						if(e != null)
						{
							NapileQuickFixProviderEP.callRegisterFor(diagnostic, e);
							infos.add(e);
						}
					}
					break;
			}
		}

		for(PostHighlightVisitor visitor : new PostHighlightVisitor[]{
				new LabelsHighlightingVisitor(bindingContext, infos),
				new MethodssHighlightingVisitor(bindingContext, infos),
				new VariablesHighlightingVisitor(bindingContext, infos),
				new TypeKindHighlightingVisitor(bindingContext, infos),
				new SoftKeywordPostHighlightVisitor(bindingContext, infos),
				new InjectionHighlightingVisitor(bindingContext, infos)
		})
		{
			file.accept(visitor);
		}
	}

	@Override
	public void doApplyInformationToEditor()
	{
		if(infos == null)
			return;

		UpdateHighlightersUtil.setHighlightersToEditor(myProject, myDocument, 0, file.getTextLength(), infos, getColorsScheme(), getId());
	}
}
