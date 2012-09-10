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

package org.napile.compiler.lang.cfg;

import org.jetbrains.annotations.Nullable;

/**
 * @author svtk
 */
public enum VariableUseState
{
	LAST_READ(3),
	LAST_WRITTEN(2),
	ONLY_WRITTEN_NEVER_READ(1),
	UNUSED(0);

	private final int importance;

	VariableUseState(int importance)
	{
		this.importance = importance;
	}

	public VariableUseState merge(@Nullable VariableUseState variableUseState)
	{
		if(variableUseState == null || importance > variableUseState.importance)
			return this;
		return variableUseState;
	}

	public static boolean isUsed(@Nullable VariableUseState variableUseState)
	{
		return variableUseState != null && variableUseState != UNUSED;
	}
}
