package beans.node;

import beans.id.VarId;
import beans.node.AbstractNode;
import beans.node.StatementNode;
import beans.node.exp.ExpNode;
import beans.type.TypeVar;

/**
 * Created by antonskripacev on 31.03.17.
 */
public class DeclareVarNode extends StatementNode {
    public TypeVar type;
    public VarId var;
    public ExpNode expNode;

    public DeclareVarNode(int typeNode) {
        super(typeNode);
    }
}
