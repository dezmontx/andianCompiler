package beans.node;

import beans.node.exp.ExpNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by antonskripacev on 02.04.17.
 */
public class IfNode extends StatementNode {
    public ExpNode condition;
    public List<StatementNode> ifBranch = new ArrayList<StatementNode>();
    public List<StatementNode> elseBranch = new ArrayList<StatementNode>();


    public IfNode(int type) {
        super(type);
    }
}
