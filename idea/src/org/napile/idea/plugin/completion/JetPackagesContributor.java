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

package org.napile.idea.plugin.completion;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.jvm.compiler.TipsManager;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.psi.NapileNamespaceHeader;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.idea.plugin.project.WholeProjectAnalyzerFacade;
import org.napile.idea.plugin.references.JetSimpleNameReference;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.PlainPrefixMatcher;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;

/**
 * Performs completion in package directive. Should suggest only packages and avoid showing fake package produced by
 * DUMMY_IDENTIFIER.
 *
 * @author Nikolay Krasko
 */
public class JetPackagesContributor extends CompletionContributor
{

	private static final String DUMMY_IDENTIFIER = "___package___";

	public JetPackagesContributor()
	{
		extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<CompletionParameters>()
		{
			@Override
			protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet result)
			{

				final PsiElement position = parameters.getPosition();
				if(!(position.getContainingFile() instanceof NapileFile))
				{
					return;
				}

				NapileNamespaceHeader namespaceHeader = PsiTreeUtil.getParentOfType(position, NapileNamespaceHeader.class);
				if(namespaceHeader == null)
				{
					return;
				}

				final PsiReference ref = parameters.getPosition().getContainingFile().findReferenceAt(parameters.getOffset());

				if(ref instanceof JetSimpleNameReference)
				{
					JetSimpleNameReference simpleNameReference = (JetSimpleNameReference) ref;

					String name = simpleNameReference.getExpression().getText();
					if(name == null)
					{
						return;
					}

					int prefixLength = parameters.getOffset() - simpleNameReference.getExpression().getTextOffset();
					result = result.withPrefixMatcher(new PlainPrefixMatcher(name.substring(0, prefixLength)));

					BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile((NapileFile) namespaceHeader.getContainingFile()).getBindingContext();

					for(LookupElement variant : DescriptorLookupConverter.collectLookupElements(bindingContext, TipsManager.getReferenceVariants(namespaceHeader, bindingContext)))
					{
						if(!variant.getLookupString().contains(DUMMY_IDENTIFIER))
						{
							result.addElement(variant);
						}
					}

					result.stopHere();
				}
			}
		});
	}

	@Override
	public void beforeCompletion(@NotNull CompletionInitializationContext context)
	{
		// Will need to filter this dummy identifier to avoid showing it in completion
		context.setDummyIdentifier(DUMMY_IDENTIFIER);
	}
}