package beans.node.exp;

import beans.node.exp.ExpNode;
import beans.type.TypeVar;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by antonskripacev on 31.03.17.
 */
public class NewNode extends ExpNode {
    public List<ExpNode> sizes = new ArrayList<ExpNode>();//for arrays, new int[10+15] - size = 10+15
    public int sizeOfStruct;

    public NewNode(int type) {
        super(type);
    }
}
