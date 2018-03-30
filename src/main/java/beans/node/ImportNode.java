package beans.node;

/**
 * Created by antonskripacev on 25.03.17.
 */
public class ImportNode extends AbstractNode {
    private String fName;

    public ImportNode(String fName, int type) {
        super(type);
        this.fName = fName;
    }

    public String getFileName() {
        return fName;
    }
}
