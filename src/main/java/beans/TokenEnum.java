package beans;

/**
 * Created by antonskripacev on 23.03.17.
 */
public enum TokenEnum {


    ID(""),

    /**
     * OPERATIONS
     * */
    PLUS("+"),MINUS("-"),ASSIGN("="),MULTIPLY("*"),DIVIDE("/"),
    AND("&"),OR("|"),DOT("."), COMMA(","),
    GREATER(">"),LESS("<"),NOT("!"),

    /**
     * SIGNS
     * */
    OPENBRACE("{"),CLOSEBRACE("}"),SEMICOLON(";"), COLON(":"),OPENPAREN("("),
    CLOSEPAREN(")"),OPENSQUARE("["),CLOSESQUARE("]"), SHARP("#"),

    /**
     * CONSTS
     */
    INTCONST(""), FLOATCONST(""), BOOLEANCONST(""), STRINGCONST(""),

    /**
     * RESERVED WORDS
     */
    IF("if"),ELSE("else"),INT("int"),BOOLEAN("boolean"),
    STRING("String"),FLOAT("float"),VOID("void"),WHILE("while"),NEW("new"),
    BREAK("break"),CONTINUE("continue"),FOR("for"),SWITCH("switch"),
    CASE("case"),DO("do"),RETURN("return"),STRUCT("struct"),
    GOTO("goto"), IMPORT("import"), TRUE("true"), FALSE("false");

    private String value;

    public String getValue() {
        return value;
    }

    static public TokenEnum getEnumElementByValue(String valueL) {
        for(TokenEnum e : TokenEnum.values()) {
            if(e.getValue().equals(valueL)) {
                return e;
            }
        }

        return null;
    }

    static public boolean isOperator(TokenEnum e) {
        if(e == PLUS || e == MINUS || e == MULTIPLY || e == DIVIDE || e == AND || e == OR || e == DOT || e == OPENSQUARE) {
            return true;
        }

        return false;
    }

    TokenEnum(String value) {
        this.value = value;
    }
}
