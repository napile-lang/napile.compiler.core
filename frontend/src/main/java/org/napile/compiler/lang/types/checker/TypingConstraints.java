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

package org.napile.compiler.lang.types.checker;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.lang.types.TypeConstructor;

/**
 * Methods of this class return true to continue type checking and false to fail
 */
public interface TypingConstraints
{
	boolean assertEqualTypes(@NotNull NapileType a, @NotNull NapileType b, @NotNull TypeCheckingProcedure typeCheckingProcedure);

	boolean assertEqualTypeConstructors(@NotNull TypeConstructor a, @NotNull TypeConstructor b);

	boolean assertSubtype(@NotNull NapileType subtype, @NotNull NapileType supertype, @NotNull TypeCheckingProcedure typeCheckingProcedure);

	boolean noCorrespondingSupertype(@NotNull NapileType subtype, @NotNull NapileType supertype);
}
