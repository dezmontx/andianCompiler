package beans.node.exp;

import beans.id.StructId;
import beans.type.TypeVar;

import java.util.Map;

/**
 * Created by antonskripacev on 03.04.17.
 */
public class FieldNode extends ExpNode {
    public String propName;
    public StructId parent;

    public FieldNode(int type) {
        super(type);
    }

    public int getOrderNumber() {
        int index = 0;
        for (Map.Entry<TypeVar, String> entry : parent.properties.entrySet()) {
            if (entry.getValue().equals(propName)) {
                return index;
            }

            index++;
        }

        return -1;
    }

    public TypeVar getType() {
        for (Map.Entry<TypeVar, String> entry : parent.properties.entrySet()) {
            if (entry.getValue().equals(propName)) {
                return entry.getKey();
            }
        }

        return null;
    }
}