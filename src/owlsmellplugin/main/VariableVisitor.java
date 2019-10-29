package owlsmellplugin.main;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

public class VariableVisitor extends ASTVisitor {
	List<FieldDeclaration> variables = new ArrayList<>();

    @Override
    public boolean visit(FieldDeclaration node) {
        variables.add(node);
        return super.visit(node);
    }

    public List<FieldDeclaration> getVariables() {
        return variables;
    }
}
