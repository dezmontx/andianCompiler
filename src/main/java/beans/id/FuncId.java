package beans.id;

import beans.type.TypeVar;

import java.util.LinkedHashMap;

/**
 * Created by antonskripacev on 30.03.17.
 */
public class FuncId extends IdRow {
    public TypeVar returnType;
    public LinkedHashMap<String, IdRow> propIds = new LinkedHashMap<String, IdRow>();
    public LinkedHashMap<TypeVar, String> properties = new LinkedHashMap<TypeVar, String>();
    public int sumOffset;
}
