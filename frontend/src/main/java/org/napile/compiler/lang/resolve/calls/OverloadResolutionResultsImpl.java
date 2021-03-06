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
import java.util.Collections;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.resolve.TemporaryBindingTrace;

/**
 * @author abreslav
 */
/*package*/ class OverloadResolutionResultsImpl<D extends CallableDescriptor> implements OverloadResolutionResults<D>
{

	public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> success(@NotNull ResolvedCallWithTrace<D> descriptor)
	{
		return new OverloadResolutionResultsImpl<D>(Code.SUCCESS, Collections.singleton(descriptor));
	}

	public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> nameNotFound()
	{
		return new OverloadResolutionResultsImpl<D>(Code.NAME_NOT_FOUND, Collections.<ResolvedCallWithTrace<D>>emptyList());
	}

	public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> singleFailedCandidate(ResolvedCallWithTrace<D> candidate)
	{
		return new OverloadResolutionResultsImpl<D>(Code.SINGLE_CANDIDATE_ARGUMENT_MISMATCH, Collections.singleton(candidate));
	}

	public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> manyFailedCandidates(Collection<ResolvedCallWithTrace<D>> failedCandidates)
	{
		return new OverloadResolutionResultsImpl<D>(Code.MANY_FAILED_CANDIDATES, failedCandidates);
	}

	public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> ambiguity(Collection<ResolvedCallWithTrace<D>> descriptors)
	{
		return new OverloadResolutionResultsImpl<D>(Code.AMBIGUITY, descriptors);
	}

	public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> incompleteTypeInference(Collection<ResolvedCallWithTrace<D>> descriptors)
	{
		return new OverloadResolutionResultsImpl<D>(Code.INCOMPLETE_TYPE_INFERENCE, descriptors);
	}

	public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> incompleteTypeInference(ResolvedCallWithTrace<D> descriptor)
	{
		return new OverloadResolutionResultsImpl<D>(Code.INCOMPLETE_TYPE_INFERENCE, Collections.singleton(descriptor));
	}

	private final Collection<ResolvedCallWithTrace<D>> results;
	private final Code resultCode;
	private TemporaryBindingTrace trace;

	private OverloadResolutionResultsImpl(@NotNull Code resultCode, @NotNull Collection<ResolvedCallWithTrace<D>> results)
	{
		this.results = results;
		this.resultCode = resultCode;
	}

	@Override
	@NotNull
	public Collection<ResolvedCallWithTrace<D>> getResultingCalls()
	{
		return results;
	}

	@Override
	@NotNull
	public ResolvedCallWithTrace<D> getResultingCall()
	{
		assert isSingleResult();
		return results.iterator().next();
	}

	@NotNull
	@Override
	public D getResultingDescriptor()
	{
		return getResultingCall().getResultingDescriptor();
	}

	@Override
	@NotNull
	public Code getResultCode()
	{
		return resultCode;
	}

	@Override
	public boolean isSuccess()
	{
		return resultCode.isSuccess();
	}

	@Override
	public boolean isSingleResult()
	{
		return isSuccess() || resultCode == Code.SINGLE_CANDIDATE_ARGUMENT_MISMATCH;
	}

	@Override
	public boolean isNothing()
	{
		return resultCode == Code.NAME_NOT_FOUND;
	}

	@Override
	public boolean isAmbiguity()
	{
		return resultCode == Code.AMBIGUITY;
	}
	//
	//    public OverloadResolutionResultsImpl<D> newContents(@NotNull Collection<D> functionDescriptors) {
	//        return new OverloadResolutionResultsImpl<D>(resultCode, functionDescriptors);
	//    }

	public TemporaryBindingTrace getTrace()
	{
		return trace;
	}

	public OverloadResolutionResultsImpl<D> setTrace(TemporaryBindingTrace trace)
	{
		this.trace = trace;
		return this;
	}
}
