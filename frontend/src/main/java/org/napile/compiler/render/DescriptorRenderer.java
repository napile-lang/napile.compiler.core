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

package org.napile.compiler.render;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.resolve.name.FqNameUnsafe;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.*;
import org.napile.compiler.lang.diagnostics.rendering.Renderer;
import org.napile.compiler.lang.lexer.NapileKeywordToken;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.types.ErrorUtils;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.lang.types.MethodTypeConstructor;
import org.napile.compiler.lang.types.MultiTypeConstructor;
import org.napile.compiler.lang.types.MultiTypeEntry;
import org.napile.compiler.lang.types.SelfTypeConstructor;
import org.napile.compiler.lang.types.TypeUtils;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;

/**
 * @author abreslav
 */
public class DescriptorRenderer implements Renderer<DeclarationDescriptor>
{

	public static final DescriptorRenderer COMPACT_WITH_MODIFIERS = new DescriptorRenderer()
	{
		@Override
		protected boolean shouldRenderDefinedIn()
		{
			return false;
		}
	};

	public static final DescriptorRenderer COMPACT = new DescriptorRenderer()
	{
		@Override
		protected boolean shouldRenderDefinedIn()
		{
			return false;
		}

		@Override
		protected boolean shouldRenderModifiers()
		{
			return false;
		}
	};

	public static final DescriptorRenderer TEXT = new DescriptorRenderer();

	public static final DescriptorRenderer DEBUG_TEXT = new DescriptorRenderer()
	{
		@Override
		protected boolean hasDefaultValue(CallParameterDescriptor descriptor)
		{
			// hasDefaultValue() has effects
			return descriptor.declaresDefaultValue();
		}
	};

	public static final DescriptorRenderer HTML = new HtmlDescriptorRenderer();

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final RenderDeclarationDescriptorVisitor rootVisitor = new RenderDeclarationDescriptorVisitor();

	protected final DeclarationDescriptorVisitor<Void, StringBuilder> subVisitor = new RenderDeclarationDescriptorVisitor()
	{
		@Override
		public Void visitTypeParameterDescriptor(TypeParameterDescriptor descriptor, StringBuilder builder)
		{
			renderTypeParameter(descriptor, builder);
			return null;
		}

		@Override
		public Void visitCallParameterAsVariableDescriptor(CallParameterDescriptor descriptor, StringBuilder builder)
		{
			super.visitVariableDescriptor(descriptor, builder, false);
			if(hasDefaultValue(descriptor))
			{
				builder.append(" = ...");
			}
			return null;
		}
	};

	protected boolean hasDefaultValue(CallParameterDescriptor descriptor)
	{
		return descriptor.hasDefaultValue();
	}

	protected String renderKeyword(NapileKeywordToken keyword)
	{
		return keyword.getValue();
	}

	public String renderType(NapileType type)
	{
		return renderType(type, false);
	}

	public String renderTypeWithShortNames(NapileType type)
	{
		return renderType(type, true);
	}

	protected String renderType(NapileType type, boolean shortNamesOnly)
	{
		if(type == null)
			return escape("[NULL]");
		else if(ErrorUtils.isErrorType(type))
			return escape(type.toString());
		else if(type.getConstructor() instanceof SelfTypeConstructor)
			return renderKeyword(NapileTokens.THIS_KEYWORD);
		else if(type.getConstructor() instanceof MethodTypeConstructor)
			return renderMethodType(type, shortNamesOnly);
		else if(type.getConstructor() instanceof MultiTypeConstructor)
			return renderMultiType(type, shortNamesOnly);
		else
			return renderDefaultType(type, shortNamesOnly);
	}

	protected String renderDefaultType(NapileType type, boolean shortNamesOnly)
	{
		StringBuilder sb = new StringBuilder();
		ClassifierDescriptor cd = type.getConstructor().getDeclarationDescriptor();

		Object typeNameObject;

		if(cd == null || cd instanceof TypeParameterDescriptor)
		{
			typeNameObject = type.getConstructor();
		}
		else
		{
			if(shortNamesOnly)
			{
				// for nested classes qualified name should be used
				typeNameObject = cd.getName();
				DeclarationDescriptor parent = cd.getContainingDeclaration();
				while(parent instanceof ClassDescriptor)
				{
					typeNameObject = parent.getName() + "." + typeNameObject;
					parent = parent.getContainingDeclaration();
				}
			}
			else
			{
				typeNameObject = DescriptorUtils.getFQName(cd);
			}
		}

		sb.append(typeNameObject);
		if(!type.getArguments().isEmpty())
		{
			sb.append(escape("<"));
			appendTypes(sb, type.getArguments(), shortNamesOnly);
			sb.append(escape(">"));
		}
		if(type.isNullable())
		{
			sb.append("?");
		}
		return sb.toString();
	}

	protected void appendTypes(StringBuilder result, List<NapileType> types, boolean shortNamesOnly)
	{
		for(Iterator<NapileType> iterator = types.iterator(); iterator.hasNext(); )
		{
			result.append(renderType(iterator.next(), shortNamesOnly));
			if(iterator.hasNext())
			{
				result.append(", ");
			}
		}
	}

	private String renderMultiType(NapileType type, final boolean shortNamesOnly)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(StringUtil.join(((MultiTypeConstructor)type.getConstructor()).getEntries(), new Function<MultiTypeEntry, String>()
		{
			@Override
			public String fun(MultiTypeEntry multiTypeEntry)
			{
				StringBuilder b = new StringBuilder();
				if(multiTypeEntry.mutable != null)
					b.append(renderKeyword(multiTypeEntry.mutable ? NapileTokens.VAR_KEYWORD : NapileTokens.VAL_KEYWORD)).append(" ");
				if(multiTypeEntry.name != null)
					b.append(multiTypeEntry.name).append(" : ");
				b.append(renderType(multiTypeEntry.type, shortNamesOnly));
				return b.toString();
			}
		}, ", "));
		sb.append("]");
		return sb.toString();
	}

	protected String renderMethodType(NapileType type, final boolean shortNamesOnly)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append("(");
		sb.append(StringUtil.join(((MethodTypeConstructor) type.getConstructor()).getParameterTypes().entrySet(), new Function<Map.Entry<Name, NapileType>, String>()
		{
			@Override
			public String fun(Map.Entry<Name, NapileType> nameJetTypeEntry)
			{
				return nameJetTypeEntry.getKey() + " : " + renderType(nameJetTypeEntry.getValue(), shortNamesOnly);
			}
		}, ", "));
		sb.append(") -> ");

		sb.append(renderType(((MethodTypeConstructor) type.getConstructor()).getReturnType(), shortNamesOnly));
		sb.append("}");
		if(type.isNullable())
			sb.append("?");
		return sb.toString();
	}

	protected String escape(String s)
	{
		return s;
	}

	private String lt()
	{
		return escape("<");
	}

	@NotNull
	@Override
	public String render(@NotNull DeclarationDescriptor declarationDescriptor)
	{
		StringBuilder stringBuilder = new StringBuilder();
		declarationDescriptor.accept(rootVisitor, stringBuilder);

		if(shouldRenderDefinedIn())
		{
			appendDefinedIn(declarationDescriptor, stringBuilder);
		}
		return stringBuilder.toString();
	}

	public String renderFunctionParameters(@NotNull MethodDescriptor methodDescriptor)
	{
		StringBuilder stringBuilder = new StringBuilder();
		renderValueParameters(methodDescriptor, stringBuilder);
		return stringBuilder.toString();
	}

	protected boolean shouldRenderDefinedIn()
	{
		return true;
	}

	protected boolean shouldRenderModifiers()
	{
		return true;
	}

	private void appendDefinedIn(DeclarationDescriptor declarationDescriptor, StringBuilder stringBuilder)
	{
		if(declarationDescriptor instanceof ModuleDescriptor)
		{
			stringBuilder.append(" is a module");
			return;
		}
		stringBuilder.append(" ").append(renderMessage("defined in")).append(" ");

		final DeclarationDescriptor containingDeclaration = declarationDescriptor.getContainingDeclaration();
		if(containingDeclaration != null)
		{
			FqNameUnsafe fqName = DescriptorUtils.getFQName(containingDeclaration);
			stringBuilder.append(FqName.ROOT.equalsTo(fqName) ? "root package" : escape(fqName.getFqName()));
		}
	}

	public String renderMessage(String s)
	{
		return s;
	}

	protected void renderValueParameters(CallableMemberDescriptor descriptor, StringBuilder builder)
	{
		if(descriptor.getValueParameters().isEmpty())
		{
			renderEmptyValueParameters(builder);
		}
		for(Iterator<CallParameterDescriptor> iterator = descriptor.getValueParameters().iterator(); iterator.hasNext(); )
		{
			renderValueParameter(iterator.next(), !iterator.hasNext(), builder);
		}
	}

	protected void renderEmptyValueParameters(StringBuilder builder)
	{
		builder.append("()");
	}

	protected void renderValueParameter(CallParameterDescriptor parameterDescriptor, boolean isLast, StringBuilder builder)
	{
		if(parameterDescriptor.getIndex() == 0)
		{
			builder.append("(");
		}
		parameterDescriptor.accept(subVisitor, builder);
		if(!isLast)
		{
			builder.append(", ");
		}
		else
		{
			builder.append(")");
		}
	}

	public boolean renderTypeParameters(List<TypeParameterDescriptor> typeParameters, StringBuilder builder)
	{
		if(!typeParameters.isEmpty())
		{
			builder.append(lt());
			for(Iterator<TypeParameterDescriptor> iterator = typeParameters.iterator(); iterator.hasNext(); )
			{
				TypeParameterDescriptor typeParameterDescriptor = iterator.next();
				typeParameterDescriptor.accept(subVisitor, builder);
				if(iterator.hasNext())
				{
					builder.append(", ");
				}
			}
			builder.append(">");
			return true;
		}
		return false;
	}

	private class RenderDeclarationDescriptorVisitor implements DeclarationDescriptorVisitor<Void, StringBuilder>
	{
		@Override
		public Void visitCallParameterAsVariableDescriptor(CallParameterDescriptor descriptor, StringBuilder builder)
		{
			return visitVariableDescriptor(descriptor, builder);
		}

		@Override
		public Void visitCallParameterAsReferenceDescriptor(CallParameterAsReferenceDescriptorImpl descriptor, StringBuilder builder)
		{
			return visitVariableDescriptor(descriptor, builder);
		}

		@Override
		public Void visitVariableDescriptor(VariableDescriptor descriptor, StringBuilder builder)
		{
			return visitVariableDescriptor(descriptor, builder, false);
		}

		@Override
		public Void visitLocalVariableDescriptor(LocalVariableDescriptor descriptor, StringBuilder builder)
		{
			return visitVariableDescriptor(descriptor, builder);
		}

		protected Void visitVariableDescriptor(VariableDescriptor descriptor, StringBuilder builder, boolean skipValVar)
		{
			NapileType type = descriptor.getType();

			renderModality(descriptor.getModality(), builder);
			String typeString = renderPropertyPrefixAndComputeTypeString(descriptor, builder, skipValVar, Collections.<TypeParameterDescriptor>emptyList(), type);
			renderName(descriptor, builder);
			builder.append(" : ").append(escape(typeString));
			return null;
		}

		private String renderPropertyPrefixAndComputeTypeString(@NotNull VariableDescriptor descriptor, @NotNull StringBuilder builder, boolean skipVar,  @NotNull List<TypeParameterDescriptor> typeParameters, @Nullable NapileType outType)
		{
			String typeString = lt() + "no type>";
			if(!skipVar)
				builder.append(renderKeyword(descriptor.isMutable() ? NapileTokens.VAR_KEYWORD : NapileTokens.VAL_KEYWORD)).append(" ");

			if(outType != null)
				typeString = renderType(outType);

			renderTypeParameters(typeParameters, builder);

			return typeString;
		}

		@Override
		public Void visitPropertyDescriptor(VariableDescriptorImpl descriptor, StringBuilder builder)
		{
			renderVisibility(descriptor, builder);
			renderModality(descriptor.getModality(), builder);
			String typeString = renderPropertyPrefixAndComputeTypeString(descriptor, builder, false, descriptor.getTypeParameters(), descriptor.getType());
			renderName(descriptor, builder);
			builder.append(" : ").append(escape(typeString));
			return null;
		}

		private void renderVisibility(DeclarationDescriptorWithVisibility visibility, StringBuilder builder)
		{
			if(!shouldRenderModifiers())
				return;

			if(visibility.getVisibility() != Visibility.PUBLIC)
				builder.append(renderKeyword(visibility.getVisibility().getKeyword())).append(" ");
			if(visibility.isStatic())
				builder.append(renderKeyword(NapileTokens.STATIC_KEYWORD)).append(" ");
		}

		private void renderModality(Modality modality, StringBuilder builder)
		{
			if(!shouldRenderModifiers())
				return;
			NapileKeywordToken keyword = null;
			switch(modality)
			{
				case FINAL:
					keyword = NapileTokens.FINAL_KEYWORD;
					break;
				case ABSTRACT:
					keyword = NapileTokens.ABSTRACT_KEYWORD;
					break;
				default:
					break;
			}
			if(keyword != null)
				builder.append(renderKeyword(keyword)).append(" ");
		}

		@Override
		public Void visitFunctionDescriptor(MethodDescriptor descriptor, StringBuilder builder)
		{
			renderVisibility(descriptor, builder);
			renderModality(descriptor.getModality(), builder);
			builder.append(renderKeyword(NapileTokens.METH_KEYWORD)).append(" ");
			if(renderTypeParameters(descriptor.getTypeParameters(), builder))
				builder.append(" ");

			renderName(descriptor, builder);
			renderValueParameters(descriptor, builder);
			if(!TypeUtils.isEqualFqName(descriptor.getReturnType(), NapileLangPackage.NULL))
				builder.append(" : ").append(renderType(descriptor.getReturnType()));
			renderWhereSuffix(descriptor, builder);
			return null;
		}

		private void renderWhereSuffix(@NotNull CallableMemberDescriptor callable, @NotNull StringBuilder builder)
		{
			for(TypeParameterDescriptor typeParameter : callable.getTypeParameters())
			{
				if(typeParameter.getUpperBounds().size() > 1)
				{
					for(NapileType upperBound : typeParameter.getUpperBounds())
					{
						builder.append(", ");
						builder.append(typeParameter.getName());
						builder.append(" : ");
						builder.append(renderType(upperBound));
					}
				}
			}
		}

		@Override
		public Void visitConstructorDescriptor(ConstructorDescriptor constructorDescriptor, StringBuilder builder)
		{
			renderVisibility(constructorDescriptor, builder);

			builder.append(renderKeyword(NapileTokens.THIS_KEYWORD));

			ClassifierDescriptor classDescriptor = constructorDescriptor.getContainingDeclaration();
			renderTypeParameters(classDescriptor.getTypeConstructor().getParameters(), builder);
			renderValueParameters(constructorDescriptor, builder);
			return null;
		}

		@Override
		public Void visitTypeParameterDescriptor(TypeParameterDescriptor descriptor, StringBuilder builder)
		{
			builder.append(lt());
			renderTypeParameter(descriptor, builder);
			builder.append(">");
			return null;
		}

		@Override
		public Void visitNamespaceDescriptor(PackageDescriptor packageDescriptor, StringBuilder builder)
		{
			builder.append(renderKeyword(NapileTokens.PACKAGE_KEYWORD)).append(" ");
			renderName(packageDescriptor, builder);
			return null;
		}

		@Override
		public Void visitModuleDeclaration(ModuleDescriptor descriptor, StringBuilder builder)
		{
			renderName(descriptor, builder);
			return null;
		}


		@Override
		public Void visitClassDescriptor(ClassDescriptor descriptor, StringBuilder builder)
		{
			renderClassDescriptor(descriptor, builder, NapileTokens.CLASS_KEYWORD);
			return null;
		}


		public void renderClassDescriptor(ClassDescriptor descriptor, StringBuilder builder, NapileKeywordToken keyword)
		{
			renderVisibility(descriptor, builder);

			if(descriptor.getKind() != ClassKind.ANONYM_CLASS)
			{
				renderModality(descriptor.getModality(), builder);
			}
			builder.append(renderKeyword(keyword));
			if(descriptor.getKind() != ClassKind.ANONYM_CLASS)
			{
				builder.append(" ");
				renderName(descriptor, builder);
				renderTypeParameters(descriptor.getTypeConstructor().getParameters(), builder);
			}

			Collection<? extends NapileType> supertypes = descriptor.getTypeConstructor().getSupertypes();
			if(supertypes.isEmpty() || supertypes.size() == 1 && TypeUtils.isEqualFqName(supertypes.iterator().next(), NapileLangPackage.ANY))
			{
			}
			else
			{
				builder.append(" : ");
				for(Iterator<? extends NapileType> iterator = supertypes.iterator(); iterator.hasNext(); )
				{
					NapileType supertype = iterator.next();
					builder.append(renderType(supertype));
					if(iterator.hasNext())
					{
						builder.append(" & ");
					}
				}
			}
		}

		protected void renderName(DeclarationDescriptor descriptor, StringBuilder stringBuilder)
		{
			stringBuilder.append(escape(descriptor.getName().getName()));
		}

		protected void renderTypeParameter(TypeParameterDescriptor descriptor, StringBuilder builder)
		{
			renderName(descriptor, builder);

			if(!descriptor.getConstructors().isEmpty())
			{
				for(ConstructorDescriptor constructorDescriptor : descriptor.getConstructors())
				{
					renderValueParameters(constructorDescriptor, builder);
				}
			}

			if(!descriptor.getUpperBounds().isEmpty())
			{
				builder.append(" : ");
				builder.append(StringUtil.join(descriptor.getUpperBounds(), new Function<NapileType, String>()
				{
					@Override
					public String fun(NapileType napileType)
					{
						return renderType(napileType);
					}
				}, " & "));
			}
		}
	}

	public static class HtmlDescriptorRenderer extends DescriptorRenderer
	{

		@Override
		protected String escape(String s)
		{
			return s.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
		}

		@Override
		public String renderKeyword(NapileKeywordToken keyword)
		{
			return "<b>" + keyword.getValue() + "</b>";
		}

		@Override
		public String renderMessage(String s)
		{
			return "<i>" + s + "</i>";
		}
	}
}
