package parser;

import beans.node.*;
import beans.node.exp.ExpNode;
import beans.type.PrimitiveType;
import beans.type.TypeVar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class LLVMGenerator {
    private static DeclareFuncNode curFunction;
    private static Stack<WhileNode> curCycle = new Stack<WhileNode>();
    private static int currentIndex = 0;

    public static void generateLLVM(List<AbstractNode> list) {
        for(AbstractNode node : list) {
            if(node instanceof DeclareFuncNode) {
                currentIndex = 0;
                curFunction = ((DeclareFuncNode) node);
                generateFunction((DeclareFuncNode)node);
            }

            if(node instanceof DeclareStructNode) {
                generateStructDeclaration((DeclareStructNode) node);
            }
        }
    }

    private static void generateFunction(DeclareFuncNode funcNode) {
        String params = generateParamsList(funcNode.getId().properties);

        // define i32 @main() #0 {
        System.out.println("define "
                + getType(funcNode.getId().returnType)
                + " @"
                + funcNode.getId().name
                + " ("
                + params
                + ") "
                + "{");

        currentIndex += funcNode.getId().properties.size();

        generateStatementList(funcNode.getStatementNodes());

        System.out.println("}");
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
            whileNode.uniqueId = System.currentTimeMillis();
            curCycle.push(whileNode);

            generateWhile((WhileNode)statement);
            curCycle.pop();
        }

        if(statement.typeNode == AbstractNode.DO_WHILE_NODE) {
            WhileNode whileNode = (WhileNode)statement;
            whileNode.uniqueId = System.currentTimeMillis();
            curCycle.push(whileNode);

            generateWhile((WhileNode)statement);
            curCycle.pop();
        }

        if(statement.typeNode == AbstractNode.FOR_NODE) {
            ForNode forNode = (ForNode)statement;
            forNode.uniqueId = System.currentTimeMillis();
            curCycle.push(forNode);

            generateFor(forNode);
            curCycle.pop();
        }

        if(statement.typeNode == AbstractNode.SWITCH_NODE) {
            generateSwitch((SwitchNode)statement);
        }
    }

    private static void generateWhile(WhileNode whileNode) {
        System.out.println("; <label>:" + currentIndex + ":");
        whileNode.startIndex = "%" + currentIndex;
        currentIndex++;

        generateExpression(whileNode.condition);
        System.out.println("br i1 %" + currentIndex + ", label " + whileNode.startIndex + ", label %" + whileNode.uniqueId);
        currentIndex++;

        generateStatementList(whileNode.statements);

        //TODO replace all while unique IDS
        System.out.println("; <label>:" + currentIndex + ":");
        currentIndex++;
    }

    private static void generateDoWhile(WhileNode whileNode) {
        System.out.println("; <label>:" + currentIndex + ":");
        whileNode.startIndex = "%" + currentIndex;
        currentIndex++;

        generateStatementList(whileNode.statements);

        generateExpression(whileNode.condition);
        System.out.println("br i1 %" + currentIndex + ", label %" + whileNode.startIndex + ", label %" + whileNode.uniqueId);

        //TODO replace all while unique IDS
        System.out.println("; <label>:" + currentIndex + ":");
        currentIndex++;
    }

    private static void generateFor(ForNode forNode) {
        generateDeclaration(forNode.declaration);

        System.out.println("; <label>:" + currentIndex + ":");
        forNode.startIndex = "%" + currentIndex;
        currentIndex++;

        generateExpression(forNode.condition);
        int indexBodyFor = currentIndex + 1;
        System.out.println("br i1 %" + currentIndex++ + ", label %" + indexBodyFor + ", label %" + forNode.uniqueId);
        currentIndex++;

        System.out.println("; <label>:" + indexBodyFor + ":");
        generateStatementList(forNode.statements);
        generateAssignment(forNode.assign);
        System.out.println("br label " + forNode.startIndex);
    }

    private static void generateSwitch(SwitchNode switchNode) {
        generateExpression(switchNode.exp);
        switchNode.endIndex = System.currentTimeMillis();
        System.out.println("switch " + getType(switchNode.exp.getTypeExp()) + " %" + currentIndex + ", label %" + switchNode.endIndex + "[");
        currentIndex++;

        for(Map.Entry<String, ArrayList<StatementNode>> switchCase : switchNode.cases.entrySet()) {
            switchNode.caseIndexes.put(switchCase.getKey(), System.currentTimeMillis());
            System.out.println(getType(switchCase.getKey()) + " " + switchCase.getKey() + ", label %" + switchNode.caseIndexes.get(switchCase.getKey()));
        }

        System.out.println("]");

        for(Map.Entry<String, ArrayList<StatementNode>> switchCase : switchNode.cases.entrySet()) {
            // TODO replace temporary indexes
            System.out.println("; <label>:" + currentIndex + ":");
            currentIndex++;

            generateStatementList(switchCase.getValue());

            System.out.println("br label %" + switchNode.endIndex);
        }

        System.out.println("; <label>:" + currentIndex + ":");
        currentIndex++;

        // TODO replace temporary indexes
    }

    private static void generateExpression(ExpNode expNode) {

    }

    private static void generateDeclaration(DeclareVarNode declareVarNode) {

    }

    private static void generateAssignment(AssignNode assignNode) {

    }

    private static void generateContinue() {
        System.out.println("br label " + curCycle.peek().startIndex);
    }

    private static void generateBreak() {
        System.out.println("br label " + curCycle.peek().uniqueId);
    }

    private static void generateGoto(GotoNode gotoNode) {
        String index = "";

        for(LabelNode labelNode : curFunction.getLabels()) {
            if(labelNode.name.equals(gotoNode.label)) {
                index = labelNode.lId;
            }
        }

        // br label %1
        System.out.println("br label " + index);
    }

    private static void generateLabel(LabelNode labelNode) {
        // ; <label>:2:
        System.out.println("; <label>:" + currentIndex + ":");

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
        System.out.println("%struct." + structNode.id.name + " = type { " + params + " }");
    }

    private static String getType(TypeVar type) {
        // TODO

        if(type instanceof PrimitiveType) {
            PrimitiveType primitiveType = (PrimitiveType)type;
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

        return null;
    }

    private static String getType(String exp) {
        try {
            Integer f = Integer.parseInt(exp);
            return "i32";
        } catch (NumberFormatException e) {
            return "float";
        }
    }
}
