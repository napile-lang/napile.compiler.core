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

package org.napile.compiler.lang.resolve.calls;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.descriptors.CallParameterDescriptor;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.resolve.calls.inference.ConstraintSystem;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.NapileType;
import com.intellij.openapi.util.Pair;

/**
 * @author abreslav
 */
public interface ResolvedCall<D extends CallableDescriptor>
{
	/**
	 * A target callable descriptor as it was accessible in the corresponding scope, i.e. with type arguments not substituted
	 */
	@NotNull
	D getCandidateDescriptor();

	/**
	 * Return a variable if this is anonym method call
	 */
	@Nullable
	Pair<VariableDescriptor, ReceiverDescriptor> getVariableCallInfo();

	/**
	 * Type arguments are substituted. This descriptor is guaranteed to have NO declared type parameters
	 */
	@NotNull
	D getResultingDescriptor();

	/**
	 * If the target was a member of a class, this is the object of that class to call it on
	 */
	@NotNull
	ReceiverDescriptor getThisObject();

	/**
	 * Determines whether receiver argument or this object is substituted for explicit receiver
	 */
	@NotNull
	ExplicitReceiverKind getExplicitReceiverKind();

	/**
	 * Values (arguments) for value parameters
	 */
	@NotNull
	Map<CallParameterDescriptor, ResolvedValueArgument> getValueArguments();

	/**
	 * Values (arguments) for value parameters indexed by parameter index
	 */
	@NotNull
	List<ResolvedValueArgument> getValueArgumentsByIndex();

	/**
	 * What's substituted for type parameters
	 */
	@NotNull
	Map<TypeParameterDescriptor, NapileType> getTypeArguments();

	@Nullable
	ConstraintSystem getConstraintSystem();

	boolean isSafeCall();
}
