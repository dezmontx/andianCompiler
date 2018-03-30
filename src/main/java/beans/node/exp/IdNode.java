package beans.node.exp;

import beans.id.IdRow;
import beans.node.exp.ExpNode;

/**
 * Created by antonskripacev on 29.03.17.
 */
public class IdNode extends ExpNode {
    public IdRow id;
    public String name;//for struct

    public IdNode(int type, IdRow id) {
        super(type);
        this.id = id;
    }

    public IdNode(int type, String name) {
        super(type);
        this.name = name;
    }
}
