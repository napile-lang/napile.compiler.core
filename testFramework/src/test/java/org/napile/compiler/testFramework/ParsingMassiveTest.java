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

package org.napile.compiler.testFramework;

import org.napile.compiler.lang.psi.NapileFile;
import junit.framework.Test;

/**
 * @author VISTALL
 * @since 15:39/14.04.13
 */
public class ParsingMassiveTest extends AbstractMassiveTestSuite
{
	public static Test suite()
	{
		return new ParsingMassiveTest();
	}

	@Override
	public String getExpectedText(NapileFile file) throws Exception
	{
		return new PsiTreeDebugBuilder().setShowErrorElements(true).setShowWhiteSpaces(true).psiToString(file);
	}

	@Override
	public String getResultExt()
	{
		return "parsing";
	}
}