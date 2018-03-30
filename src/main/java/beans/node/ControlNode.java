package beans.node;

import beans.node.exp.ExpNode;

/**
 * Created by antonskripacev on 02.04.17.
 */

/*
*   RETURN, BREAK, CONTINUE
 */
public class ControlNode extends StatementNode {
    public ExpNode exp;//only for return

    public ControlNode(int type) {
        super(type);
    }
}
