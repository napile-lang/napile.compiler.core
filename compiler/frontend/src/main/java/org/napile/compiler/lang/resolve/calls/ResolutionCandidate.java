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

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import com.google.common.collect.Lists;

/**
 * @author svtk
 */
public class ResolutionCandidate<D extends CallableDescriptor>
{
	private final D candidateDescriptor;
	private ReceiverDescriptor thisObject; // receiver object of a method
	private ExplicitReceiverKind explicitReceiverKind;
	private Boolean isSafeCall;

	private ResolutionCandidate(@NotNull D descriptor, @NotNull ReceiverDescriptor thisObject, @NotNull ExplicitReceiverKind explicitReceiverKind, @Nullable Boolean isSafeCall)
	{
		this.candidateDescriptor = descriptor;
		this.thisObject = thisObject;
		this.explicitReceiverKind = explicitReceiverKind;
		this.isSafeCall = isSafeCall;
	}

	/*package*/
	static <D extends CallableDescriptor> ResolutionCandidate<D> create(@NotNull D descriptor)
	{
		return new ResolutionCandidate<D>(descriptor, ReceiverDescriptor.NO_RECEIVER, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER, null);
	}

	public static <D extends CallableDescriptor> ResolutionCandidate<D> create(@NotNull D descriptor, boolean isSafeCall)
	{
		return create(descriptor, ReceiverDescriptor.NO_RECEIVER, ReceiverDescriptor.NO_RECEIVER, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER, isSafeCall);
	}

	public static <D extends CallableDescriptor> ResolutionCandidate<D> create(@NotNull D descriptor, @NotNull ReceiverDescriptor thisObject, @NotNull ReceiverDescriptor receiverArgument, @NotNull ExplicitReceiverKind explicitReceiverKind, boolean isSafeCall)
	{
		return new ResolutionCandidate<D>(descriptor, thisObject, explicitReceiverKind, isSafeCall);
	}

	public void setThisObject(@NotNull ReceiverDescriptor thisObject)
	{
		this.thisObject = thisObject;
	}

	public void setExplicitReceiverKind(@NotNull ExplicitReceiverKind explicitReceiverKind)
	{
		this.explicitReceiverKind = explicitReceiverKind;
	}

	@NotNull
	public D getDescriptor()
	{
		return candidateDescriptor;
	}

	@NotNull
	public ReceiverDescriptor getThisObject()
	{
		return thisObject;
	}

	@NotNull
	public ExplicitReceiverKind getExplicitReceiverKind()
	{
		return explicitReceiverKind;
	}

	@NotNull
	public static <D extends CallableDescriptor> List<ResolutionCandidate<D>> convertCollection(@NotNull Collection<? extends D> descriptors, boolean isSafeCall)
	{
		List<ResolutionCandidate<D>> result = Lists.newArrayList();
		for(D descriptor : descriptors)
		{
			result.add(create(descriptor, isSafeCall));
		}
		return result;
	}

	public void setSafeCall(boolean safeCall)
	{
		assert isSafeCall == null;
		isSafeCall = safeCall;
	}

	public boolean isSafeCall()
	{
		assert isSafeCall != null;
		return isSafeCall;
	}
}
