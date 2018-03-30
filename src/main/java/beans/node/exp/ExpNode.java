package beans.node.exp;

import beans.node.StatementNode;
import beans.type.TypeVar;

/**
 * Created by antonskripacev on 26.03.17.
 */
public class ExpNode extends StatementNode {
    private TypeVar typeExp;
    private String operator;
    private ExpNode firstOperand;
    private ExpNode secondOperand;
    public int label;
    public int register;

    public ExpNode(int type) {
        super(type);
    }

    public ExpNode getFirstOperand() {
        return firstOperand;
    }

    public void setFirstOperand(ExpNode firstOperand) {
        this.firstOperand = firstOperand;
    }

    public ExpNode getSecondOperand() {
        return secondOperand;
    }

    public void setSecondOperand(ExpNode secondOperand) {
        this.secondOperand = secondOperand;
    }

    public void setTypeExp(TypeVar typeExp) {
        this.typeExp = typeExp;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public TypeVar getTypeExp() {
        return typeExp;
    }
}
