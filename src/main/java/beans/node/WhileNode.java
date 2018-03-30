package beans.node;

import beans.node.exp.ExpNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by antonskripacev on 02.04.17.
 */
public class WhileNode extends StatementNode {
    public String startIndex;
    public Long uniqueId;
    public List<StatementNode> statements = new ArrayList<StatementNode>();
    public ExpNode condition;

    public WhileNode(int type) {
        super(type);
    }
}
