package beans.id;

import beans.type.TypeVar;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created by antonskripacev on 30.03.17.
 */
public class StructId extends IdRow {
    public LinkedHashMap<TypeVar, String> properties = new LinkedHashMap<TypeVar, String>();
}
