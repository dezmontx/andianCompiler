package beans.tac.tacExt;

import beans.tac.Operand;
import beans.tac.Operation;
import beans.tac.TAC;
import beans.type.PrimitiveType;
import beans.type.TypeVar;

/**
 * Created by antonskripacev on 07.04.17.
 */
public class BinOperTAC extends TAC {
    public Operand fOp = new Operand();
    public Operand sOp = new Operand();
    public Operation operation;
    public Operand result = new Operand();
    public int offset;
    public TypeVar type;

    public BinOperTAC(){};

    public BinOperTAC(Operand result,Operand fOp){
        this.result = result;
        this.fOp = fOp;
    }

    public BinOperTAC(Operand result,Operand fOp, Operation oper){
        this.result = result;
        this.fOp = fOp;
        this.operation = oper;
    }

    public BinOperTAC(Operand result,Operand fOp,Operand sOp,Operation operation){
        this.result = result;
        this.fOp = fOp;
        this.sOp = sOp;
        this.operation = operation;
    }


}
