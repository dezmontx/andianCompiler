package beans.tac;

import beans.id.FuncId;
import beans.id.VarId;

/**
 * Created by antonskripacev on 07.04.17.
 */
public class Operand {
    public String tempVar;
    public String constant;
    public String label;
    public VarId refVar;
    public FuncId refFunc;

    public Operand(){
    }

    public Operand(VarId refVar) {
        this.refVar = refVar;
    }

    public Operand(String tempVar,String constant,String label){
        this.tempVar = tempVar;
        this.constant = constant;
        this.label = label;
    }
}
