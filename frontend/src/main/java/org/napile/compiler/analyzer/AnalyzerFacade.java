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

import org.jetbrains.annotations.NotNull;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.di.InjectorForTopDownAnalyzerBasic;
import org.napile.compiler.lang.descriptors.ModuleDescriptor;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.resolve.BindingTraceImpl;
import org.napile.compiler.lang.resolve.CachedBodiesResolveContext;
import org.napile.compiler.lang.resolve.ObservableBindingTrace;
import org.napile.compiler.lang.resolve.TopDownAnalysisParameters;
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
	public static AnalyzeExhaust analyzeFiles(@NotNull Project project, @NotNull AnalyzeContext analyzeContext, @NotNull Predicate<NapileFile> predicate)
	{
		BindingTraceImpl bindingTraceImpl = new BindingTraceImpl();

		final ModuleDescriptor owner = new ModuleDescriptor(Name.special("<module>"));

		TopDownAnalysisParameters topDownAnalysisParameters = new TopDownAnalysisParameters(predicate, false);

		InjectorForTopDownAnalyzerBasic injector = new InjectorForTopDownAnalyzerBasic(project, topDownAnalysisParameters, new ObservableBindingTrace(bindingTraceImpl), owner);
		try
		{
			injector.getTopDownAnalyzer().analyzeFiles(project, analyzeContext);

			return AnalyzeExhaust.success(bindingTraceImpl, new CachedBodiesResolveContext(injector.getTopDownAnalysisContext()), injector);
		}
		finally
		{
			injector.destroy();
		}
	}}
