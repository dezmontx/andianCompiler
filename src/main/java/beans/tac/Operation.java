package beans.tac;

/**
 * Created by antonskripacev on 07.04.17.
 */
public enum Operation {
    PLUS("+"),MINUS("-"),EQUAL("=="), NOTEQUAL("!="),MULTIPLY("*"),DIVIDE("/"),
    AND("&"),OR("|"),DOT("."), COMMA(","), ASSIGN("="), ARRAY("[]"),
    GREATER(">"),LESS("<") , GREATEREQ(">="), LESSEQ("<="), NEW("new"),
    CALL(""), SAVE("save"), LOAD("load"), IFTRUE("ifTrue"), IFFALSE("ifFalse"), RESULT("result");

    public String value;

    Operation(String value) {
        this.value = value;
    }

    public static Operation getOperationByValue(String value) {
        for(Operation op : Operation.values()) {
            if(op.value.equals(value)) {
                return op;
            }
        }

        return null;
    }
}
