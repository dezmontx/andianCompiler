package beans.node;

import beans.id.FuncId;
import beans.node.exp.ExpNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by antonskripacev on 02.04.17.
 */
public class FuncCallStmNode extends StatementNode {
    public List<ExpNode> args = new ArrayList<ExpNode>();
    public FuncId func;

    public FuncCallStmNode(int type) {
        super(type);
    }
}
