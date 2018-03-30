package beans.node;

/**
 * Created by antonskripacev on 26.03.17.
 */
public class GotoNode extends StatementNode {
    //TODO LabelNode
    public String label;

    public GotoNode(int type, String label) {
        super(type);
        this.label = label;
    }
}
