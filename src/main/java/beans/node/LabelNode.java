package beans.node;

/**
 * Created by antonskripacev on 26.03.17.
 */
public class LabelNode extends StatementNode {
    public String name;
    public String lId;

    public String getName() {
        return name;
    }

    public LabelNode(String name, int type) {
        super(type);
        this.name = name;
    }
}
