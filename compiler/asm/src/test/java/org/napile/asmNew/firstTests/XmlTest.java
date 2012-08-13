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

package org.napile.asmNew.firstTests;

import java.io.StringWriter;

import org.napile.asm.tree.members.ClassNode;
import org.napile.asmNew.writters.XmlVisitor;

/**
 * @author VISTALL
 * @date 22:46/13.08.12
 */
public class XmlTest
{
	public static void main(String... arg)
	{
		ClassNode classNode = NodeUtil.createTestNode();

		XmlVisitor xmlVisitor = new XmlVisitor();
		classNode.accept(xmlVisitor, null);

		StringWriter writer = new StringWriter();
		xmlVisitor.write(writer);

		System.out.println(writer);
	}
}
