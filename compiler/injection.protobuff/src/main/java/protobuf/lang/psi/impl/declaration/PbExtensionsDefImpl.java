package protobuf.lang.psi.impl.declaration;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import protobuf.lang.psi.PbPsiElementVisitor;
import protobuf.lang.psi.api.declaration.PbExtensionsDef;
import protobuf.lang.psi.impl.PbPsiElementImpl;

/**
 * @author Nikolay Matveev
 */
public class PbExtensionsDefImpl extends PbPsiElementImpl implements PbExtensionsDef {
    public PbExtensionsDefImpl(ASTNode node){
        super(node);
    }

    @Override
    public void accept(@NotNull PbPsiElementVisitor visitor) {
        visitor.visitExtensionsDefinition(this);
    }
}
