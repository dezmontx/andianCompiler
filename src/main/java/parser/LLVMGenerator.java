package parser;

import beans.id.IdRow;
import beans.id.VarId;
import beans.node.*;
import beans.node.exp.*;
import beans.type.ArrayType;
import beans.type.PrimitiveType;
import beans.type.StructType;
import beans.type.TypeVar;

import java.util.*;

public class LLVMGenerator {
    private static DeclareFuncNode curFunction;
    private static Stack<WhileNode> curCycle = new Stack<WhileNode>();
    private static int currentIndex = 0;
    private static String llvm = "";

    public static void generateLLVM(List<AbstractNode> list) {
        for(AbstractNode node : list) {
            if(node instanceof DeclareFuncNode) {
                currentIndex = 1;
                curFunction = ((DeclareFuncNode) node);
                generateFunction((DeclareFuncNode)node);
            }

            if(node instanceof DeclareStructNode) {
                generateStructDeclaration((DeclareStructNode) node);
            }
        }

        System.out.println(llvm);
    }

    private static void generateFunction(DeclareFuncNode funcNode) {
        String params = generateParamsList(funcNode.getId().properties);

        // define i32 @main() #0 {
        llvm += "define "
                + getType(funcNode.getId().returnType)
                + " @"
                + funcNode.getId().name
                + " ("
                + params
                + ") "
                + "{\n";

        currentIndex += funcNode.getId().properties.size();

        for(Map.Entry<TypeVar, String> prop : funcNode.getId().properties.entrySet()) {
            llvm += "%" + currentIndex + " = alloca " + getType(prop.getKey()) + ", " + getAlignForType(prop.getKey()) + "\n";
            llvm += "store " + getType(prop.getKey()) + " %" + (currentIndex - funcNode.getId().properties.size() - 1)
                    + ", " + getPointerForType(prop.getKey()) + " %" + currentIndex + " ," + getAlignForType(prop.getKey()) + "\n";

            ((VarId)funcNode.getId().propIds.get(prop.getValue())).index = currentIndex;

            currentIndex++;
        }

        generateStatementList(funcNode.getStatementNodes());

        llvm += "}\n";
    }

    private static String generateParamsList(Map<TypeVar, String> params) {
        String paramsStr = "";
        for(Map.Entry<TypeVar, String> entry : params.entrySet()) {
            paramsStr += getType(entry.getKey()) + ",";
        }

        if(paramsStr.length() != 0) {
            paramsStr = paramsStr.substring(0, paramsStr.length() - 1);
        }

        return paramsStr;
    }

    private static void generateStatementList(List<StatementNode> statements) {
        for(StatementNode statement : statements) {
            generateStatement(statement);
        }
    }

    private static void generateStatement(StatementNode statement) {
        if(statement instanceof LabelNode) {
            generateLabel((LabelNode)statement);
        }

        if(statement instanceof GotoNode) {
            generateGoto((GotoNode)statement);
        }

        if(statement.typeNode == AbstractNode.CONTINUE_NODE) {
            generateContinue();
        }

        if(statement.typeNode == AbstractNode.BREAK_NODE) {
            generateBreak();
        }

        if(statement.typeNode == AbstractNode.WHILE_NODE) {
            WhileNode whileNode = (WhileNode)statement;
            whileNode.uniqueId = getRand();
            curCycle.push(whileNode);

            generateWhile((WhileNode)statement);
            curCycle.pop();
        }

        if(statement.typeNode == AbstractNode.DO_WHILE_NODE) {
            WhileNode whileNode = (WhileNode)statement;
            whileNode.uniqueId = getRand();
            curCycle.push(whileNode);

            generateDoWhile((WhileNode)statement);
            curCycle.pop();
        }

        if(statement.typeNode == AbstractNode.FOR_NODE) {
            ForNode forNode = (ForNode)statement;
            forNode.uniqueId = getRand();
            curCycle.push(forNode);

            generateFor(forNode);
            curCycle.pop();
        }

        if(statement.typeNode == AbstractNode.SWITCH_NODE) {
            generateSwitch((SwitchNode)statement);
        }

        if(statement.typeNode == AbstractNode.IF_NODE) {
            generateIf((IfNode)statement);
        }

        if(statement.typeNode == AbstractNode.ASSIGN_NODE) {
            generateAssignment((AssignNode)statement);
        }

        if(statement.typeNode == AbstractNode.RETURN_NODE) {
            generateReturn((ControlNode)statement);
        }

        if(statement.typeNode == AbstractNode.DECLARE_VAR_NODE) {
            generateDeclaration((DeclareVarNode)statement);
        }

        if(statement.typeNode == AbstractNode.FUNC_CALL_NODE) {

        }
    }

    private static void generateIf(IfNode statement) {
        if(statement.elseBranch == null || statement.elseBranch.size() == 0) {
            generateExpression(statement.condition);
            Integer startIfIndex = currentIndex + 1;
            Long end = getRand();
            llvm += "br i1 %" + currentIndex++ + ", label %" + startIfIndex + ", label %" + end +"\n";

            llvm += "; <label>:" + startIfIndex + ":" +"\n";
            currentIndex++;
            generateStatementList(statement.ifBranch);
            llvm += "br label %" + end  +"\n";

            llvm += "; <label>:" + currentIndex++ + ":" +"\n";
            llvm = llvm.replace(Long.toString(end), Long.toString(currentIndex - 1));
        } else {
            generateExpression(statement.condition);
            Integer startIfIndex = currentIndex + 1;
            Long startElseIndex = getRand();
            Long end = getRand();
            llvm += "br i1 %" + currentIndex++ + ", label %" + startIfIndex + ", label %" + startElseIndex +"\n";

            llvm += "; <label>:" + startIfIndex + ":" +"\n";
            currentIndex++;
            generateStatementList(statement.ifBranch);
            llvm += "br label %" + end  +"\n";

            llvm += "; <label>:" + currentIndex + ":" +"\n";
            llvm = llvm.replace(Long.toString(startElseIndex), Long.toString(currentIndex));
            currentIndex++;
            generateStatementList(statement.elseBranch);
            llvm += "br label %" + end  +"\n";

            llvm += "; <label>:" + currentIndex++ + ":" +"\n";
            llvm = llvm.replace(Long.toString(end), Long.toString(currentIndex - 1));
        }
    }

    private static void generateWhile(WhileNode whileNode) {
        llvm += "; <label>:" + currentIndex + ":" +"\n";
        whileNode.startIndex = "%" + currentIndex;
        currentIndex++;

        generateExpression(whileNode.condition);
        llvm += "br i1 %" + currentIndex + ", label " + whileNode.startIndex + ", label %" + whileNode.uniqueId +"\n";
        currentIndex++;

        generateStatementList(whileNode.statements);

        llvm += "; <label>:" + currentIndex + ":" +"\n";
        llvm = llvm.replace(Long.toString(whileNode.uniqueId), Long.toString(currentIndex));
        currentIndex++;
    }

    private static void generateDoWhile(WhileNode whileNode) {
        llvm += "; <label>:" + currentIndex + ":" +"\n";
        whileNode.startIndex = "%" + currentIndex;
        currentIndex++;

        generateStatementList(whileNode.statements);

        generateExpression(whileNode.condition);
        llvm += "br i1 %" + currentIndex + ", label %" + whileNode.startIndex + ", label %" + whileNode.uniqueId +"\n";

        llvm = llvm.replace(Long.toString(whileNode.uniqueId), Long.toString(currentIndex));
        llvm += "; <label>:" + currentIndex + ":" +"\n";
        currentIndex++;
    }

    private static void generateFor(ForNode forNode) {
        generateDeclaration(forNode.declaration);

        llvm += "; <label>:" + currentIndex + ":" +"\n";
        forNode.startIndex = "%" + currentIndex;
        currentIndex++;

        generateExpression(forNode.condition);
        int indexBodyFor = currentIndex + 1;
        llvm += "br i1 %" + currentIndex++ + ", label %" + indexBodyFor + ", label %" + forNode.uniqueId +"\n";
        currentIndex++;

        llvm += "; <label>:" + indexBodyFor + ":" +"\n";
        generateStatementList(forNode.statements);
        generateAssignment(forNode.assign);
        llvm += "br label " + forNode.startIndex +"\n";
        llvm += "; <label>:" + currentIndex + ":" +"\n";
        llvm = llvm.replace(Long.toString(forNode.uniqueId), Long.toString(currentIndex));
        currentIndex++;
    }

    private static void generateSwitch(SwitchNode switchNode) {
        generateExpression(switchNode.exp);
        switchNode.endIndex = getRand();
        llvm += "switch " + getType(switchNode.exp.getTypeExp()) + " %" + currentIndex + ", label %" + switchNode.endIndex + "[" +"\n";
        currentIndex++;

        for(Map.Entry<String, ArrayList<StatementNode>> switchCase : switchNode.cases.entrySet()) {
            switchNode.caseIndexes.put(switchCase.getKey(), getRand());
            llvm += getType(switchCase.getKey()) + " " + switchCase.getKey() + ", label %" + switchNode.caseIndexes.get(switchCase.getKey()) +"\n";
        }

        llvm += "]" +"\n";

        for(Map.Entry<String, ArrayList<StatementNode>> switchCase : switchNode.cases.entrySet()) {
            llvm += "; <label>:" + currentIndex + ":" + "\n";
            llvm = llvm.replace(Long.toString(switchNode.caseIndexes.get(switchCase.getKey())), Long.toString(currentIndex));
            currentIndex++;

            generateStatementList(switchCase.getValue());

            llvm += "br label %" + switchNode.endIndex + "\n";
        }

        llvm += "; <label>:" + currentIndex + ":" + "\n";
        llvm = llvm.replace(Long.toString(switchNode.endIndex), Long.toString(currentIndex));
        currentIndex++;
    }

    private static int generateExpression(ExpNode expNode) {
        if(expNode instanceof NewNode) {
            NewNode newNode = (NewNode)expNode;
            // TODO

            return currentIndex;
        }

        if(expNode instanceof IdNode) {
            IdNode idNode = (IdNode)expNode;

            //((VarId)idNode.id).index;
            //   %6 = load i32, i32* %2, align 4
            currentIndex++;
            llvm += "%" + currentIndex + " = load " + getType(((VarId)idNode.id).type) + ", " +
                    getPointerForType(((VarId)idNode.id).type) + " %" + ((VarId)idNode.id).index + ", " +
                    getAlignForType(((VarId)idNode.id).type);

            return currentIndex;
        }

        if(expNode instanceof ConstNode) {
            //   %1 = alloca i32, align 4
            //   store i32 0, i32* %1, align 4
            return currentIndex;
        }

        if(expNode instanceof FuncCallNode) {
            FuncCallNode funcCallNode = (FuncCallNode)expNode;

            //   %3 = call i32 @c(i32 3)

            return currentIndex;
        }

        if(expNode instanceof ComplexOperand) {
            ComplexOperand complexOperand = (ComplexOperand)expNode;

            // TODO та же штука что и в assign только без store
            // load ....

            return currentIndex;
        }

        switch (expNode.operator) {
            case "+":
                /*int index1 = generateExpression(expNode.firstOperand);
                int index2 = generateExpression(expNode.secondOperand);

                currentIndex++;
                // %currentIndex = add

                return currentIndex;*/
                break;
            case "-":
                break;
            case "*":
                break;
            case "/":
                break;
            case "&":
                break;
            case "|":
                break;
            case ">":
                break;
            case ">=":
                break;
            case "<":
                break;
            case "<=":
                break;
            case "==":
                break;
            case "!=":
                break;

        }

        return currentIndex;
    }

    private static void generateDeclaration(DeclareVarNode declareVarNode) {
        llvm += "%" + currentIndex + " = alloca " + getType(declareVarNode.type) + ", ";

        llvm += getAlignForType(declareVarNode.type) + "\n";

        declareVarNode.var.index = currentIndex;

        currentIndex++;

        if(declareVarNode.expNode != null) {
            generateExpression(declareVarNode.expNode);
            llvm += "store " + getType(declareVarNode.type) + " %" + currentIndex + ", "
                    + getPointerForType(declareVarNode.type) + " %" + declareVarNode.var.index + ", " + getAlignForType(declareVarNode.type) + "\n";

            currentIndex++;
        }
    }

    private static void generateAssignment(AssignNode assignNode) {
        generateExpression(assignNode.rval);
        int expIndex = currentIndex;

        if(assignNode.lval.operands.size() == 1) {
            llvm += "store " + getType(assignNode.rval.getTypeExp()) + " %" + currentIndex + ", " +
                    getPointerForType(assignNode.rval.getTypeExp()) + " %" + ((VarId)((IdNode)assignNode.lval.operands.get(0)).id).index
                    + "," + getAlignForType(assignNode.rval.getTypeExp()) + "\n";
            currentIndex++;
        } else {
            int indexBase = ++currentIndex;
            TypeVar typeVar = ((VarId)((IdNode)assignNode.lval.operands.get(0)).id).type;
            llvm += "%" + indexBase + " = load " + getType(typeVar) + ", " + getPointerForType(typeVar)
                    + " %" + ((VarId) ((IdNode)assignNode.lval.operands.get(0)).id).index + ", " + getAlignForType(typeVar) + "\n";

            currentIndex++;

            assignNode.lval.operands.remove(0);
            int curLevelOfArray = 1;

            for(ExpNode exp : assignNode.lval.operands) {
                if(exp instanceof FieldNode) {
                    FieldNode field = (FieldNode)exp;
                    String type = "";
                    String typePointer = "";

                    if(typeVar instanceof ArrayType) {
                        type = getType(typeVar).replaceAll("\\*", "");
                        typePointer = type + "*";
                    } else {
                        type = getType(typeVar);
                        typePointer = getPointerForType(typeVar);
                    }

                    llvm += "%" + currentIndex + " = getelementptr inbounds " + type + ", "
                            + typePointer + " %" + indexBase + ", i32 0, i32 " +  field.getOrderNumber() + "\n";

                    typeVar = exp.getTypeExp();
                    indexBase = currentIndex++;
                    curLevelOfArray = 1;
                } else {
                    generateExpression(exp);
                    int indexExp = currentIndex++;
                    llvm += "%" + currentIndex + " = sext i32 %" + indexExp + " to i64\n";
                    indexExp = currentIndex++;
                    llvm += "%" + currentIndex + " = getelementptr inbounds " + getType(typeVar, curLevelOfArray) + ", "
                    + getPointerForType(typeVar, curLevelOfArray) + " %" + indexBase + ", i64" + " %" + indexExp + "\n";

                    String type = getType(typeVar, curLevelOfArray);

                    if(!(type.replace("*", "").equals(type))) {
                        indexExp = currentIndex++;

                        llvm += "%" + currentIndex + " = load " + getType(typeVar, curLevelOfArray) + ", "
                                + getPointerForType(typeVar, curLevelOfArray) + " %" + indexExp
                                + " align " + (type.replace("*", "").equals(type) ? "4\n" : "8\n");

                    }

                    indexBase = currentIndex++;

                    curLevelOfArray++;
                }
            }

            llvm += "store " + getType(assignNode.rval.getTypeExp()) + " %" + expIndex + ", " +
                    getPointerForType(assignNode.rval.getTypeExp()) + " %" + indexBase
                    + "," + getAlignForType(assignNode.rval.getTypeExp()) + "\n";
        }
    }

    private static void generateContinue() {
        llvm += "br label " + curCycle.peek().startIndex + "\n";
    }

    private static void generateBreak() {
        llvm += "br label " + curCycle.peek().uniqueId + "\n";
    }

    private static void generateGoto(GotoNode gotoNode) {
        String index = "";

        for(LabelNode labelNode : curFunction.getLabels()) {
            if(labelNode.name.equals(gotoNode.label)) {
                index = labelNode.lId;
            }
        }

        // br label %1
        llvm += "br label " + index + "\n";
    }

    private static void generateLabel(LabelNode labelNode) {
        // ; <label>:2:
        llvm += "; <label>:" + currentIndex + ":"+ "\n";

        for(LabelNode node : curFunction.getLabels()) {
            if(node.name.equals(labelNode.name)) {
                node.lId = "%" + currentIndex;
            }
        }

        currentIndex++;
    }

    private static void generateStructDeclaration(DeclareStructNode structNode) {
        String params = generateParamsList(structNode.id.properties);

        // %struct.Cell = type { i32, i32*, %struct.Cell* }
        llvm += "%struct." + structNode.id.name + " = type { " + params + " }" + "\n";
    }

    private static String getType(TypeVar type) {
        if(type instanceof PrimitiveType) {
            switch (((PrimitiveType)type).type) {
                case INT:
                    return "i32";
                case FLOAT:
                    return "float";
                case BOOLEAN:
                    return "%1";
                case STRING:
                    return "i8*";
                case VOID:
                    return "void";
            }
        }

        if(type instanceof ArrayType) {
            ArrayType arrayType = (ArrayType)type;

            if (arrayType.structId != null) {
                return "%struct." + arrayType.structId.name + getTypePointerForArray(arrayType.levels);
            } else if(arrayType.primitiveType != null) {
                switch (arrayType.primitiveType) {
                    case INT:
                        return "i32" + getTypePointerForArray(arrayType.levels);
                    case FLOAT:
                        return "float" + getTypePointerForArray(arrayType.levels);
                    case BOOLEAN:
                        return "%1" + getTypePointerForArray(arrayType.levels);
                    case STRING:
                        return "i8*" + getTypePointerForArray(arrayType.levels);
                }
            }
        }

        if(type instanceof StructType) {
            return "%struct." + ((StructType) type).structId.name;
        }

        return null;
    }

    private static String getType(TypeVar type, int countAsterisks) {
        String strType = getType(type);

        return strType.substring(0, strType.length() - countAsterisks);
    }

    private static String getPointerForType(TypeVar type, int countAsterisks) {
        String strType = getType(type);

        return strType.substring(0, strType.length() - countAsterisks + 1);
    }

    private static void generateReturn(ControlNode node) {
        llvm += "ret " + getType(node.exp.getTypeExp()) + " %" + currentIndex + "\n";
    }

    private static String getType(String exp) {
        try {
            Integer.parseInt(exp);
            return "i32";
        } catch (NumberFormatException e) {
            return "float";
        }
    }

    private static String getPointerForType(TypeVar typeVar) {
        return getType(typeVar) + "*";
    }

    private static String getTypePointerForArray(int level) {
        String pointer = "";
        for(int i = 0; i < level; i++) {
            pointer += "*";
        }

        return pointer;
    }

    private static String getAlignForType(TypeVar typeVar) {
        if (typeVar instanceof PrimitiveType) {
            switch (((PrimitiveType) typeVar).type) {
                case INT:
                    return "align 4";
                case FLOAT:
                    return "align 4";
                case BOOLEAN:
                    return "align 1";
                case STRING:
                    return "align 8";
            }
        }

        if (typeVar instanceof ArrayType || typeVar instanceof StructType) {
            return "align 8";
        }

        return null;
    }

    private static long getRand() {
        long a = Math.abs(new Random().nextLong());

        return a;
    }
}
