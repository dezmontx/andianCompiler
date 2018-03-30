package beans.node;

import beans.node.exp.ExpNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;

/**
 * Created by antonskripacev on 31.03.17.
 */
public class SwitchNode extends StatementNode {
    public Long endIndex;
    public LinkedHashMap<String, Long> caseIndexes = new LinkedHashMap<String, Long>();
    public LinkedHashMap<String, ArrayList<StatementNode>> cases = new LinkedHashMap<String, ArrayList<StatementNode>>();
    public ExpNode exp;

    public SwitchNode(int type) {
        super(type);
    }
}
