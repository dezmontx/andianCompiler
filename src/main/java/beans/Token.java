package beans;

/**
 * Created by antonskripacev on 23.03.17.
 */
public class Token {
    private TokenEnum type;

    private String value;//for ID, CONST
    private int line;
    private int position;

    public TokenEnum getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public int getLine() {
        return line;
    }

    public int getPosition() {
        return position;
    }

    public void setType(TokenEnum type) {
        this.type = type;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Token(int line, int position) {
        this.line = line;
        this.position = position;
    }
}
