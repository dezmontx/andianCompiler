package beans.node.exp;

import beans.node.exp.ExpNode;
import beans.type.TypeVar;

/**
 * Created by antonskripacev on 26.03.17.
 */
public class ConstNode extends ExpNode {
    private String value;

    public ConstNode(int type, String value, TypeVar typeNode) {
        super(type);
        this.value = value;
        this.setTypeExp(typeNode);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public TypeVar getTypeNode() {
        return getTypeExp();
    }

    public void setTypeNode(TypeVar typeNode) {
        this.setTypeExp(typeNode);
    }
}
