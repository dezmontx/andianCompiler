package beans.node;

import java.util.HashMap;
import java.util.List;

/**
 * Created by antonskripacev on 23.03.17.
 */

public class AbstractNode {
    public static final int STRUCT = 1;//complete
    public static final int FUNC = 2;//complete
    public static final int IMPORT = 3;//complete
    public static final int LABEL = 4;//complete
    public static final int GOTO = 5;//complete
    public static final int EXPRESSION = 6;//complete
    public static final int CONST_NODE = 7;//complete
    public static final int ID_NODE = 8;//complete
    public static final int COMPLEXE_OPERAND_NODE = 9;//complete
    public static final int NEW_NODE = 10; //complete
    public static final int DECLARE_VAR_NODE = 12;//complete
    public static final int FUNC_CALL_NODE = 13;//complete
    public static final int ASSIGN_NODE = 14;//complete
    public static final int SWITCH_NODE = 15;//complete
    public static final int FUNC_CALL_STM_NODE = 16;
    public static final int IF_NODE = 17;//complete
    public static final int WHILE_NODE = 18;//complete
    public static final int DO_WHILE_NODE = 19;//complete
    public static final int FOR_NODE = 20;//complete
    public static final int BREAK_NODE = 21;//complete
    public static final int CONTINUE_NODE = 22;//complete
    public static final int RETURN_NODE = 23;//complete
    public static final int FIELD_NODE = 24;//complete

    public int typeNode;

    public AbstractNode(int typeNode) {
        this.typeNode = typeNode;
    }
}









