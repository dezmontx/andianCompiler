package parser;

import beans.Token;
import beans.node.*;
import beans.node.exp.*;
import beans.type.ArrayType;
import beans.type.PrimitiveType;
import beans.type.StructType;
import beans.type.TypeVar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by antonskripacev on 03.04.17.
 */
public class Visualiser {
    public void visualiseTableToken(List<Token> listTokens) {
        System.out.println("  =============================================");
        System.out.printf("%3s %-10s %3s %-4s %3s %-15s %3s\n", "|", "type", "|", "line", "|", "value", "|");
        System.out.println("  =============================================");
        for(Token t : listTokens) {
            System.out.printf("%3s %-10s %3s %-4s %3s %-15s %3s\n", "|", t.getType().getValue(), "|", t.getLine(), "|", t.getValue() == null ? "" : t.getValue(), "|");
        }

        System.out.println("  =============================================");
        System.out.println("\n");
    }

    public void visualiseAST(List<? extends AbstractNode> nodes, int level) {
        for(int i = 0 ; i < nodes.size(); ++i) {
            switch (nodes.get(i).typeNode) {
                case AbstractNode.IMPORT:
                    ImportNode nodeI = (ImportNode)nodes.get(i);
                    printTab(level);
                    System.out.println("import " + nodeI.getFileName() + ";");
                    break;
                case AbstractNode.STRUCT:
                    DeclareStructNode nodeS = (DeclareStructNode)nodes.get(i);
                    printTab(level);
                    System.out.println("struct " + nodeS.id.name + " {");

                    for(Map.Entry<TypeVar, String> entry : nodeS.id.properties.entrySet()) {
                        printTab(level + 1);
                        printType(entry.getKey());
                        System.out.println(entry.getValue() + ";");
                    }
                    printTab(level);
                    System.out.println("}\n");
                    break;
                case AbstractNode.FUNC:
                    DeclareFuncNode nodeF = (DeclareFuncNode)nodes.get(i);
                    printTab(level);
                    printType(nodeF.getId().returnType);
                    System.out.print(nodeF.getId().name + " (");
                    int j = 0;
                    for(Map.Entry<TypeVar, String> entry : nodeF.getId().properties.entrySet()) {
                        printType(entry.getKey());
                        System.out.print(entry.getValue());

                        if(j != nodeF.getId().properties.size() - 1) {
                            System.out.print(",");
                        }

                        j++;
                    }
                    System.out.println("){");
                    visualiseAST(nodeF.getStatementNodes(), level + 1);
                    System.out.println("}");
                    System.out.println("");
                    break;
                case AbstractNode.LABEL:
                    LabelNode labelNode = (LabelNode)nodes.get(i);
                    printTab(level);
                    System.out.println("#" + labelNode.getName());
                    break;
                case AbstractNode.GOTO:
                    GotoNode gotoNode = (GotoNode)nodes.get(i);
                    printTab(level);
                    System.out.println("goto " + gotoNode.label + ";");
                    break;
                case AbstractNode.IF_NODE:
                    IfNode ifNode = (IfNode)nodes.get(i);
                    printTab(level);
                    System.out.print("if(");
                    printExp(ifNode.condition);
                    System.out.println("){");
                    visualiseAST(ifNode.ifBranch, level + 1);
                    printTab(level);
                    System.out.print("}");
                    if(ifNode.elseBranch != null && ifNode.elseBranch.size() > 0) {
                        System.out.println(" else {");
                        visualiseAST(ifNode.elseBranch, level + 1);
                        printTab(level);
                        System.out.println("}");
                    } else {
                        System.out.println();
                    }
                    break;
                case AbstractNode.WHILE_NODE:
                    WhileNode whileNode = (WhileNode)nodes.get(i);
                    printTab(level);
                    System.out.print("while(");
                    printExp(whileNode.condition);
                    System.out.println("){");
                    visualiseAST(whileNode.statements, level + 1);
                    printTab(level);
                    System.out.println("}");
                    break;
                case AbstractNode.DO_WHILE_NODE:
                    WhileNode doWhileNode = (WhileNode)nodes.get(i);
                    printTab(level);
                    System.out.println("do {");
                    visualiseAST(doWhileNode.statements, level + 1);
                    printTab(level);
                    System.out.print("} while(");
                    printExp(doWhileNode.condition);
                    System.out.println(");");
                    break;
                case AbstractNode.ASSIGN_NODE:
                    AssignNode assignNode = (AssignNode)nodes.get(i);
                    printTab(level);
                    printAssign(assignNode);
                    System.out.println(";");
                    break;
                case AbstractNode.DECLARE_VAR_NODE:
                    DeclareVarNode declareVarNode = (DeclareVarNode)nodes.get(i);
                    printTab(level);
                    printDeclaration(declareVarNode);
                    System.out.println(";");
                    break;
                case AbstractNode.RETURN_NODE:
                    ControlNode returnNode = (ControlNode)nodes.get(i);
                    printTab(level);
                    System.out.print("return ");
                    if(returnNode.exp != null) printExp(returnNode.exp);
                    System.out.println(";");
                    break;
                case AbstractNode.BREAK_NODE:
                    printTab(level);
                    System.out.println("break;");
                    break;
                case AbstractNode.CONTINUE_NODE:
                    printTab(level);
                    System.out.println("continue;");
                    break;
                case AbstractNode.FOR_NODE:
                    ForNode forNode = (ForNode)nodes.get(i);
                    printTab(level);
                    System.out.print("for(");
                    printDeclaration(forNode.declaration);
                    System.out.print("; ");
                    printExp(forNode.condition);
                    System.out.print("; ");
                    printAssign(forNode.assign);
                    System.out.println("){");
                    visualiseAST(forNode.statements, level + 1);
                    printTab(level);
                    System.out.println("}");
                    break;
                case AbstractNode.SWITCH_NODE:
                    SwitchNode switchNode = (SwitchNode)nodes.get(i);
                    printTab(level);
                    System.out.print("switch(");
                    printExp(switchNode.exp);
                    System.out.println("){");

                    int k = 0;
                    for(Map.Entry<String, ArrayList<StatementNode>> entry : switchNode.cases.entrySet()) {
                        printTab(level + 1);
                        System.out.println("case " + entry.getKey() + ":");
                        visualiseAST(entry.getValue(), level + 2);
                    }

                    printTab(level);
                    System.out.println("}");
                    break;
            }
        }
    }

    private void printAssign(AssignNode node) {
        printExp(node.lval);
        System.out.print(" = ");
        printExp(node.rval);
    }

    private void printDeclaration(DeclareVarNode node) {
        printType(node.type);
        System.out.print(node.var.name);
        if(node.expNode != null) {
            System.out.print(" = ");
            printExp(node.expNode);
        }
    }

    private void printExp(ExpNode expNode) {
        if(expNode instanceof ComplexOperand) {
            ComplexOperand node = (ComplexOperand)expNode;

            IdNode idNode = (IdNode)node.operands.get(0);
            System.out.print(idNode.name );

            for(int i = 1; i < node.operands.size(); i++) {
                if(node.operands.get(i) instanceof FieldNode) {
                    System.out.print("."+((FieldNode)node.operands.get(i)).propName);
                } else {
                    System.out.print("[");
                    printExp(node.operands.get(i));
                    System.out.print("]");
                }
            }
        } else if(expNode instanceof IdNode) {
            IdNode node = (IdNode)expNode;
            System.out.print(node.id.name);
        } else if(expNode instanceof ConstNode) {
            ConstNode node = (ConstNode)expNode;
            System.out.print(node.getValue());
        } else if(expNode instanceof NewNode) {
            NewNode node = (NewNode)expNode;
            System.out.print("new ");
            if(node.getTypeExp() instanceof ArrayType) {
                ArrayType type = (ArrayType)node.getTypeExp();

                if(type.structId != null) {
                    System.out.print(type.structId.name);
                } else {
                    System.out.print(type.primitiveType.value);
                }

                for(ExpNode size : node.sizes) {
                    System.out.print("[");
                    printExp(size);
                    System.out.print("]");
                }
            } else if(node.getTypeExp() instanceof StructType) {
                StructType type = (StructType)node.getTypeExp();

                System.out.println(type.structId.name + "()");
            }
        } else if(expNode instanceof FuncCallNode) {
            FuncCallNode node = (FuncCallNode)expNode;

            System.out.print(node.func.name + "(");
            for(int i = 0; i < node.args.size(); i++) {
                printExp(node.args.get(i));
                if(i != node.args.size() - 1) {
                    System.out.print(", ");
                }
            }
            System.out.print(")");
        } else {
            printExp(expNode.getFirstOperand());
            System.out.print(" " + expNode.getOperator() + " ");
            printExp(expNode.getSecondOperand());
        }
    }

    private void printType(TypeVar typeVar) {
        if(typeVar instanceof PrimitiveType) {
            System.out.print(((PrimitiveType) typeVar).type.value + " ");
        }

        if(typeVar instanceof ArrayType) {
            ArrayType type = (ArrayType)typeVar;

            if(type.structId != null) {
                System.out.print(type.structId.name);
            } else {
                System.out.print(type.primitiveType.value);
            }

            for(int i = 0; i < type.levels; i++) {
                System.out.print("[]");

                if(i == type.levels - 1) {
                    System.out.print(" ");
                }
            }
        }

        if(typeVar instanceof StructType) {
            System.out.print(((StructType)typeVar).structId.name + " ");
        }
    }

    private void printTab(int count) {
        for(int i = 0; i < count; i++) {
            System.out.print("\t");
        }
    }
}
