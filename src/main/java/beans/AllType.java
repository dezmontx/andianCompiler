package beans;

/**
 * Created by antonskripacev on 23.03.17.
 */
public enum AllType {
    INT("int"),
    FLOAT("float"),
    STRING("String"),
    BOOLEAN("boolean"),
    VOID("void");

    public String value;

    AllType(String value) {
        this.value = value;
    }

    public static AllType getTypeByString(String valueL) {
        for(AllType t : AllType.values()) {
            if(t.value.equals(valueL)) {
                return t;
            }
        }

        return null;
    }

    public static AllType getTypeByTokenEnum(TokenEnum valueL) {
        switch (valueL) {
            case INTCONST:
                return INT;
            case FLOATCONST:
                return FLOAT;
            case STRINGCONST:
                return STRING;
            case BOOLEANCONST:
                return BOOLEAN;
        }

        return null;
    }
}
