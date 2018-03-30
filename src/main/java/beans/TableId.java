package beans;

import beans.id.IdRow;
import beans.id.VarId;
import beans.node.AbstractNode;
import beans.type.TypeVar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by antonskripacev on 23.03.17.
 */
public class TableId implements Cloneable{
    public TableId parent;
    public List<IdRow> ids = new ArrayList<IdRow>();
    public boolean isHidden = false;

    public TableId(TableId parent) {
        this.parent = parent;
    }

    public TableId getParent() {
        return parent;
    }

    public List<IdRow> getIds() {
        return ids;
    }

    public void setParent(TableId parent) {
        this.parent = parent;
    }

    /*
             type:

             */
    public IdRow lookupUserIdTable(String varName, Class type) {
        if(varName.equals("") || this.isHidden) {
            return null;
        }

        for(IdRow id : this.ids) {
            if(type.equals(id.getClass()) && varName.equals(id.name)) {
                return id;
            }
        }

        return null;
    }

    public static IdRow lookupUserIdTableRecursive(TableId table, String varName, Class type) {
        if(table == null || varName.equals("") || table.isHidden) {
            return null;
        }

        for(IdRow id : table.ids) {
            if(type.equals(id.getClass()) && varName.equals(id.name)) {
                return id;
            }
        }

        return lookupUserIdTableRecursive(table.parent, varName, type);
    }
}
