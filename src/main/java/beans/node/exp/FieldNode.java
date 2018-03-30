package beans.node.exp;

import beans.id.StructId;

/**
 * Created by antonskripacev on 03.04.17.
 */
public class FieldNode extends ExpNode {
    public String propName;
    public StructId parent;

    public FieldNode(int type) {
        super(type);
    }
}
