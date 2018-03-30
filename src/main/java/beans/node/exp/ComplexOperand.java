package beans.node.exp;

import beans.node.exp.ExpNode;
import beans.node.exp.IdNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by antonskripacev on 31.03.17.
 */
public class ComplexOperand extends ExpNode {
    public List<ExpNode> operands = new ArrayList<ExpNode>();

    public ComplexOperand(int type) {
        super(type);
    }
}
