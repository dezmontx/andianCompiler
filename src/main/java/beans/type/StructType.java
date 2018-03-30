package beans.type;

import beans.id.StructId;

/**
 * Created by antonskripacev on 30.03.17.
 */
public class StructType extends TypeVar {
    public StructId structId;

    @Override
    public TypeVar clone() {
        StructType structType = new StructType();
        structType.structId = this.structId;

        return structType;
    }
}
