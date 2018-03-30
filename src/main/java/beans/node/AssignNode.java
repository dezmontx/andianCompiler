package beans.node;

import beans.node.exp.ComplexOperand;
import beans.node.exp.ExpNode;

/**
 * Created by antonskripacev on 31.03.17.
 */
public class AssignNode extends StatementNode {
    public ComplexOperand lval;
    public ExpNode rval;

    public AssignNode(int typeNode) {
        super(typeNode);
    }
}
