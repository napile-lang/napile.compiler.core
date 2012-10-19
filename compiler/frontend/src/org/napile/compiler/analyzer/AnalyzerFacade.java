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

package org.napile.compiler.analyzer;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.di.InjectorForTopDownAnalyzerBasic;
import org.napile.compiler.lang.descriptors.ModuleDescriptor;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.BindingTraceContext;
import org.napile.compiler.lang.resolve.BodiesResolveContext;
import org.napile.compiler.lang.resolve.CachedBodiesResolveContext;
import org.napile.compiler.lang.resolve.ObservableBindingTrace;
import org.napile.compiler.lang.resolve.TopDownAnalysisParameters;
import org.napile.compiler.lang.psi.NapileFile;
import com.google.common.base.Predicate;
import com.intellij.openapi.project.Project;

/**
 * @author Pavel Talanov
 */
public class AnalyzerFacade
{
	private AnalyzerFacade()
	{
	}

	@NotNull
	public static AnalyzeExhaust analyzeBodiesInFiles(@NotNull Project project, @NotNull Predicate<NapileFile> filesForBodiesResolve, @NotNull BindingTrace headersTraceContext, @NotNull BodiesResolveContext bodiesResolveContext)
	{
		return AnalyzerFacadeForEverything.analyzeBodiesInFilesWithJavaIntegration(project, filesForBodiesResolve, headersTraceContext, bodiesResolveContext);
	}

	@NotNull
	public static AnalyzeExhaust analyzeFiles(@NotNull Project project, @NotNull Collection<NapileFile> files, @NotNull Predicate<NapileFile> filesToAnalyzeCompletely)
	{
		BindingTraceContext bindingTraceContext = new BindingTraceContext();

		final ModuleDescriptor owner = new ModuleDescriptor(Name.special("<module>"));

		TopDownAnalysisParameters topDownAnalysisParameters = new TopDownAnalysisParameters(filesToAnalyzeCompletely, false, false);

		InjectorForTopDownAnalyzerBasic injector = new InjectorForTopDownAnalyzerBasic(project, topDownAnalysisParameters, new ObservableBindingTrace(bindingTraceContext), owner);
		try
		{
			injector.getTopDownAnalyzer().analyzeFiles(files);

			return AnalyzeExhaust.success(bindingTraceContext.getBindingContext(), new CachedBodiesResolveContext(injector.getTopDownAnalysisContext()));
		}
		finally
		{
			injector.destroy();
		}
	}}
