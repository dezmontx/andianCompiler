package beans.node;


import beans.id.StructId;

/**
 * Created by antonskripacev on 23.03.17.
 */
public class DeclareStructNode extends AbstractNode {
    public StructId id;

    public DeclareStructNode(StructId id, int type) {
        super(type);
        this.id = id;
    }
}
