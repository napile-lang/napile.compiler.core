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

package org.napile.compiler.lang.descriptors;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.resolve.name.Name;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.TypeSubstitutor;
import com.google.common.collect.Sets;

/**
 * @author abreslav
 */
public abstract class PropertyAccessorDescriptor extends DeclarationDescriptorNonRootImpl implements MethodDescriptor, MemberDescriptor
{
	private final boolean hasBody;
	private final boolean isDefault;
	private final boolean isStatic;
	private final Modality modality;
	private final PropertyDescriptor correspondingProperty;
	private final Kind kind;
	private Visibility visibility;

	public PropertyAccessorDescriptor(@NotNull Modality modality, @NotNull Visibility visibility, @NotNull PropertyDescriptor correspondingProperty, @NotNull List<AnnotationDescriptor> annotations, @NotNull Name name, boolean hasBody, boolean isDefault, Kind kind, boolean isStatic)
	{
		super(correspondingProperty.getContainingDeclaration(), annotations, name);
		this.modality = modality;
		this.visibility = visibility;
		this.correspondingProperty = correspondingProperty;
		this.hasBody = hasBody;
		this.isDefault = isDefault;
		this.kind = kind;
		this.isStatic = isStatic;
	}

	public boolean hasBody()
	{
		return hasBody;
	}

	@Override
	public boolean isNative()
	{
		return false; //TODO [VISTALL] invalid
	}

	@Override
	public boolean isStatic()
	{
		return isStatic;
	}

	public boolean isDefault()
	{
		return isDefault;
	}

	@Override
	public Kind getKind()
	{
		return kind;
	}

	@NotNull
	@Override
	public abstract PropertyAccessorDescriptor getOriginal();

	@NotNull
	@Override
	public MethodDescriptor substitute(TypeSubstitutor substitutor)
	{
		throw new UnsupportedOperationException(); // TODO
	}

	@NotNull
	@Override
	public List<TypeParameterDescriptor> getTypeParameters()
	{
		return Collections.emptyList();
	}

	@NotNull
	@Override
	public Modality getModality()
	{
		return modality;
	}

	@NotNull
	@Override
	public Visibility getVisibility()
	{
		return visibility;
	}

	public void setVisibility(Visibility visibility)
	{
		this.visibility = visibility;
	}

	@NotNull
	public PropertyDescriptor getCorrespondingProperty()
	{
		return correspondingProperty;
	}

	@NotNull
	@Override
	public ReceiverDescriptor getReceiverParameter()
	{
		return getCorrespondingProperty().getReceiverParameter();
	}

	@NotNull
	@Override
	public ReceiverDescriptor getExpectedThisObject()
	{
		return getCorrespondingProperty().getExpectedThisObject();
	}

	@NotNull
	@Override
	public PropertyAccessorDescriptor copy(DeclarationDescriptor newOwner, Modality modality, boolean makeInvisible, Kind kind, boolean copyOverrides)
	{
		throw new UnsupportedOperationException("Accessors must be copied by the corresponding property");
	}

	protected Set<PropertyAccessorDescriptor> getOverriddenDescriptors(boolean isGetter)
	{
		Set<? extends PropertyDescriptor> overriddenProperties = getCorrespondingProperty().getOverriddenDescriptors();
		Set<PropertyAccessorDescriptor> overriddenAccessors = Sets.newHashSet();
		for(PropertyDescriptor overriddenProperty : overriddenProperties)
		{
			PropertyAccessorDescriptor accessorDescriptor = isGetter ? overriddenProperty.getGetter() : overriddenProperty.getSetter();
			if(accessorDescriptor != null)
			{
				overriddenAccessors.add(accessorDescriptor);
			}
		}
		return overriddenAccessors;
	}

	@Override
	public void addOverriddenDescriptor(@NotNull CallableMemberDescriptor overridden)
	{
		throw new IllegalStateException();
	}
}
