package beans.node;

/**
 * Created by antonskripacev on 02.04.17.
 */
public class ForNode extends WhileNode {
    public DeclareVarNode declaration;
    public AssignNode assign;

    public ForNode(int type) {
        super(type);
    }
}
