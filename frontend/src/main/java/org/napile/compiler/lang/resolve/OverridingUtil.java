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

package org.napile.compiler.lang.resolve;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.CallParameterDescriptor;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.CallableMemberDescriptor;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptorImpl;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.types.ErrorUtils;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.lang.types.TypeConstructor;
import org.napile.compiler.lang.types.TypeSubstitutor;
import org.napile.compiler.lang.types.checker.NapileTypeChecker;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.util.Function;

/**
 * @author abreslav
 */
public class OverridingUtil
{

	private OverridingUtil()
	{
	}

	public static <D extends CallableDescriptor> Set<D> filterOverrides(Set<D> candidateSet)
	{
		return filterOverrides(candidateSet, Function.ID);
	}

	public static <D> Set<D> filterOverrides(Set<D> candidateSet, Function<? super D, ? extends CallableDescriptor> transform)
	{
		Set<D> candidates = Sets.newLinkedHashSet();
		outerLoop:
		for(D meD : candidateSet)
		{
			CallableDescriptor me = transform.fun(meD);
			for(D otherD : candidateSet)
			{
				CallableDescriptor other = transform.fun(otherD);
				if(me == other)
					continue;
				if(overrides(other, me))
				{
					continue outerLoop;
				}
			}
			for(D otherD : candidates)
			{
				CallableDescriptor other = transform.fun(otherD);
				if(me.getOriginal() == other.getOriginal() && isOverridableBy(other, me).getResult() == OverrideCompatibilityInfo.Result.OVERRIDABLE && isOverridableBy(me, other).getResult() == OverrideCompatibilityInfo.Result.OVERRIDABLE)
				{
					continue outerLoop;
				}
			}
			//            System.out.println(me);
			candidates.add(meD);
		}
		//        Set<D> candidates = Sets.newLinkedHashSet(candidateSet);
		//        for (D descriptor : candidateSet) {
		//            Set<CallableDescriptor> overriddenDescriptors = Sets.newHashSet();
		//            getAllOverriddenDescriptors(descriptor.getOriginal(), overriddenDescriptors);
		//            candidates.removeAll(overriddenDescriptors);
		//        }
		return candidates;
	}

	public static <Descriptor extends CallableDescriptor> boolean overrides(@NotNull Descriptor f, @NotNull Descriptor g)
	{
		Set<CallableDescriptor> overriddenDescriptors = Sets.newHashSet();
		getAllOverriddenDescriptors(f.getOriginal(), overriddenDescriptors);
		CallableDescriptor originalG = g.getOriginal();
		for(CallableDescriptor overriddenFunction : overriddenDescriptors)
		{
			if(originalG.equals(overriddenFunction.getOriginal()))
				return true;
		}
		return false;
	}

	private static void getAllOverriddenDescriptors(@NotNull CallableDescriptor current, @NotNull Set<CallableDescriptor> overriddenDescriptors)
	{
		if(overriddenDescriptors.contains(current))
			return;
		for(CallableDescriptor descriptor : current.getOriginal().getOverriddenDescriptors())
		{
			getAllOverriddenDescriptors(descriptor, overriddenDescriptors);
			overriddenDescriptors.add(descriptor);
		}
	}

	@NotNull
	public static OverrideCompatibilityInfo isOverridableBy(@NotNull CallableDescriptor superDescriptor, @NotNull CallableDescriptor subDescriptor)
	{
		if(superDescriptor instanceof MethodDescriptor)
		{
			if(!(subDescriptor instanceof MethodDescriptor))
				return OverrideCompatibilityInfo.memberKindMismatch();
		}
		else if(superDescriptor instanceof VariableDescriptorImpl)
		{
			if(!(subDescriptor instanceof VariableDescriptorImpl))
				return OverrideCompatibilityInfo.memberKindMismatch();
		}
		else
		{
			throw new IllegalArgumentException("This type of CallableDescriptor cannot be checked for overridability: " + superDescriptor);
		}

		// TODO: check outside of this method
		if(!superDescriptor.getName().equals(subDescriptor.getName()))
		{
			return OverrideCompatibilityInfo.nameMismatch();
		}

		return isOverridableByImpl(superDescriptor, subDescriptor, true);
	}

	private static List<NapileType> compiledValueParameters(CallableDescriptor callableDescriptor)
	{
		ArrayList<NapileType> parameters = new ArrayList<NapileType>(callableDescriptor.getValueParameters().size());
		for(CallParameterDescriptor parameterDescriptor : callableDescriptor.getValueParameters())
		{
			parameters.add(parameterDescriptor.getType());
		}
		return parameters;
	}

	private static int compiledValueParameterCount(CallableDescriptor callableDescriptor)
	{
		return callableDescriptor.getValueParameters().size();
	}

	/**
	 * @param forOverride true for override, false for overload
	 */
	static OverrideCompatibilityInfo isOverridableByImpl(@NotNull CallableDescriptor superDescriptor, @NotNull CallableDescriptor subDescriptor, boolean forOverride)
	{

		// TODO : Visibility

		if(compiledValueParameterCount(superDescriptor) != compiledValueParameterCount(subDescriptor))
		{
			return OverrideCompatibilityInfo.valueParameterNumberMismatch();
		}

		List<NapileType> superValueParameters = compiledValueParameters(superDescriptor);
		List<NapileType> subValueParameters = compiledValueParameters(subDescriptor);

		if(forOverride)
		{
			if(superDescriptor.getTypeParameters().size() != subDescriptor.getTypeParameters().size())
			{
				for(int i = 0; i < superValueParameters.size(); ++i)
				{
					NapileType superValueParameterType = getUpperBound(superValueParameters.get(i));
					NapileType subValueParameterType = getUpperBound(subValueParameters.get(i));
					// TODO: compare erasure
					if(!NapileTypeChecker.INSTANCE.equalTypes(superValueParameterType, subValueParameterType))
					{
						return OverrideCompatibilityInfo.typeParameterNumberMismatch();
					}
				}
				return OverrideCompatibilityInfo.valueParameterTypeMismatch(null, null, OverrideCompatibilityInfo.Result.CONFLICT);
			}
		}

		if(forOverride)
		{

			List<TypeParameterDescriptor> superTypeParameters = superDescriptor.getTypeParameters();
			List<TypeParameterDescriptor> subTypeParameters = subDescriptor.getTypeParameters();

			BiMap<TypeConstructor, TypeConstructor> axioms = HashBiMap.create();
			for(int i = 0, typeParametersSize = superTypeParameters.size(); i < typeParametersSize; i++)
			{
				TypeParameterDescriptor superTypeParameter = superTypeParameters.get(i);
				TypeParameterDescriptor subTypeParameter = subTypeParameters.get(i);
				axioms.put(superTypeParameter.getTypeConstructor(), subTypeParameter.getTypeConstructor());
			}

			for(int i = 0, typeParametersSize = superTypeParameters.size(); i < typeParametersSize; i++)
			{
				TypeParameterDescriptor superTypeParameter = superTypeParameters.get(i);
				TypeParameterDescriptor subTypeParameter = subTypeParameters.get(i);

				if(!NapileTypeChecker.INSTANCE.equalTypes(superTypeParameter.getUpperBoundsAsType(), subTypeParameter.getUpperBoundsAsType(), axioms))
				{
					return OverrideCompatibilityInfo.boundsMismatch(superTypeParameter, subTypeParameter);
				}
			}

			for(int i = 0, unsubstitutedValueParametersSize = superValueParameters.size(); i < unsubstitutedValueParametersSize; i++)
			{
				NapileType superValueParameter = superValueParameters.get(i);
				NapileType subValueParameter = subValueParameters.get(i);

				boolean bothErrors = ErrorUtils.isErrorType(superValueParameter) && ErrorUtils.isErrorType(subValueParameter);
				if(!bothErrors && !NapileTypeChecker.INSTANCE.equalTypes(superValueParameter, subValueParameter, axioms))
				{
					return OverrideCompatibilityInfo.valueParameterTypeMismatch(superValueParameter, subValueParameter, OverrideCompatibilityInfo.Result.INCOMPATIBLE);
				}
			}
		}
		else
		{

			for(int i = 0; i < superValueParameters.size(); ++i)
			{
				NapileType superValueParameterType = getUpperBound(superValueParameters.get(i));
				NapileType subValueParameterType = getUpperBound(subValueParameters.get(i));
				// TODO: compare erasure
				if(!NapileTypeChecker.INSTANCE.equalTypes(superValueParameterType, subValueParameterType))
				{
					return OverrideCompatibilityInfo.valueParameterTypeMismatch(superValueParameterType, subValueParameterType, OverrideCompatibilityInfo.Result.INCOMPATIBLE);
				}
			}

			return OverrideCompatibilityInfo.success();
		}

		return OverrideCompatibilityInfo.success();
	}

	private static NapileType getUpperBound(NapileType type)
	{
		if(type.getConstructor().getDeclarationDescriptor() instanceof ClassDescriptor)
		{
			return type;
		}
		else if(type.getConstructor().getDeclarationDescriptor() instanceof TypeParameterDescriptor)
		{
			return ((TypeParameterDescriptor) type.getConstructor().getDeclarationDescriptor()).getUpperBoundsAsType();
		}
		else
		{
			System.out.println("unknown type constructor: " + type.getConstructor().getClass().getName());
			return type;
		}
	}

	public static boolean isReturnTypeOkForOverride(@NotNull NapileTypeChecker typeChecker, @NotNull CallableDescriptor superDescriptor, @NotNull CallableDescriptor subDescriptor)
	{
		List<TypeParameterDescriptor> superTypeParameters = superDescriptor.getTypeParameters();
		List<TypeParameterDescriptor> subTypeParameters = subDescriptor.getTypeParameters();
		Map<TypeConstructor, NapileType> substitutionContext = Maps.newHashMap();
		for(int i = 0, typeParametersSize = superTypeParameters.size(); i < typeParametersSize; i++)
		{
			TypeParameterDescriptor superTypeParameter = superTypeParameters.get(i);
			TypeParameterDescriptor subTypeParameter = subTypeParameters.get(i);
			substitutionContext.put(superTypeParameter.getTypeConstructor(), subTypeParameter.getDefaultType());
		}

		// This code compares return types, but they are not a part of the signature, so this code does not belong here
		TypeSubstitutor typeSubstitutor = TypeSubstitutor.create(substitutionContext);
		NapileType substitutedSuperReturnType = typeSubstitutor.substitute(superDescriptor.getReturnType(), null);
		assert substitutedSuperReturnType != null;
		if(!typeChecker.isSubtypeOf(subDescriptor.getReturnType(), substitutedSuperReturnType))
		{
			return false;
		}

		return true;
	}

	/**
	 * Get overridden descriptors that are declarations or delegations.
	 *
	 * @see CallableMemberDescriptor.Kind#isReal()
	 */
	public static Collection<CallableMemberDescriptor> getOverriddenDeclarations(CallableMemberDescriptor descriptor)
	{
		Map<ClassDescriptor, CallableMemberDescriptor> result = Maps.newHashMap();
		getOverriddenDeclarations(descriptor, result);
		return result.values();
	}

	private static void getOverriddenDeclarations(CallableMemberDescriptor descriptor, Map<ClassDescriptor, CallableMemberDescriptor> r)
	{
		if(descriptor.getKind().isReal() || descriptor.getKind() == CallableMemberDescriptor.Kind.CREATED_BY_PLUGIN)
		{
			r.put((ClassDescriptor) descriptor.getContainingDeclaration(), descriptor);
		}
		else
		{
			if(descriptor.getOverriddenDescriptors().isEmpty())
			{
				throw new IllegalStateException();
			}
			for(CallableMemberDescriptor overridden : descriptor.getOverriddenDescriptors())
			{
				getOverriddenDeclarations(overridden, r);
			}
		}
	}

	public static void bindOverride(CallableMemberDescriptor fromCurrent, CallableMemberDescriptor fromSupertype)
	{
		fromCurrent.addOverriddenDescriptor(fromSupertype);

		for(CallParameterDescriptor parameterFromCurrent : fromCurrent.getValueParameters())
		{
			assert parameterFromCurrent.getIndex() < fromSupertype.getValueParameters().size() : "An override relation between functions implies that they have the same number of value parameters";
			CallParameterDescriptor parameterFromSupertype = fromSupertype.getValueParameters().get(parameterFromCurrent.getIndex());
			parameterFromCurrent.addOverriddenDescriptor(parameterFromSupertype);
		}
	}

	public static class OverrideCompatibilityInfo
	{

		public enum Result
		{
			OVERRIDABLE,
			INCOMPATIBLE,
			CONFLICT,
		}

		private static final OverrideCompatibilityInfo SUCCESS = new OverrideCompatibilityInfo(Result.OVERRIDABLE, "SUCCESS");

		@NotNull
		public static OverrideCompatibilityInfo success()
		{
			return SUCCESS;
		}

		@NotNull
		public static OverrideCompatibilityInfo nameMismatch()
		{
			return new OverrideCompatibilityInfo(Result.INCOMPATIBLE, "nameMismatch"); // TODO
		}

		@NotNull
		public static OverrideCompatibilityInfo typeParameterNumberMismatch()
		{
			return new OverrideCompatibilityInfo(Result.INCOMPATIBLE, "typeParameterNumberMismatch"); // TODO
		}

		@NotNull
		public static OverrideCompatibilityInfo valueParameterNumberMismatch()
		{
			return new OverrideCompatibilityInfo(Result.INCOMPATIBLE, "valueParameterNumberMismatch"); // TODO
		}

		@NotNull
		public static OverrideCompatibilityInfo boundsMismatch(TypeParameterDescriptor superTypeParameter, TypeParameterDescriptor subTypeParameter)
		{
			return new OverrideCompatibilityInfo(Result.INCOMPATIBLE, "boundsMismatch"); // TODO
		}

		@NotNull
		public static OverrideCompatibilityInfo valueParameterTypeMismatch(NapileType superValueParameter, NapileType subValueParameter, Result result)
		{
			return new OverrideCompatibilityInfo(result, "valueParameterTypeMismatch"); // TODO
		}

		@NotNull
		public static OverrideCompatibilityInfo memberKindMismatch()
		{
			return new OverrideCompatibilityInfo(Result.INCOMPATIBLE, "memberKindMismatch"); // TODO
		}

		@NotNull
		public static OverrideCompatibilityInfo returnTypeMismatch(NapileType substitutedSuperReturnType, NapileType unsubstitutedSubReturnType)
		{
			return new OverrideCompatibilityInfo(Result.CONFLICT, "returnTypeMismatch: " +
					unsubstitutedSubReturnType +
					" >< " +
					substitutedSuperReturnType); // TODO
		}

		@NotNull
		public static OverrideCompatibilityInfo varOverriddenByVal()
		{
			return new OverrideCompatibilityInfo(Result.INCOMPATIBLE, "varOverriddenByVal"); // TODO
		}

		////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		private final Result overridable;
		private final String message;

		public OverrideCompatibilityInfo(Result success, String message)
		{
			this.overridable = success;
			this.message = message;
		}

		public Result getResult()
		{
			return overridable;
		}

		public String getMessage()
		{
			return message;
		}
	}
}
