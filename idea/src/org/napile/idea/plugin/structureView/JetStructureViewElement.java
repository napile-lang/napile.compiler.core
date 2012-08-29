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

package org.napile.idea.plugin.structureView;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.ValueParameterDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileClassInitializer;
import org.napile.compiler.lang.psi.NapileLikeClass;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.resolve.DescriptorRenderer;
import org.napile.idea.plugin.project.WholeProjectAnalyzerFacade;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.util.Function;
import com.intellij.util.PsiIconUtil;

/**
 * @author yole
 */
public class JetStructureViewElement implements StructureViewTreeElement
{
	private final NavigatablePsiElement myElement;

	// For file context will be updated after each construction
	// For other tree sub-elements it's immutable.
	private BindingContext context;

	private String elementText;

	public JetStructureViewElement(NavigatablePsiElement element, BindingContext context)
	{
		myElement = element;
		this.context = context;
	}

	public JetStructureViewElement(NapileFile fileElement)
	{
		myElement = fileElement;
	}

	@Override
	public Object getValue()
	{
		return myElement;
	}

	@Override
	public void navigate(boolean requestFocus)
	{
		myElement.navigate(requestFocus);
	}

	@Override
	public boolean canNavigate()
	{
		return myElement.canNavigate();
	}

	@Override
	public boolean canNavigateToSource()
	{
		return myElement.canNavigateToSource();
	}

	@Override
	public ItemPresentation getPresentation()
	{
		return new ItemPresentation()
		{
			@Override
			public String getPresentableText()
			{
				if(elementText == null)
				{
					elementText = getElementText();
				}

				return elementText;
			}

			@Override
			public String getLocationString()
			{
				return null;
			}

			@Override
			public Icon getIcon(boolean open)
			{
				if(myElement.isValid())
				{
					return PsiIconUtil.getProvidersIcon(myElement, open ? Iconable.ICON_FLAG_OPEN : Iconable.ICON_FLAG_CLOSED);
				}

				return null;
			}
		};
	}

	@Override
	public TreeElement[] getChildren()
	{
		if(myElement instanceof NapileFile)
		{
			final NapileFile jetFile = (NapileFile) myElement;

			context = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(jetFile).getBindingContext();

			return wrapDeclarations(jetFile.getDeclarations());
		}
		else if(myElement instanceof NapileClass)
		{
			NapileClass napileClass = (NapileClass) myElement;
			List<NapileDeclaration> declarations = new ArrayList<NapileDeclaration>();

			declarations.addAll(napileClass.getDeclarations());
			return wrapDeclarations(declarations);
		}
		else if(myElement instanceof NapileLikeClass)
		{
			return wrapDeclarations(((NapileLikeClass) myElement).getDeclarations());
		}

		return new TreeElement[0];
	}

	private String getElementText()
	{
		String text = "";

		// Try to find text in correspondent descriptor
		if(myElement instanceof NapileDeclaration)
		{
			NapileDeclaration declaration = (NapileDeclaration) myElement;

			final DeclarationDescriptor descriptor = context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration);
			if(descriptor != null)
			{
				text = getDescriptorTreeText(descriptor);
			}
		}

		if(StringUtil.isEmpty(text))
		{
			text = myElement.getName();
		}

		if(StringUtil.isEmpty(text))
		{
			if(myElement instanceof NapileClassInitializer)
			{
				return "<class initializer>";
			}

		}

		return text;
	}

	private TreeElement[] wrapDeclarations(List<? extends NapileDeclaration> declarations)
	{
		TreeElement[] result = new TreeElement[declarations.size()];
		for(int i = 0; i < declarations.size(); i++)
		{
			result[i] = new JetStructureViewElement(declarations.get(i), context);
		}
		return result;
	}

	public static String getDescriptorTreeText(@NotNull DeclarationDescriptor descriptor)
	{
		StringBuilder textBuilder;

		if(descriptor instanceof MethodDescriptor)
		{
			textBuilder = new StringBuilder();

			MethodDescriptor methodDescriptor = (MethodDescriptor) descriptor;
			ReceiverDescriptor receiver = methodDescriptor.getReceiverParameter();
			if(receiver.exists())
			{
				textBuilder.append(DescriptorRenderer.TEXT.renderType(receiver.getType())).append(".");
			}

			textBuilder.append(methodDescriptor.getName());

			String parametersString = StringUtil.join(methodDescriptor.getValueParameters(), new Function<ValueParameterDescriptor, String>()
			{
				@Override
				public String fun(ValueParameterDescriptor valueParameterDescriptor)
				{
					return valueParameterDescriptor.getName() + ":" +
							DescriptorRenderer.TEXT.renderType(valueParameterDescriptor.getType());
				}
			}, ",");

			textBuilder.append("(").append(parametersString).append(")");

			JetType returnType = methodDescriptor.getReturnType();
			textBuilder.append(":").append(DescriptorRenderer.TEXT.renderType(returnType));
		}
		else if(descriptor instanceof VariableDescriptor)
		{
			JetType outType = ((VariableDescriptor) descriptor).getType();

			textBuilder = new StringBuilder(descriptor.getName().getName());
			textBuilder.append(":").append(DescriptorRenderer.TEXT.renderType(outType));
		}
		else if(descriptor instanceof ClassDescriptor)
		{
			textBuilder = new StringBuilder(descriptor.getName().getName());
			textBuilder.append(" (").append(DescriptorUtils.getFQName(descriptor.getContainingDeclaration())).append(")");
		}
		else
		{
			return DescriptorRenderer.TEXT.render(descriptor);
		}

		return textBuilder.toString();
	}
}
