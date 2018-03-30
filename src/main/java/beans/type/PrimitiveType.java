package beans.type;

import beans.AllType;

/**
 * Created by antonskripacev on 30.03.17.
 */
public class PrimitiveType extends TypeVar {
    public AllType type;

    public PrimitiveType(){};
    public PrimitiveType(AllType type){ this.type = type;};

    @Override
    public TypeVar clone() {
        PrimitiveType primitiveType = new PrimitiveType();
        primitiveType.type = this.type;

        return primitiveType;
    }
}
