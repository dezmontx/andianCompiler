package parser;

import beans.AllType;
import beans.id.FuncId;
import beans.id.VarId;
import beans.node.*;
import beans.node.exp.*;
import beans.tac.Operand;
import beans.tac.Operation;
import beans.tac.TAC;
import beans.tac.tacExt.*;
import beans.type.PrimitiveType;
import beans.type.TypeVar;

import java.util.*;

/**
 * Created by antonskripacev on 05.04.17.
 */
public class TranslatorTAC {
    public static final List<TAC> tacs = new ArrayList<TAC>();
    public static int curNumLabel;
    public static String curCycle;
    public static final HashMap<Integer, Float> floats = new HashMap<Integer, Float>();
    public static int curPointerFloat = 0;
    //for each block remind offset
    public static Stack<Integer> curOffsetFromStartStack = new Stack<Integer>();

    public static void createFuncLabelTAC(String name) {
        FuncDeclTAC tacRow = new FuncDeclTAC();
        tacRow.name = "func_" + name;
        tacs.add(tacRow);
    }

    public static void createGoTOStatement(LabelNode label) {
        GoToTAC goToTAC = new GoToTAC();
        goToTAC.label = label.lId;
        tacs.add(goToTAC);
    }

    public static void createGoTOStatement(String label) {
        GoToTAC goToTAC = new GoToTAC();
        goToTAC.label = label;
        tacs.add(goToTAC);
    }

    public static void createLabelStatement(LabelNode labelNode) {
        LabelTAC labelTAC = new LabelTAC();
        labelTAC.label = "$L" + (curNumLabel++) + ":";
        labelNode.lId = labelTAC.label;
        tacs.add(labelTAC);
    }

    public static void createLabelStatement(String name) {
        LabelTAC labelTAC = new LabelTAC();
        labelTAC.label = name;
        tacs.add(labelTAC);
    }

    public static void createAssignStatement(VarId var, ExpNode exp) {
        BinOperTAC tacRow = new BinOperTAC();

        tacRow.result.refVar = var;
        tacRow.fOp.tempVar = createSequenceTacForExp(exp, 0);
        tacRow.operation = Operation.SAVE;

        tacs.add(tacRow);
    }

    public static void createAssignStatement(ComplexOperand op, ExpNode exp) {
        if(op.operands.size() == 1) {
            createAssignStatement((VarId) (((IdNode)op.operands.get(0)).id), exp);
            return;
        }

        BinOperTAC tacRow = new BinOperTAC();
        tacRow.result.tempVar = createComplexeOperandSequence(op, 0);
        tacRow.fOp.tempVar = createSequenceTacForExp(exp, 1);
        tacRow.operation = Operation.SAVE;

        tacs.add(tacRow);
    }

    public static String createComplexeOperandSequence(ComplexOperand op, int startNumVar) {
        BinOperTAC tac2 = new BinOperTAC(
                new Operand("$t" + startNumVar, null, null),
                new Operand((VarId)((IdNode)op.operands.get(0)).id),
                Operation.ASSIGN
        );

        tacs.add(tac2);

        List<ExpNode> indexesArray = new ArrayList<ExpNode>();

        for(int i = 1; i < op.operands.size(); i++) {
            ExpNode oper = op.operands.get(i);
            if(oper instanceof FieldNode) {
                if(indexesArray.size() != 0) {
                    generateSequenceForArrayIndexes(indexesArray, startNumVar);

                    indexesArray.clear();
                }

                FieldNode op2 = (FieldNode)oper;

                int index = 0;
                int offset = 0;
                for(Map.Entry<TypeVar, String> e : op2.parent.properties.entrySet()) {
                    if(e.getValue().equals(op2.propName)) {
                        break;
                    }

                    offset += 4;
                    index++;
                }

                BinOperTAC tac = new BinOperTAC(
                        new Operand("$t" + startNumVar, null, null),
                        new Operand(null, Integer.toString(index), null),
                        Operation.ARRAY
                );

                tac.offset = offset;

                tacs.add(tac);

                continue;
            }

            indexesArray.add(oper);
        }

        if(indexesArray.size() != 0) {
            generateSequenceForArrayIndexes(indexesArray, startNumVar);

            indexesArray.clear();
        }

        return "$t" + startNumVar;
    }

    public static void generateSequenceForArrayIndexes(List<ExpNode> indexes, int startNumVar) {
        int index = indexes.size();

        for(ExpNode i : indexes) {
            createSequenceTacForExp(i, startNumVar + 2);

            for(int j = 1; j < index; j++) {
                BinOperTAC tac1 = new BinOperTAC(
                        new Operand("$t" + (startNumVar + 3), null, null),
                        new Operand("$t" + (startNumVar), null, null),
                        new Operand(null, Integer.toString(-(j * 4)), null),
                        Operation.LOAD
                );
                tacs.add(tac1);

                BinOperTAC tac2 = new BinOperTAC(
                        new Operand("$t" + (startNumVar + 2), null, null),
                        new Operand("$t" + (startNumVar + 2), null, null),
                        new Operand("$t" + (startNumVar + 3), null, null),
                        Operation.MULTIPLY
                );

                PrimitiveType intType = new PrimitiveType();
                intType.type = AllType.INT;
                tac2.type = intType;

                tacs.add(tac2);
            }

            BinOperTAC tac = new BinOperTAC(
                    new Operand("$t" + (startNumVar + 1), null, null),
                    new Operand("$t" + (startNumVar + 1), null, null),
                    new Operand("$t" + (startNumVar + 2), null, null),
                    Operation.PLUS
            );

            PrimitiveType intType = new PrimitiveType();
            intType.type = AllType.INT;
            tac.type = intType;

            index--;

            tacs.add(tac);
        }

        BinOperTAC tac = new BinOperTAC(
                new Operand("$t" + startNumVar, null, null),
                new Operand("$t" + (startNumVar + 1), null, null),
                Operation.ARRAY
        );

        tacs.add(tac);
    }

    public static void createReturnStatement(ExpNode exp, FuncId func) {
        ReturnTAC tacRow = new ReturnTAC();
        tacRow.func = func;

        if (exp != null) {
            String var = createSequenceTacForExp(exp, 0);
            tacRow.tVar = var;
        } else {
            tacRow.tVar = null;
        }

        tacs.add(tacRow);
    }

    public static String createSequenceTacForExp(ExpNode exp, int startNumVar) {
        if(exp instanceof NewNode) {
            NewNode newNode = (NewNode)exp;

            if(((NewNode) exp).sizes.size() > 0) {
                BinOperTAC tacRow = new BinOperTAC();

                tacRow.result.tempVar = "$t" + Integer.toString(startNumVar + 1);
                tacRow.fOp.tempVar = createSequenceTacForExp(((NewNode) exp).sizes.get(0), startNumVar + 2);
                tacRow.operation = Operation.ASSIGN;

                AddHeapTAC addTac = new AddHeapTAC();
                addTac.tempVar = "$t" + (startNumVar + 2);
                tacs.add(addTac);

                PrimitiveType intType = new PrimitiveType();
                intType.type = AllType.INT;
                tacRow.type = intType;

                tacs.add(tacRow);
            }

            for(int i = 1; i < newNode.sizes.size(); i++) {
                createSequenceTacForExp(newNode.sizes.get(i), startNumVar + 2);
                AddHeapTAC tacRow2 = new AddHeapTAC();
                tacRow2.tempVar = "$t" + (startNumVar + 2);

                tacs.add(tacRow2);

                BinOperTAC tacRow3 = new BinOperTAC();

                tacRow3.result.tempVar = "$t" + Integer.toString(startNumVar + 1);
                tacRow3.fOp.tempVar = "$t" + Integer.toString(startNumVar + 1);
                tacRow3.sOp.tempVar = "$t" + Integer.toString(startNumVar + 2);
                tacRow3.operation = Operation.MULTIPLY;

                PrimitiveType intType3 = new PrimitiveType();
                intType3.type = AllType.INT;
                tacRow3.type = intType3;

                tacs.add(tacRow3);
            }

            if(newNode.sizes.size() == 0) {
                BinOperTAC tac = new BinOperTAC(
                        new Operand("$t" + (startNumVar), null, null),
                        new Operand(null, Integer.toString(newNode.sizeOfStruct), null),
                        Operation.NEW
                );
                tacs.add(tac);
            } else {
                BinOperTAC tac = new BinOperTAC(
                        new Operand("$t" + (startNumVar), null, null),
                        new Operand("$t" + (startNumVar + 1), null, null),
                        new Operand(null, Integer.toString(newNode.sizeOfStruct), null),
                        Operation.NEW
                );
                tacs.add(tac);
            }



        } else {
            fillTree(exp, false);
            exp.register = startNumVar;
            fillRegisterTree(exp, 1);
            generateCodeForExp(exp, 1);
        }

        return "$t" + startNumVar;
    }

    public static Operand generateOperand(ExpNode node) {
        if(node instanceof ConstNode) {
            ConstNode cNode = (ConstNode) node;

            return new Operand(null, cNode.getValue(), null);
        }

        if(node instanceof IdNode) {
            IdNode var = (IdNode) node;

            return new Operand((VarId)var.id);
        }

        if(node instanceof FuncCallNode) {
            FuncCallNode call = (FuncCallNode) node;

            int num = 0;
            for(ExpNode arg : call.args) {
                createSequenceTacForExp(arg, 7);

                ParamTAC tac = new ParamTAC();
                tac.tVar = "$t7";
                tac.number = num++;

                tacs.add(tac);
            }

            BinOperTAC tac = new BinOperTAC();
            tac.operation = Operation.CALL;
            Operand op = new Operand();
            Operand res = new Operand();
            res.tempVar = "$t7";
            op.refFunc = call.func;
            tac.fOp = op;
            tac.result = res;

            tacs.add(tac);

            return new Operand(null, "$t7", null);
        }

        if(node instanceof  ComplexOperand) {
            ComplexOperand complex = (ComplexOperand)node;
            if(complex.operands.size() == 1) {
                return new Operand((VarId)((IdNode)complex.operands.get(0)).id);
            }

            createComplexeOperandSequence((ComplexOperand)node, 5/*some magic constant*/);
            BinOperTAC tac = new BinOperTAC();
            tac.result.tempVar = "$t5";
            tac.fOp.tempVar = "$t5";
            tac.operation = Operation.LOAD;
            tacs.add(tac);

            return new Operand(null, "$t5", null);
        }

        return null;
    }

    public static void generateCodeForExp(ExpNode node, int level) {
        if (node.label == 0) {
            return;
        }

        if (node.getFirstOperand() == null && node.label == 1) {
            BinOperTAC tac = new BinOperTAC(
                    new Operand("$t" + node.register, null, null),
                    generateOperand(node));
            tac.type = node.getTypeExp();

            tacs.add(tac);

            //System.out.println("MOVE " + cNode.getValue() + ", R" + node.register);
            return;
        }

        if (node.getFirstOperand() != null && node.getFirstOperand().label == 0) {
            generateCodeForExp(node.getSecondOperand(), level + 1);

            BinOperTAC tac = new BinOperTAC(
                    new Operand("$t" + node.register, null, null),
                    generateOperand(node.getFirstOperand()),
                    new Operand("$t" + node.register, null, null),
                    Operation.getOperationByValue(node.getOperator()));
            tac.type = node.getTypeExp();

            tacs.add(tac);
            //System.out.println("R" + node.register + " = " + cNode.getValue() + " " + node.getOperator() + " " + "R" + node.register);
            return;
        }

        if (node.getFirstOperand().getFirstOperand() != null && node.getSecondOperand().getFirstOperand() != null) {
            if (node.getSecondOperand().label >= node.getFirstOperand().label) {
                if (node.getSecondOperand().label >= node.getFirstOperand().label) {
                    generateCodeForExp(node.getSecondOperand(), level + 1);
                    generateCodeForExp(node.getFirstOperand(), level + 1);
                } else {
                    generateCodeForExp(node.getFirstOperand(), level + 1);
                    generateCodeForExp(node.getSecondOperand(), level + 1);
                }

                BinOperTAC tac = new BinOperTAC(
                        new Operand("$t" + node.getSecondOperand().register, null, null),
                        new Operand( "$t" + node.getFirstOperand().register, null, null),
                        new Operand("$t" + node.getSecondOperand().register, null, null),
                        Operation.getOperationByValue(node.getOperator()));
                tac.type = node.getTypeExp();

                tacs.add(tac);

                //System.out.println("R" + node.getSecondOperand().register + "=" + "R" + node.getFirstOperand().register +
                //       " " + node.getOperator() + " R" + node.getSecondOperand().register);
            } else {
                if (node.getSecondOperand().label >= node.getFirstOperand().label) {
                    generateCodeForExp(node.getSecondOperand(), level + 1);
                    generateCodeForExp(node.getFirstOperand(), level + 1);
                } else {
                    generateCodeForExp(node.getFirstOperand(), level + 1);
                    generateCodeForExp(node.getSecondOperand(), level + 1);
                }
                BinOperTAC tac = new BinOperTAC(
                        new Operand("$t" + node.getSecondOperand().register, null, null),
                        new Operand( "$t" + node.getFirstOperand().register, null, null),
                        new Operand("$t" + node.getSecondOperand().register, null, null),
                        Operation.getOperationByValue(node.getOperator()));
                tac.type = node.getTypeExp();

                tacs.add(tac);

                //System.out.println("R" + node.getSecondOperand().register + "=" + "R" + node.getFirstOperand().register +
                //        " " + node.getOperator() + " R" + node.getSecondOperand().register);

                tac = new BinOperTAC(
                        new Operand("$t" + node.getFirstOperand().register, null, null),
                        new Operand("$t" + node.getSecondOperand().register,null, null));
                tac.type = node.getTypeExp();
                tacs.add(tac);
                //System.out.println("MOVE " + " R" + node.getSecondOperand().register + ",R" + node.getFirstOperand().register);
            }
        } else {
            if (node.getSecondOperand().label >= node.getFirstOperand().label) {
                generateCodeForExp(node.getSecondOperand(), level + 1);
                generateCodeForExp(node.getFirstOperand(), level + 1);
            } else {
                generateCodeForExp(node.getFirstOperand(), level + 1);
                generateCodeForExp(node.getSecondOperand(), level + 1);
            }

            if (node.getFirstOperand().label == 0) {
                BinOperTAC tac = new BinOperTAC(
                        new Operand("$t" + node.register, null, null),
                        new Operand("$t" + node.getSecondOperand().register, null, null),
                        generateOperand(node.getFirstOperand()),
                        Operation.getOperationByValue(node.getOperator()));
                tac.type = node.getTypeExp();
                tacs.add(tac);
                //System.out.println("R" + node.register + "=" + "R" + node.getFirstOperand().register +
                //        " " + node.getOperator() + cNode.getValue());
            } else {
                BinOperTAC tac = new BinOperTAC(
                        new Operand("$t" + node.register, null, null),
                        new Operand("$t" + node.getFirstOperand().register, null, null),
                        new Operand("$t" + node.getSecondOperand().register,null, null),
                        Operation.getOperationByValue(node.getOperator()));
                tac.type = node.getTypeExp();
                tacs.add(tac);
                //System.out.println("R" + node.register + "=" + "R" + node.getFirstOperand().register +
                //        " " + node.getOperator() + " " + "R" + node.getSecondOperand().register);
            }
        }
    }

    public static void fillRegisterTree(ExpNode exp, int level) {
        if (exp.getFirstOperand() == null) {

        } else {
            if (exp.getFirstOperand().label <= exp.getSecondOperand().label) {
                exp.getFirstOperand().register = exp.register + 1;
                exp.getSecondOperand().register = exp.register;
            } else {
                exp.getSecondOperand().register = exp.register + 1;
                exp.getFirstOperand().register = exp.register;
            }

            fillRegisterTree(exp.getFirstOperand(), level + 1);
            fillRegisterTree(exp.getSecondOperand(), level + 1);
        }
    }

    public static void fillTree(ExpNode exp, boolean isLeft) {
        if (exp.getFirstOperand() != null) {
            fillTree(exp.getFirstOperand(), true);
            fillTree(exp.getSecondOperand(), false);

            if (exp.getFirstOperand().label == exp.getSecondOperand().label) {
                exp.label = exp.getFirstOperand().label + 1;
            } else {
                exp.label = Math.max(exp.getFirstOperand().label, exp.getSecondOperand().label);
            }
        } else {
            if (isLeft) {
                exp.label = 0;
            } else {
                exp.label = 1;
            }
        }
    }

    public static void createStartFor(ForNode node, int labelFor) {
        createAssignStatement(node.declaration.var, node.declaration.expNode);
        createLabelStatement("$L" + Integer.toString(labelFor) + "_start:");
        curCycle = "$L" + Integer.toString(labelFor);
        String tVar = createSequenceTacForExp(node.condition, 0);
        createIfFalse(tVar, "$L" + Integer.toString(labelFor) + "_end:");
    }

    public static void createIfFalse(String tVar, String labelTo) {
        BinOperTAC tac = new BinOperTAC();
        Operand op1 = new Operand();
        Operand op2 = new Operand();
        op1.tempVar = tVar;
        op2.label = labelTo;
        tac.operation = Operation.IFFALSE;
        tac.fOp = op1;
        tac.sOp = op2;

        tacs.add(tac);
    }

    public static void createIfTrue(String tVar, String labelTo) {
        BinOperTAC tac = new BinOperTAC();
        Operand op1 = new Operand();
        Operand op2 = new Operand();
        op1.tempVar = tVar;
        op2.label = labelTo;
        tac.operation = Operation.IFTRUE;
        tac.fOp = op1;
        tac.sOp = op2;

        tacs.add(tac);
    }

    public static void createEndFor(ForNode node, int labelFor) {
        createAssignStatement(node.assign.lval, node.assign.rval);
        createGoTOStatement("$L" + Integer.toString(labelFor) + "_start:");
        createLabelStatement("$L" + Integer.toString(labelFor) + "_end:");
    }

    public static void createStartWhile(WhileNode node, int labelWhile) {
        createLabelStatement("$L" + Integer.toString(labelWhile) + "_start:");
        curCycle = "$L" + Integer.toString(labelWhile);
        String tVar = createSequenceTacForExp(node.condition, 0);
        createIfFalse(tVar, "$L" + Integer.toString(labelWhile) + "_end:");
    }

    public static void createEndWhile(int labelWhile) {
        createGoTOStatement("$L" + Integer.toString(labelWhile) + "_start");
        createLabelStatement("$L" + Integer.toString(labelWhile) + "_end");
    }

    public static void createStartDoWhile(int labelWhile) {
        createLabelStatement("$L" + Integer.toString(labelWhile) + "_start:");
        curCycle = "$L" + Integer.toString(labelWhile);
    }

    public static void createEndDoWhile(WhileNode node, int labelWhile) {
        String tVar = createSequenceTacForExp(node.condition, 0);
        createIfTrue(tVar, "$L" + Integer.toString(labelWhile) + "_start:");
        createLabelStatement("$L" + Integer.toString(labelWhile) + "_end");
    }

    public static void createStartIf(IfNode node, int labelIf) {
        String tVar = createSequenceTacForExp(node.condition, 0);
        createIfFalse(tVar, "$L" + Integer.toString(labelIf) + "_false:");
    }

    public static void createEndIf(int labelIf) {
        createGoTOStatement("$L" + Integer.toString(labelIf) + "_end:");
        createLabelStatement("$L" + Integer.toString(labelIf) + "_false:");
    }

    public static void createEndIfElse(int labelIf) {
        createLabelStatement("$L" + Integer.toString(labelIf) + "_end:");
    }

    public static void createVoidFuncCall(FuncCallStmNode node) {
        for (int i = 0; i < node.args.size(); i++) {
            String tVar = createSequenceTacForExp(node.args.get(i), 0);

            ParamTAC tac = new ParamTAC();
            tac.number = i;
            tac.tVar = tVar;

            tacs.add(tac);
        }

        BinOperTAC tac = new BinOperTAC();
        tac.operation = Operation.CALL;
        Operand op = new Operand();
        op.refFunc = node.func;
        tac.fOp = op;

        tacs.add(tac);
    }

    public static void print() {
        for (TAC tac : tacs) {
            if (tac instanceof LabelTAC) {
                LabelTAC tac2 = (LabelTAC) tac;
                System.out.println(tac2.label);
            }

            if (tac instanceof GoToTAC) {
                GoToTAC tac2 = (GoToTAC) tac;
                System.out.println("goto " + tac2.label);
            }

            if (tac instanceof BinOperTAC) {
                printExp((BinOperTAC)tac);
            }

            if (tac instanceof FuncDeclTAC) {
                FuncDeclTAC tac2 = (FuncDeclTAC) tac;
                System.out.println(tac2.name + ":");
            }

            if (tac instanceof ReturnTAC) {
                ReturnTAC tac2 = (ReturnTAC) tac;
                System.out.println("return " + (tac2.tVar == null ? "" : tac2.tVar));
            }

            if (tac instanceof ParamTAC) {
                ParamTAC tac2 = (ParamTAC) tac;
                System.out.println("param " + tac2.number + " " + tac2.tVar);
            }

            if (tac instanceof IfTrueTAC) {
                IfTrueTAC tac2 = (IfTrueTAC) tac;
                System.out.println("ifTrue " + tac2.tVar + " goto " + tac2.label);
            }

            if (tac instanceof IfFalseTAC) {
                IfFalseTAC tac2 = (IfFalseTAC) tac;
                System.out.println("ifTrue " + tac2.tVar + " goto " + tac2.label);
            }
        }
    }

    public static void printExp(BinOperTAC tac) {
        if(tac.operation == null || tac.operation == Operation.ASSIGN || tac.operation == Operation.LOAD ||
                tac.operation == Operation.SAVE || tac.operation == Operation.ARRAY || tac.operation == Operation.CALL ||
                tac.operation == Operation.NEW) {
            String fOp = "";
            String result = "";

            if(tac.fOp == null) return;

            if(tac.fOp.constant != null) {
                fOp = tac.fOp.constant;
            }

            if(tac.fOp.tempVar != null) {
                fOp = tac.fOp.tempVar;
            }

            if(tac.fOp.refVar != null) {
                fOp = tac.fOp.refVar.name;
            }

            if(tac.result.refVar != null) {
                result = tac.result.refVar.name;
            }

            if(tac.result.constant != null) {
                result = tac.result.constant;
            }

            if(tac.result.tempVar != null) {
                result = tac.result.tempVar;
            }

            if(tac.operation == Operation.LOAD || tac.operation == Operation.SAVE) {
                if(tac.sOp.constant == null) {
                    System.out.println(tac.operation.value + " " + result + " " + fOp);
                } else {
                    System.out.println(tac.operation.value + " " + result + " " + fOp + " " + tac.sOp.constant);
                }
            } else if(tac.operation == Operation.ARRAY) {
                System.out.println(result + " = " + result + "[" + fOp + "]");
            } else if(tac.operation == Operation.CALL) {
                System.out.println("call " + tac.fOp.refFunc.name + " " + tac.result.tempVar);
            } else if(tac.operation == Operation.NEW) {
                System.out.println("new " + tac.result.tempVar + " " + tac.fOp.tempVar + " " + tac.sOp.constant);
            } else if(tac.operation == Operation.ASSIGN) {
                System.out.println(result + " = " + fOp);
            }  else {
                System.out.println(result + " = " + fOp);
            }

            return;
        }

        switch (tac.operation) {
            case IFTRUE:
                System.out.println("ifTrue " + tac.fOp.tempVar + " goto " + tac.sOp.label);
                break;
            case IFFALSE:
                System.out.println("ifFalse " + tac.fOp.tempVar + " goto " + tac.sOp.label);
                break;
            case ARRAY:
                break;
            default:
                String fOp = "";
                String sOp = "";
                String result = "";

                if(tac.fOp.constant != null) {
                    fOp = tac.fOp.constant;
                }

                if(tac.fOp.tempVar != null) {
                    fOp = tac.fOp.tempVar;
                }

                if(tac.fOp.refVar != null) {
                    fOp = tac.fOp.refVar.name;
                }

                if(tac.sOp.constant != null) {
                    sOp = tac.sOp.constant;
                }

                if(tac.sOp.tempVar != null) {
                    sOp = tac.sOp.tempVar;
                }

                if(tac.result.constant != null) {
                    result = tac.result.constant;
                }

                if(tac.result.tempVar != null) {
                    result = tac.result.tempVar;
                }

                System.out.println(result + " = " + fOp + " " + tac.operation.value + " " + sOp);
        }
    }
}
