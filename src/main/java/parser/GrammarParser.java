package parser;

import beans.*;
import beans.id.FuncId;
import beans.id.IdRow;
import beans.id.StructId;
import beans.id.VarId;
import beans.node.*;
import beans.node.exp.*;
import beans.tac.Operand;
import beans.tac.Operation;
import beans.tac.TAC;
import beans.tac.tacExt.BinOperTAC;
import beans.type.ArrayType;
import beans.type.PrimitiveType;
import beans.type.StructType;
import beans.type.TypeVar;
import exception.GrammarRecognizeException;
import exception.SyntaxException;

import java.util.*;


/**
 * Created by antonskripacev on 23.03.17.
 */
public class GrammarParser {
    private List<Token> tokens;
    private List<AbstractNode> nodes = new ArrayList<AbstractNode>();
    private TableId tableId = new TableId(null);
    //costilina
    public static FuncId mainFunc;

    private int countLines;

    private DeclareFuncNode currentFunction;
    private int levelCycle = 0;
    private int maxOffsetInFunction = 0;

    public List<AbstractNode> parse(ArrayList<Token> tokens) throws GrammarRecognizeException, SyntaxException {
        if(tokens.size() == 0) {
            throw new GrammarRecognizeException("Nothing to parse");
        }

        this.tokens = tokens;
        countLines = tokens.get(tokens.size() - 1).getLine();

        createIOFunctions();

        while(!tokens.isEmpty()) {
            int initialSize = tokens.size();
            try {
                tryToRecognizeImport();
                tryToRecognizeStruct();
                tryToRecognizeFunction();
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new GrammarRecognizeException("unexpected eof, line " + getLineError());
            }

            if(initialSize == tokens.size()) {
                throw new GrammarRecognizeException("Grammar error, line " + tokens.get(0).getLine());
            }
        }

        boolean isMainExist = false;
        for(int i = 0; i < nodes.size(); ++i) {
            if(nodes.get(i) instanceof DeclareFuncNode) {
                DeclareFuncNode funcNode = (DeclareFuncNode)nodes.get(i);
                FuncId func = funcNode.getId();

                if(func.name.equals("main") && func.properties.size() == 0 && func.returnType instanceof  PrimitiveType && ((PrimitiveType)func.returnType).type == AllType.VOID) {
                    isMainExist = true;
                    break;
                }
            }
        }

        if(!isMainExist) {
            throw new SyntaxException("Syntax error: func main is expected(return type - void, count args - 0)");
        }

        return nodes;
    }

    private void createIOFunctions() {
        tableId.getIds().add(createIOFunction("printi", AllType.VOID, AllType.INT));
        tableId.getIds().add(createIOFunction("printf", AllType.VOID, AllType.FLOAT));
        tableId.getIds().add(createIOFunction("prints", AllType.VOID, AllType.STRING));
        tableId.getIds().add(createIOFunction("printb", AllType.VOID, AllType.BOOLEAN));
        tableId.getIds().add(createIOFunction("readi", AllType.INT, AllType.VOID));
        tableId.getIds().add(createIOFunction("readf", AllType.FLOAT, AllType.VOID));
        tableId.getIds().add(createIOFunction("reads", AllType.STRING, AllType.VOID));
        tableId.getIds().add(createIOFunction("readb", AllType.BOOLEAN, AllType.VOID));
    }

    public FuncId createIOFunction(String name, AllType returnType, AllType typeArg) {
        FuncId funcId = new FuncId();
        funcId.name = name;

        PrimitiveType lreturnType = new PrimitiveType();
        lreturnType.type = returnType;
        funcId.returnType = lreturnType;

        if(typeArg != AllType.VOID) {
            PrimitiveType ltypeArg = new PrimitiveType();
            ltypeArg.type = typeArg;
            funcId.properties.put(ltypeArg, funcId.name);
        }

        return funcId;
    }

    private boolean tryToRecognizeImport() throws GrammarRecognizeException, IndexOutOfBoundsException {
        if(tokens.get(0).getType() == TokenEnum.IMPORT) {
            if(tokens.size() < 3) {
                throw new GrammarRecognizeException("Grammar error: incorrect import, line " + tokens.get(0).getLine());
            }

            if(tokens.get(1).getType() != TokenEnum.STRINGCONST || tokens.get(1).getValue().equals("") || tokens.get(2).getType() != TokenEnum.SEMICOLON) {
                throw new GrammarRecognizeException("Grammar error: incorrect import, line " + tokens.get(0).getLine());
            } else {
                ImportNode node = new ImportNode(tokens.get(1).getValue(), AbstractNode.IMPORT);
                nodes.add(node);
                removeFirstTokenSeveralTimes(3);
            }
        }

        return !(tokens.size() == 0);
    }

    private boolean tryToRecognizeStruct() throws GrammarRecognizeException, SyntaxException {
        if(tokens.get(0).getType() != TokenEnum.STRUCT) return false;

        if (tokens.size() < 3)
            throw new GrammarRecognizeException("Grammar error: incorrect structure declaration, line " + getLineError());

        if (tokens.size() >= 3) {
            if (tokens.get(0).getType() == TokenEnum.STRUCT && tokens.get(1).getType() == TokenEnum.ID && tokens.get(2).getType() == TokenEnum.OPENBRACE) {

                if(tableId.lookupUserIdTable(tokens.get(1).getValue(), StructId.class) != null) {
                    throw new SyntaxException("Syntax error: struct " + tokens.get(1).getValue() + " is already defined, line " + tokens.get(1).getLine());
                }

                if (tokens.get(3).getType() == TokenEnum.CLOSEBRACE)
                    throw new SyntaxException("Syntax error: structure must have at least one field, line " + getLineError());

                LinkedHashMap<TypeVar, String> props = new LinkedHashMap<TypeVar, String>();

                StructId idRow = new StructId();
                idRow.name = tokens.get(1).getValue();
                idRow.properties = props;
                tableId.getIds().add(idRow);

                removeFirstTokenSeveralTimes(3);

                while (tokens.size() > 0 && tokens.get(0).getType() != TokenEnum.CLOSEBRACE) {
                    tryToRecognizeDeclarationWithoutAssign(props);
                }

                if (tokens.size() == 0 || tokens.get(0).getType() != TokenEnum.CLOSEBRACE) {
                    throw new GrammarRecognizeException("Grammar error: incorrect structure declaration, line " + getLineError());
                } else {
                    DeclareStructNode node = new DeclareStructNode(idRow, AbstractNode.STRUCT);
                    nodes.add(node);
                    removeFirstTokenSeveralTimes(1);
                }
            }
        }

        return !(tokens.size() == 0);
    }

    //COMPLETE
    private boolean tryToRecognizeFunction() throws SyntaxException, GrammarRecognizeException{
        if(tokens.size() == 0) return false;

        if(tokens.get(0).getType() != TokenEnum.ID && AllType.getTypeByString(tokens.get(0).getType().getValue()) == null) return false;

        TypeVar type = tryToRecognizeType(true);

        if(tokens.get(0).getType() == TokenEnum.ID && tokens.get(1).getType() == TokenEnum.OPENPAREN ) {
            for(IdRow id : tableId.getIds()) {
                if(id.name.equals(tokens.get(0).getValue()) && (id instanceof FuncId)) {
                    throw new SyntaxException("Syntax error: function " + tokens.get(0).getValue() + " is already defined, line " + tokens.get(0).getLine());
                }
            }

            LinkedHashMap<TypeVar, String> props = new LinkedHashMap<TypeVar, String>();
            LinkedHashMap<String, IdRow> propIds = new LinkedHashMap<String, IdRow>();
            FuncId row = new FuncId();
            row.name = tokens.get(0).getValue();
            row.returnType = type;
            row.properties = props;
            row.propIds = propIds;

            if(row.name.equals("main")) {
                mainFunc = row;
            }

            TranslatorTAC.createFuncLabelTAC(row.name);

            removeFirstTokenSeveralTimes(2);

            if(tokens.get(0).getType() != TokenEnum.CLOSEPAREN) {
                while(tokens.size() > 0 && tokens.get(0).getType() != TokenEnum.CLOSEPAREN) {
                    tryToRecognizeArgumentInList(props, propIds);
                }

                if(tokens.get(0).getType() == TokenEnum.CLOSEPAREN) {
                    removeFirstTokenSeveralTimes(1);
                } else {
                    throw new GrammarRecognizeException("Grammar error: ) is expected, line " + getLineError());
                }
            } else {
                removeFirstTokenSeveralTimes(1);
            }

            DeclareFuncNode node = new DeclareFuncNode(row, AbstractNode.FUNC);
            currentFunction = node;

            if(tokens.get(0).getType() == TokenEnum.OPENBRACE) {
                removeFirstTokenSeveralTimes(1);

                tableId = new TableId(tableId);

                //first item is return address,($sp+0, $sp+4 - first var)
                Integer offset = TranslatorTAC.curOffsetFromStartStack.push(4);

                for(Map.Entry<TypeVar, String> var : currentFunction.getId().properties.entrySet()) {
                    VarId rowVar = new VarId();
                    rowVar.isInitialised = true;
                    rowVar.name = var.getValue();
                    rowVar.type = var.getKey();
                    tableId.getIds().add(rowVar);
                    currentFunction.getId().propIds.put(var.getValue(), rowVar);

                    rowVar.offset = offset;
                    offset += 4;
                }

                maxOffsetInFunction = offset;

                tryToRecognizeStatementList(node.getStatementNodes());

                if((row.returnType instanceof PrimitiveType && ((PrimitiveType) row.returnType).type != AllType.VOID) || row.returnType instanceof ArrayType || row.returnType instanceof StructType) {
                    if(!(node.getStatementNodes().get(node.getStatementNodes().size() - 1) instanceof ControlNode) || (((ControlNode)node.getStatementNodes().get(node.getStatementNodes().size() - 1)).exp == null)) {
                        throw new SyntaxException("Syntas error: last sentence in non void func must be return in func " + row.name + " line" + getLineError());
                    }
                } else {
                    TranslatorTAC.createReturnStatement(null, currentFunction.getId());
                }

                tableId.isHidden = true;
                tableId = tableId.getParent();

                if(tokens.get(0).getType() == TokenEnum.CLOSEBRACE) {
                    removeFirstTokenSeveralTimes(1);
                    //TODO if recursion is allowed, translate it several lines upper
                    tableId.getIds().add(row);
                } else {
                    throw new GrammarRecognizeException("Grammar error: } is expected, line: " + getLineError());
                }

                row.sumOffset = maxOffsetInFunction;
                TranslatorTAC.curOffsetFromStartStack.pop();
                maxOffsetInFunction = 0;

                nodes.add(node);
            } else {
                throw new GrammarRecognizeException("Grammar error: { is expected, line: " + getLineError());
            }
        } else {
            throw new GrammarRecognizeException("Grammar error: incorrect function declaration, line " + getLineError());
        }

        return !(tokens.size() == 0);
    }

    private int max(Stack<Integer> stack) {
        int max = Integer.MIN_VALUE;
        for(Integer i : stack) {
            if(i > max) {
                max = i;
            }
        }

        return max;
    }

    private boolean tryToRecognizeStatementList(List<StatementNode> statements) throws GrammarRecognizeException, SyntaxException {

        while(tokens.size() > 0 && tokens.get(0).getType() != TokenEnum.CLOSEBRACE) {
            switch (tokens.get(0).getType()) {
                case DO:
                    tryToRecognizeDoWhile(statements);
                    break;
                case WHILE:
                    tryToRecognizeWhile(statements);
                    break;
                case FOR:
                    tryToRecognizeFor(statements);
                    break;
                case IF:
                    tryToRecognizeIf(statements);
                    break;
                case SWITCH:
                    tryToRecognizeSwitch(statements);
                    break;
                case ID:
                    if(
                            tokens.get(1).getType() == TokenEnum.ASSIGN // a = exp
                            || tokens.get(1).getType() == TokenEnum.DOT // a.b = exp
                            || tokens.get(1).getType() == TokenEnum.OPENSQUARE && tokens.get(2).getType() != TokenEnum.CLOSESQUARE//a[exp], not a[]! - it is declaration
                     )
                    {
                        tryToRecognizeAssign(statements, false);
                    }
                    else if (tokens.get(1).getType() == TokenEnum.OPENPAREN)
                    {// a();
                        tryToRecognizeFunctionCall(statements);
                    }
                    else if(tokens.get(1).getType() == TokenEnum.ID)
                    {// it might be only declaration of struct! (T a = ....
                        tryToRecognizeDeclaration(statements, false);
                    } else if(//T[][][]....
                              tokens.get(1).getType() == TokenEnum.OPENSQUARE && tokens.get(2).getType() == TokenEnum.CLOSESQUARE
                            ) {
                        tryToRecognizeDeclaration(statements, false);
                    } else {
                        throw  new GrammarRecognizeException("Grammar error, line " + getLineError());
                    }
                    break;
                case INT:
                case FLOAT:
                case STRING:
                case BOOLEAN:
                    tryToRecognizeDeclaration(statements, false);
                    break;
                case GOTO:
                    tryToRecognizeGoto(statements);
                    break;
                case SHARP:
                    tryToRecognizeLabel(statements);
                    break;
                case CLOSEBRACE:
                    return !(tokens.size() == 0);
                case CASE:
                    return !(tokens.size() == 0);
                case RETURN:
                    tryToRecognizeReturn(statements);
                    break;
                case CONTINUE:
                    ControlNode node = new ControlNode(AbstractNode.CONTINUE_NODE);

                    if(tokens.get(1).getType() != TokenEnum.SEMICOLON) {
                        throw new GrammarRecognizeException("Grammar error: ; is expected, line " + getLineError());
                    }

                    if(levelCycle == 0) {
                       throw new SyntaxException("Syntax error: continue operator is only available in cycle, line " + getLineError());
                    }

                    statements.add(node);

                    TranslatorTAC.createGoTOStatement(TranslatorTAC.curCycle + "_start");

                    removeFirstTokenSeveralTimes(2);
                    break;
                case BREAK:
                    ControlNode breakNode = new ControlNode(AbstractNode.BREAK_NODE);

                    if(tokens.get(1).getType() != TokenEnum.SEMICOLON) {
                        throw new GrammarRecognizeException("Grammar error: ; is expected, line " + getLineError());
                    }

                    if(levelCycle == 0) {
                        throw new SyntaxException("Syntax error: break operator is only available in cycle, line " + getLineError());
                    }

                    statements.add(breakNode);

                    TranslatorTAC.createGoTOStatement(TranslatorTAC.curCycle + "_end");

                    removeFirstTokenSeveralTimes(2);
                    break;
                default:
                    throw new GrammarRecognizeException("Grammar error, line " + getLineError());
            }
        }

        return !(tokens.size() == 0);
    }

    //COMPLETE
    private void tryToRecognizeReturn(List<StatementNode> statements) throws GrammarRecognizeException, SyntaxException {
        ControlNode returnNode = new ControlNode(AbstractNode.RETURN_NODE);

        tokens.remove(0);

        int index = -1;
        for(int i = 0; i < tokens.size(); ++i) {
            if(tokens.get(i).getType() == TokenEnum.SEMICOLON) {
                index = i;
                break;
            }
        }

        if(index == -1) {
            throw new GrammarRecognizeException("Grammar error: ; is expected, line " + getLineError());
        }

        if(index == 0) {//return;
            if (!(currentFunction.getId().returnType instanceof PrimitiveType) || (((PrimitiveType) currentFunction.getId().returnType).type != AllType.VOID)) {
                throw new GrammarRecognizeException("Grammar error: incorrect return type, line " + getLineError());
            } else {
                TranslatorTAC.createReturnStatement(null, currentFunction.getId());

                statements.add(returnNode);
                tokens.remove(0);
                return;
            }
        }

        returnNode.exp = tryToRecognizeExpression(tokens.subList(0, index));

        if(!isTypesEqual(returnNode.exp.getTypeExp(), currentFunction.getId().returnType)) {
            throw new GrammarRecognizeException("Grammar error: incorrect return type, line " + getLineError());
        }

        TranslatorTAC.createReturnStatement(returnNode.exp, currentFunction.getId());

        tokens.remove(0);

        statements.add(returnNode);
    }

    //COMPLETE
    private void tryToRecognizeFor(List<StatementNode> statements) throws GrammarRecognizeException, SyntaxException {
        ForNode node = new ForNode(AbstractNode.FOR_NODE);
        tokens.remove(0);

        if(tokens.get(0).getType() != TokenEnum.OPENPAREN) {
            throw new GrammarRecognizeException("Grammar error: ( is expected, line " + getLineError());
        }

        TranslatorTAC.curOffsetFromStartStack.push(TranslatorTAC.curOffsetFromStartStack.peek().intValue());
        tokens.remove(0);
        List<StatementNode> list = new ArrayList<StatementNode>();
        tryToRecognizeDeclaration(list, true);
        node.declaration = (DeclareVarNode)list.get(0);

        int index = -1;
        for(int i = 0; i < tokens.size(); i++) {
            if(tokens.get(i).getType() == TokenEnum.SEMICOLON) {
                index = i;
                break;
            }
        }

        if(index == -1) {
            throw new GrammarRecognizeException("Grammar error: ; is expected, line " + getLineError());
        }

        node.condition = tryToRecognizeExpression(tokens.subList(0, index));

        if(!(node.condition.getTypeExp() instanceof PrimitiveType) || (((PrimitiveType) node.condition.getTypeExp()).type != AllType.BOOLEAN)) {
            throw new SyntaxException("Syntax error: expression in for condition must be boolean type, line " + getLineError());
        }

        tokens.remove(0);

        list = new ArrayList<StatementNode>();
        tryToRecognizeAssign(list, true);
        node.assign = (AssignNode) list.get(0);

        if(tokens.get(0).getType() != TokenEnum.OPENBRACE) {
            throw new GrammarRecognizeException("Grammar error: { is expected, line " + getLineError());
        }

        tokens.remove(0);

        tableId = new TableId(tableId);

        levelCycle++;

        int labelFor = TranslatorTAC.curNumLabel++;

        TranslatorTAC.createStartFor(node, labelFor);
        tryToRecognizeStatementList(node.statements);
        TranslatorTAC.createEndFor(node, labelFor);

        if(TranslatorTAC.curOffsetFromStartStack.peek() > maxOffsetInFunction) {
            maxOffsetInFunction = TranslatorTAC.curOffsetFromStartStack.pop().intValue();
        } else {
            TranslatorTAC.curOffsetFromStartStack.pop().intValue();
        }

        levelCycle--;

        tableId.isHidden = true;
        tableId = tableId.getParent();

        if(tokens.get(0).getType() != TokenEnum.CLOSEBRACE) {
            throw new GrammarRecognizeException("Grammar error: } is expected, line " + getLineError());
        }

        tokens.remove(0);

        statements.add(node);
    }

    //COMPLETE
    private void tryToRecognizeWhile(List<StatementNode> statements) throws SyntaxException, GrammarRecognizeException {
        WhileNode node = new WhileNode(AbstractNode.WHILE_NODE);
        tokens.remove(0);

        if(tokens.get(0).getType() != TokenEnum.OPENPAREN) {
            throw new GrammarRecognizeException("Grammar error: ( is expected, line " + getLineError());
        }

        tokens.remove(0);

        int index = -1;
        int countParen = 1;
        for(int i = 0; i < tokens.size(); ++i) {
            if(tokens.get(i).getType() == TokenEnum.OPENPAREN) {
                countParen++;
            }

            if(tokens.get(i).getType() == TokenEnum.CLOSEPAREN) {
                countParen--;
            }

            if(countParen == 0) {
                index = i;
                break;
            }
        }

        if(index == -1) {
            throw new GrammarRecognizeException("Grammar error: ) is expected, line " + getLineError());
        }

        ExpNode exp = tryToRecognizeExpression(tokens.subList(0, index));

        if(!(exp.getTypeExp() instanceof PrimitiveType) || (((PrimitiveType) exp.getTypeExp()).type != AllType.BOOLEAN)) {
            throw new SyntaxException("Syntax error: expression in while condition must be boolean type, line " + getLineError());
        }

        node.condition = exp;

        tokens.remove(0);

        if(tokens.get(0).getType() != TokenEnum.OPENBRACE) {
            throw new GrammarRecognizeException("Grammar error: { is expected, line " + getLineError());
        }

        tokens.remove(0);

        tableId = new TableId(tableId);

        levelCycle++;

        TranslatorTAC.curOffsetFromStartStack.push(TranslatorTAC.curOffsetFromStartStack.peek().intValue());

        int labelFor = TranslatorTAC.curNumLabel++;
        TranslatorTAC.createStartWhile(node, labelFor);
        tryToRecognizeStatementList(node.statements);
        TranslatorTAC.createEndWhile(labelFor);

        if(TranslatorTAC.curOffsetFromStartStack.peek() > maxOffsetInFunction) {
            maxOffsetInFunction = TranslatorTAC.curOffsetFromStartStack.pop().intValue();
        } else {
            TranslatorTAC.curOffsetFromStartStack.pop().intValue();
        }

        levelCycle--;

        tableId.isHidden = true;
        tableId = tableId.getParent();

        if(tokens.get(0).getType() != TokenEnum.CLOSEBRACE) {
            throw new GrammarRecognizeException("Grammar error: } is expected, line " + getLineError());
        }

        tokens.remove(0);

        statements.add(node);
    }

    //COMPLETE
    private void tryToRecognizeDoWhile(List<StatementNode> statements) throws SyntaxException, GrammarRecognizeException {
        WhileNode node = new WhileNode(AbstractNode.DO_WHILE_NODE);
        tokens.remove(0);

        if(tokens.get(0).getType() != TokenEnum.OPENBRACE) {
            throw new GrammarRecognizeException("Grammar error: { is expected, line " + getLineError());
        }

        tokens.remove(0);

        tableId = new TableId(tableId);

        levelCycle++;

        TranslatorTAC.curOffsetFromStartStack.push(TranslatorTAC.curOffsetFromStartStack.peek().intValue());

        int labelDoWhile = TranslatorTAC.curNumLabel++;
        TranslatorTAC.createStartDoWhile(labelDoWhile);
        tryToRecognizeStatementList(node.statements);

        if(TranslatorTAC.curOffsetFromStartStack.peek() > maxOffsetInFunction) {
            maxOffsetInFunction = TranslatorTAC.curOffsetFromStartStack.pop().intValue();
        } else {
            TranslatorTAC.curOffsetFromStartStack.pop().intValue();
        }

        levelCycle--;

        tableId.isHidden = true;
        tableId = tableId.getParent();

        if(tokens.get(0).getType() != TokenEnum.CLOSEBRACE) {
            throw new GrammarRecognizeException("Grammar error: } is expected, line " + getLineError());
        }

        tokens.remove(0);

        if(tokens.get(0).getType() != TokenEnum.WHILE) {
            throw new GrammarRecognizeException("Grammar error: while is expected, line " + getLineError());
        }

        tokens.remove(0);

        if(tokens.get(0).getType() != TokenEnum.OPENPAREN) {
            throw new GrammarRecognizeException("Grammar error: ( is expected, line " + getLineError());
        }

        tokens.remove(0);

        int index = -1;
        int countParen = 1;
        for(int i = 0; i < tokens.size(); ++i) {
            if(tokens.get(i).getType() == TokenEnum.OPENPAREN) {
                countParen++;
            }

            if(tokens.get(i).getType() == TokenEnum.CLOSEPAREN) {
                countParen--;
            }

            if(countParen == 0) {
                index = i;
                break;
            }
        }

        if(index == -1) {
            throw new GrammarRecognizeException("Grammar error: ) is expected, line " + getLineError());
        }

        ExpNode exp = tryToRecognizeExpression(tokens.subList(0, index));

        if(!(exp.getTypeExp() instanceof PrimitiveType) || (((PrimitiveType) exp.getTypeExp()).type != AllType.BOOLEAN)) {
            throw new SyntaxException("Syntax error: expression in while condition must be boolean type, line " + getLineError());
        }

        node.condition = exp;
        TranslatorTAC.createEndDoWhile(node, labelDoWhile);

        tokens.remove(0);

        if(tokens.get(0).getType() != TokenEnum.SEMICOLON) {
            throw new GrammarRecognizeException("Grammar error: ; is expected, line " + getLineError());
        }

        tokens.remove(0);

        statements.add(node);
    }

    //COMPLETE
    private void tryToRecognizeIf(List<StatementNode> statements) throws SyntaxException, GrammarRecognizeException {
        int labelIf = TranslatorTAC.curNumLabel++;

        IfNode node = new IfNode(AbstractNode.IF_NODE);
        tokens.remove(0);

        if(tokens.get(0).getType() != TokenEnum.OPENPAREN) {
            throw new GrammarRecognizeException("Grammar error: ( is expected, line " + getLineError());
        }

        tokens.remove(0);

        int index = -1;
        int countParen = 1;
        for(int i = 0; i < tokens.size(); ++i) {
            if(tokens.get(i).getType() == TokenEnum.OPENPAREN) {
                countParen++;
            }

            if(tokens.get(i).getType() == TokenEnum.CLOSEPAREN) {
                countParen--;
            }

            if(countParen == 0) {
                index = i;
                break;
            }
        }

        if(index == -1) {
            throw new GrammarRecognizeException("Grammar error: ) is expected, line " + getLineError());
        }

        ExpNode exp = tryToRecognizeExpression(tokens.subList(0, index));

        if(!(exp.getTypeExp() instanceof PrimitiveType) || (((PrimitiveType) exp.getTypeExp()).type != AllType.BOOLEAN)) {
            throw new SyntaxException("Syntax error: expression in if condition must be boolean type, line " + getLineError());
        }

        node.condition = exp;

        tokens.remove(0);

        if(tokens.get(0).getType() != TokenEnum.OPENBRACE) {
            throw new GrammarRecognizeException("Grammar error: { is expected, line " + getLineError());
        }

        tokens.remove(0);

        tableId = new TableId(tableId);

        TranslatorTAC.curOffsetFromStartStack.push(TranslatorTAC.curOffsetFromStartStack.peek().intValue());
        TranslatorTAC.createStartIf(node, labelIf);
        tryToRecognizeStatementList(node.ifBranch);
        TranslatorTAC.createEndIf(labelIf);

        if(TranslatorTAC.curOffsetFromStartStack.peek() > maxOffsetInFunction) {
            maxOffsetInFunction = TranslatorTAC.curOffsetFromStartStack.pop().intValue();
        } else {
            TranslatorTAC.curOffsetFromStartStack.pop();
        }

        tableId.isHidden = true;
        tableId = tableId.getParent();

        if(tokens.get(0).getType() != TokenEnum.CLOSEBRACE) {
            throw new GrammarRecognizeException("Grammar error: } is expected, line " + getLineError());
        }

        tokens.remove(0);

        if(tokens.get(0).getType() == TokenEnum.ELSE) {
            tokens.remove(0);

            if(tokens.get(0).getType() != TokenEnum.OPENBRACE) {
                throw new GrammarRecognizeException("Grammar error: { is expected, line " + getLineError());
            }

            tokens.remove(0);

            tableId = new TableId(tableId);

            TranslatorTAC.curOffsetFromStartStack.push(TranslatorTAC.curOffsetFromStartStack.peek().intValue());
            tryToRecognizeStatementList(node.elseBranch);

            if(TranslatorTAC.curOffsetFromStartStack.peek() > maxOffsetInFunction) {
                maxOffsetInFunction = TranslatorTAC.curOffsetFromStartStack.pop().intValue();
            } else {
                TranslatorTAC.curOffsetFromStartStack.pop().intValue();
            }

            tableId.isHidden = true;
            tableId = tableId.getParent();

            if(tokens.get(0).getType() != TokenEnum.CLOSEBRACE) {
                throw new GrammarRecognizeException("Grammar error: } is expected, line " + getLineError());
            }

            tokens.remove(0);
        }
        TranslatorTAC.createEndIfElse(labelIf);

        statements.add(node);
    }

    //COMPLETE
    private void tryToRecognizeFunctionCall(List<StatementNode> statements) throws SyntaxException, GrammarRecognizeException {
        String id = tokens.get(0).getValue();
        removeFirstTokenSeveralTimes(2);

        FuncId func = (FuncId)TableId.lookupUserIdTableRecursive(tableId, id, FuncId.class);

        if(func == null) {
            throw new SyntaxException("Syntax error: function " + id + " doesn't exist");
        }

        if(!(func.returnType instanceof PrimitiveType) || ((PrimitiveType) func.returnType).type != AllType.VOID) {
            throw new SyntaxException("Syntax error: return type void is expected, line " + getLineError());
        }

        FuncCallStmNode node = new FuncCallStmNode(AbstractNode.FUNC_CALL_STM_NODE);
        node.func = func;

        while(tokens.size() > 0 && tokens.get(0).getType() != TokenEnum.CLOSEPAREN) {
            int index = -1;
            int countParen = 1;

            for(int i = 0; i < tokens.size(); i++) {
                if(tokens.get(i).getType() == TokenEnum.OPENPAREN) {
                    countParen++;
                }

                if(tokens.get(i).getType() == TokenEnum.CLOSEPAREN) {
                    countParen--;
                }

                if(countParen == 0 && tokens.get(i).getType() == TokenEnum.CLOSEPAREN) {
                    index = i;
                    break;
                }

                if(countParen == 1 && tokens.get(i).getType() == TokenEnum.COMMA) {
                    index = i;
                    break;
                }
            }

            if(index == -1) {
                throw new GrammarRecognizeException("Grammar error: invalid call function, line " + getLineError());
            }

            ExpNode exp = tryToRecognizeExpression(tokens.subList(0, index));
            node.args.add(exp);

            if(tokens.get(0).getType() == TokenEnum.COMMA) {
                tokens.remove(0);
            }
        }

        if(node.args.size() != func.properties.size()) {
            throw new SyntaxException("Syntax error: incorrect count arguments in function call, line " + getLineError());
        }

        List<TypeVar> args = new ArrayList<TypeVar>();
        for(Map.Entry<TypeVar, String> arg : func.properties.entrySet()) {
            args.add(arg.getKey());
        }

        for(int i = 0; i < node.args.size(); ++i) {
            if(!isTypesEqual(node.args.get(i).getTypeExp(), args.get(i))) {
                throw new SyntaxException("Syntax error: incorrect type argument in function call, line " + getLineError());
            }
        }

        if(tokens.size() == 0 || tokens.get(0).getType() != TokenEnum.CLOSEPAREN) {
            throw new GrammarRecognizeException("Grammar error: ) is expected");
        }

        tokens.remove(0);

        if(tokens.size() == 0 || tokens.get(0).getType() != TokenEnum.SEMICOLON) {
            throw new GrammarRecognizeException("Grammar error: ; is expected");
        }

        tokens.remove(0);

        statements.add(node);

        TranslatorTAC.createVoidFuncCall(node);
    }

    //COMPLETE
    private void tryToRecognizeSwitch(List<StatementNode> statements) throws GrammarRecognizeException, SyntaxException {

        if(tokens.get(1).getType() != TokenEnum.OPENPAREN) {
            throw new GrammarRecognizeException("Grammar error: ( is expected");
        }

        int numLabel = TranslatorTAC.curNumLabel++;

        removeFirstTokenSeveralTimes(2);

        int index = -1;
        int countParen = 1;
        for(int i = 0; i < tokens.size(); i++) {
            if(tokens.get(i).getType() == TokenEnum.OPENPAREN) {
                countParen++;
            }

            if(tokens.get(i).getType() == TokenEnum.CLOSEPAREN) {
                countParen--;
            }

            if(countParen == 0) {
                index = i;
                break;
            }
        }


        if(index == -1) {
            throw new GrammarRecognizeException("Grammar error: ) is expected, line " + tokens.get(0).getLine());
        }

        ExpNode operand = tryToRecognizeExpression(tokens.subList(0, index));

        removeFirstTokenSeveralTimes(1);

        if(tokens.get(0).getType() != TokenEnum.OPENBRACE) {
            throw new GrammarRecognizeException("Grammar error: { is expected, line " + tokens.get(0).getLine());
        }

        removeFirstTokenSeveralTimes(1);

        SwitchNode node = new SwitchNode(AbstractNode.SWITCH_NODE);
        node.exp = operand;

        TranslatorTAC.createSequenceTacForExp(node.exp, 0);

        int currentCase = 0;
        while(tokens.get(0).getType() == TokenEnum.CASE) {
            removeFirstTokenSeveralTimes(1);

            if(tokens.get(0).getType() == TokenEnum.INTCONST || tokens.get(0).getType() == TokenEnum.FLOATCONST) {
                if(!(operand.getTypeExp() instanceof PrimitiveType) || ((PrimitiveType)operand.getTypeExp()).type != AllType.getTypeByTokenEnum(tokens.get(0).getType())) {
                    throw new SyntaxException("Syntax error: incomptable type in case, line " + tokens.get(0).getLine());
                }

                String label = tokens.get(0).getValue();
                node.cases.put(label, new ArrayList<StatementNode>());
                ArrayList<StatementNode> statementsCase = node.cases.get(label);
                removeFirstTokenSeveralTimes(1);

                if(tokens.get(0).getType() != TokenEnum.COLON) {
                    throw new GrammarRecognizeException("Grammar error: <:> is expected, line " + tokens.get(0).getLine());
                }
                removeFirstTokenSeveralTimes(1);

                tableId = new TableId(tableId);

                BinOperTAC tac = new BinOperTAC();
                Operand op1 = new Operand();
                Operand op2 = new Operand();
                op2.constant = label;
                op1.tempVar = "$t0";
                Operand res = new Operand();
                res.tempVar = "$t1";
                Operation operation = Operation.NOTEQUAL;

                tac.fOp = op1;
                tac.sOp = op2;
                tac.result = res;
                tac.operation = operation;
                TranslatorTAC.tacs.add(tac);

                TranslatorTAC.createIfTrue("$t1", "$L" + Integer.toString(numLabel) + "_" + Integer.toString(currentCase) + ":");
                TranslatorTAC.curOffsetFromStartStack.push(TranslatorTAC.curOffsetFromStartStack.peek().intValue());
                tryToRecognizeStatementList(statementsCase);

                if(TranslatorTAC.curOffsetFromStartStack.peek() > maxOffsetInFunction) {
                    maxOffsetInFunction = TranslatorTAC.curOffsetFromStartStack.pop().intValue();
                } else {
                    TranslatorTAC.curOffsetFromStartStack.pop().intValue();
                }

                TranslatorTAC.createGoTOStatement("$L" + Integer.toString(numLabel) + "_end:");
                TranslatorTAC.createLabelStatement("$L" + Integer.toString(numLabel) + "_" + Integer.toString(currentCase++) + ":");

                tableId.isHidden = true;
                tableId = tableId.getParent();
            } else {
                throw new SyntaxException("Syntax error: const is expected in case, line " + getLineError());
            }
        }

        TranslatorTAC.createLabelStatement("$L" + Integer.toString(numLabel) + "_end:");

        if(node.cases.size() == 0) {
            throw new SyntaxException("Syntax error: at least one case is expected, line " + getLineError());
        }

        if(tokens.get(0).getType() == TokenEnum.CLOSEBRACE) {
            removeFirstTokenSeveralTimes(1);
        } else {
            throw new GrammarRecognizeException("Grammar error: } is expected, line: " + getLineError());
        }

        statements.add(node);
    }

    //COMPLETE
    private void tryToRecognizeAssign(List<StatementNode> statements, boolean isForFor) throws GrammarRecognizeException, SyntaxException {
        int index = -1;
        for(int i = 0; i < tokens.size(); i++) {
            if(tokens.get(i).getType() == TokenEnum.ASSIGN) {
                index = i;
                break;
            }
        }

        if(index == -1) {
            throw new GrammarRecognizeException("Syntax error: = is expected, line " + tokens.get(0).getLine());
        }

        ComplexOperand op = (ComplexOperand)tryToRecognizeComplexOperand(true, tokens.subList(0, index));
        tokens.remove(0);

        index = -1;
        int countParen = 1;
        for(int i = 0; i < tokens.size(); i++) {
            if(!isForFor) {
                if (tokens.get(i).getType() == TokenEnum.SEMICOLON) {
                    index = i;
                    break;
                }
            } else {
                if (tokens.get(i).getType() == TokenEnum.OPENPAREN) {
                    countParen++;
                }

                if (tokens.get(i).getType() == TokenEnum.CLOSEPAREN) {
                    countParen--;
                }

                if(countParen == 0) {
                    index = i;
                    break;
                }
            }
        }

        if(index == -1) {
            if(isForFor) {
                throw new GrammarRecognizeException("Syntax error: ) is expected, line " + tokens.get(0).getLine());
            }

            throw new GrammarRecognizeException("Syntax error: ; is expected, line " + tokens.get(0).getLine());
        }

        ExpNode exp = tryToRecognizeExpression(tokens.subList(0, index));


        if(!isTypesEqual(exp.getTypeExp(), op.getTypeExp())) {
            throw new SyntaxException("Syntax error: incomptable types, line " + getLineError());
        }

        tokens.remove(0);

        AssignNode node = new AssignNode(AbstractNode.ASSIGN_NODE);
        node.lval = op;
        node.rval = exp;

        if(!isForFor) TranslatorTAC.createAssignStatement(op, exp);

        statements.add(node);
    }

    //COMPLETE
    private ExpNode tryToRecognizeExpression(List<Token> list) throws GrammarRecognizeException, SyntaxException {
        List<ExpNode> expressions = new ArrayList<ExpNode>();
        List<String> operators = new ArrayList<String>();
        String operator;

        expressions.add(tryToRecognizeOperand(list));

        if(expressions.get(0) instanceof NewNode) {
            return expressions.get(0);
        }

        do {
            operator = tryToRecognizeOperator(list);
            if(!operator.equals("empty")) {
                operators.add(operator);
                for(int i = 0; i < operator.length(); ++i) {
                    list.remove(0);
                }

                expressions.add(tryToRecognizeOperand(list));
            }
        } while(!operator.equals("empty"));


        if(expressions.size() == 1) {
            return expressions.get(0);
        }

        return constructOneExpression(expressions, operators);
    }

    //COMPLETE
    private String tryToRecognizeOperator(List<Token> list) throws GrammarRecognizeException {
        if(list.size() == 0) {
            return "empty";
        }

        switch(list.get(0).getType()) {
            case PLUS:
            case MINUS:
            case MULTIPLY:
            case DIVIDE:
            case AND:
            case OR:
                return list.get(0).getType().getValue();
            case NOT:
                if(list.get(1).getType() != TokenEnum.ASSIGN) {
                    throw new GrammarRecognizeException("Grammar error: incorrect operator, line " + getLineError());
                }

                return list.get(0).getType().getValue() + list.get(1).getType().getValue(); // !=
            case ASSIGN:
                if(list.get(1).getType() != TokenEnum.ASSIGN) {
                    throw new GrammarRecognizeException("Grammar error: incorrect operator, line " + getLineError());
                }

                return list.get(0).getType().getValue() + list.get(1).getType().getValue(); // ==
            case GREATER:
                if(list.get(1).getType() != TokenEnum.ASSIGN) {
                    return list.get(0).getType().getValue(); // >
                }

                return list.get(0).getType().getValue() + list.get(1).getType().getValue(); // >=
            case LESS:
                if(list.get(1).getType() != TokenEnum.ASSIGN) {
                    return list.get(0).getType().getValue(); // <
                }

                return list.get(0).getType().getValue() + list.get(1).getType().getValue(); // <=
            default:
                throw new GrammarRecognizeException("Grammar error: operator is expected, line " + getLineError());
        }
    }

    //COMPLETE
    private ExpNode constructOneExpression(List<ExpNode> expressions, List<String> operators) throws SyntaxException {
        for(int i = 0; i < operators.size(); i++) {
            if(operators.get(i).equals("*") || operators.get(i).equals("/")) {
                ExpNode exp1 = expressions.get(i);
                ExpNode exp2 = expressions.get(i + 1);

                if(!isTypesEqual(exp1.getTypeExp(), exp2.getTypeExp())) {
                    throw new SyntaxException("Syntax error: incomptaible types, line " + getLineError());
                }

                if(!(exp1.getTypeExp() instanceof PrimitiveType) || ((PrimitiveType)exp1.getTypeExp()).type != AllType.INT && ((PrimitiveType)exp1.getTypeExp()).type != AllType.FLOAT) {
                    throw new SyntaxException("Syntax error: incomptaible operands, line " + getLineError());
                }

                ExpNode exp = new ExpNode(AbstractNode.EXPRESSION);
                exp.setFirstOperand(exp1);
                exp.setOperator(operators.get(i));
                exp.setSecondOperand(exp2);
                exp.setTypeExp(exp1.getTypeExp());
                expressions.remove(i);
                expressions.remove(i);
                operators.remove(i);
                expressions.add(i, exp);
                i--;
            }
        }

        for(int i = 0; i < operators.size(); i++) {
            if(operators.get(i).equals("+") || operators.get(i).equals("-")) {
                ExpNode exp1 = expressions.get(i);
                ExpNode exp2 = expressions.get(i + 1);

                if(!isTypesEqual(exp1.getTypeExp(), exp2.getTypeExp())) {
                    throw new SyntaxException("Syntax error: incomptaible types, line " + getLineError());
                }

                if(operators.get(i).equals("+") && (!(exp1.getTypeExp() instanceof PrimitiveType) || ((PrimitiveType)exp1.getTypeExp()).type != AllType.STRING
                        && ((PrimitiveType)exp1.getTypeExp()).type != AllType.INT && ((PrimitiveType)exp1.getTypeExp()).type != AllType.FLOAT)) {
                    throw new SyntaxException("Syntax error: incomptaible operands, line " + getLineError());
                }

                if(operators.get(i).equals("-") && (!(exp1.getTypeExp() instanceof PrimitiveType) || (((PrimitiveType)exp1.getTypeExp()).type != AllType.INT
                        && ((PrimitiveType)exp1.getTypeExp()).type != AllType.FLOAT))) {
                    throw new SyntaxException("Syntax error: incomptaible operands, line " + getLineError());
                }


                ExpNode exp = new ExpNode(AbstractNode.EXPRESSION);
                exp.setFirstOperand(exp1);
                exp.setOperator(operators.get(i));
                exp.setSecondOperand(exp2);
                exp.setTypeExp(exp1.getTypeExp());
                expressions.remove(i);
                expressions.remove(i);
                operators.remove(i);
                expressions.add(i, exp);
                i--;
            }
        }

        for(int i = 0; i < operators.size(); i++) {
            if(operators.get(i).equals("&")) {
                ExpNode exp1 = expressions.get(i);
                ExpNode exp2 = expressions.get(i + 1);

                if(!isTypesEqual(exp1.getTypeExp(), exp2.getTypeExp())) {
                    throw new SyntaxException("Syntax error: incomptaible types, line " + getLineError());
                }

                if(!(exp1.getTypeExp() instanceof PrimitiveType) || ((PrimitiveType) exp1.getTypeExp()).type != AllType.BOOLEAN) {
                    throw new SyntaxException("Syntax error: incomptaible operands, line " + getLineError());
                }


                ExpNode exp = new ExpNode(AbstractNode.EXPRESSION);
                exp.setFirstOperand(exp1);
                exp.setOperator(operators.get(i));
                exp.setSecondOperand(exp2);
                exp.setTypeExp(exp1.getTypeExp());
                expressions.remove(i);
                expressions.remove(i);
                operators.remove(i);
                expressions.add(i, exp);
                i--;
            }
        }

        for(int i = 0; i < operators.size(); i++) {
            if(operators.get(i).equals("|")) {
                ExpNode exp1 = expressions.get(i);
                ExpNode exp2 = expressions.get(i + 1);

                if(!isTypesEqual(exp1.getTypeExp(), exp2.getTypeExp())) {
                    throw new SyntaxException("Syntax error: incomptaible types, line " + getLineError());
                }

                if(!(exp1.getTypeExp() instanceof PrimitiveType) || ((PrimitiveType) exp1.getTypeExp()).type != AllType.BOOLEAN) {
                    throw new SyntaxException("Syntax error: incomptaible operands, line " + getLineError());
                }


                ExpNode exp = new ExpNode(AbstractNode.EXPRESSION);
                exp.setFirstOperand(exp1);
                exp.setOperator(operators.get(i));
                exp.setSecondOperand(exp2);
                exp.setTypeExp(exp1.getTypeExp());
                expressions.remove(i);
                expressions.remove(i);
                operators.remove(i);
                expressions.add(i, exp);
                i--;
            }
        }

        for(int i = 0; i < operators.size(); i++) {
            if(     operators.get(i).equals("==") || operators.get(i).equals("!=") || operators.get(i).equals(">=")
                    || operators.get(i).equals("<=") || operators.get(i).equals(">") || operators.get(i).equals("<")) {
                ExpNode exp1 = expressions.get(i);
                ExpNode exp2 = expressions.get(i + 1);

                if(!isTypesEqual(exp1.getTypeExp(), exp2.getTypeExp())) {
                    throw new SyntaxException("Syntax error: incomptaible types, line " + getLineError());
                }

                if(!(exp1.getTypeExp() instanceof PrimitiveType) || (((PrimitiveType) exp1.getTypeExp()).type != AllType.BOOLEAN && ((PrimitiveType)exp1.getTypeExp()).type != AllType.STRING
                        && ((PrimitiveType)exp1.getTypeExp()).type != AllType.INT && ((PrimitiveType)exp1.getTypeExp()).type != AllType.FLOAT)) {
                    throw new SyntaxException("Syntax error: incomptaible operands, line " + getLineError());
                }


                ExpNode exp = new ExpNode(AbstractNode.EXPRESSION);
                exp.setFirstOperand(exp1);
                exp.setOperator(operators.get(i));
                exp.setSecondOperand(exp2);
                exp.setTypeExp(new PrimitiveType(AllType.BOOLEAN));
                expressions.remove(i);
                expressions.remove(i);
                operators.remove(i);
                expressions.add(i, exp);
                i--;
            }
        }

        return expressions.get(0);
    }

    //COMPLETE
    private ExpNode tryToRecognizeOperand(List<Token> list) throws GrammarRecognizeException, SyntaxException {
        ExpNode expNode;

        if(list.get(0).getType() == TokenEnum.NEW) {
            NewNode node = new NewNode(AbstractNode.NEW_NODE);

            list.remove(0);
            AllType type = AllType.getTypeByString(list.get(0).getType().getValue());

            if(type != null) {
                list.remove(0);
                node.sizeOfStruct = 4;
                if(list.size() > 0 && list.get(0).getType() == TokenEnum.OPENSQUARE) {
                    while (list.size() > 0 && list.get(0).getType() == TokenEnum.OPENSQUARE) {

                        list.remove(0);
                        int countOpen = 1;
                        int index = -1;
                        for (int i = 1; i < list.size(); i++) {
                            if (list.get(i).getType() == TokenEnum.OPENSQUARE) {
                                countOpen++;
                            } else if (list.get(i).getType() == TokenEnum.CLOSESQUARE) {
                                countOpen--;
                            }

                            if (countOpen == 0) {
                                index = i;
                                break;
                            }
                        }

                        if (index == -1) {
                            throw new GrammarRecognizeException("Grammar error: ] is expected, line " + getLineError());
                        }

                        expNode = tryToRecognizeExpression(list.subList(0, index));
                        list.remove(0);
                        if ((expNode.getTypeExp() instanceof PrimitiveType) && ((PrimitiveType) expNode.getTypeExp()).type == AllType.INT) {
                            node.sizes.add(expNode);
                        } else {
                            throw new SyntaxException("Syntax error: size of array must be int, line " + list.get(0).getLine());
                        }
                    }

                    ArrayType typeArr = new ArrayType();
                    typeArr.primitiveType = type;
                    typeArr.levels = node.sizes.size();

                    node.setTypeExp(typeArr);
                }
            } else {
                if(list.get(0).getType() == TokenEnum.ID) {//array or struct declaration
                    StructId struct = (StructId)TableId.lookupUserIdTableRecursive(tableId, list.get(0).getValue(), StructId.class);

                    if(struct == null) {
                        throw new SyntaxException("Syntax error: struct " + list.get(0).getValue() + " doesn't exist");
                    }

                    node.sizeOfStruct = struct.properties.size() * 4;

                    list.remove(0);

                    if(list.size() > 0 && list.get(0).getType() == TokenEnum.OPENSQUARE) {
                        while (list.size() > 0 && list.get(0).getType() == TokenEnum.OPENSQUARE) {
                            list.remove(0);
                            int countOpen = 1;
                            int index = -1;
                            for (int i = 1; i < list.size(); i++) {
                                if (list.get(i).getType() == TokenEnum.OPENSQUARE) {
                                    countOpen++;
                                } else if (list.get(i).getType() == TokenEnum.CLOSESQUARE) {
                                    countOpen--;
                                }

                                if (countOpen == 0) {
                                    index = i;
                                    break;
                                }
                            }

                            if (index == -1) {
                                throw new GrammarRecognizeException("Grammar error: ) is expected, line " + getLineError());
                            }

                            expNode = tryToRecognizeExpression(list.subList(0, index));
                            list.remove(0);
                            if ((expNode.getTypeExp() instanceof PrimitiveType) && ((PrimitiveType) expNode.getTypeExp()).type == AllType.INT) {
                                node.sizes.add(expNode);
                            } else {
                                throw new SyntaxException("Syntax error: size of array must be int, line " + list.get(0).getLine());
                            }
                        }

                        ArrayType typeArr = new ArrayType();
                        typeArr.structId = struct;
                        typeArr.levels = node.sizes.size();

                        node.setTypeExp(typeArr);
                    } else if(list.size() > 1 && list.get(0).getType() == TokenEnum.OPENPAREN && list.get(1).getType() == TokenEnum.CLOSEPAREN) {
                        list.remove(0);
                        list.remove(0);

                        StructType typeStruct = new StructType();
                        typeStruct.structId = struct;
                        node.setTypeExp(typeStruct);
                    } else {
                        throw new GrammarRecognizeException("Grammar error: incorrect operator new, line " + list.get(0).getLine());
                    }
                } else {
                    throw new GrammarRecognizeException("Grammar error: incorrect operator new, line " + list.get(0).getLine());
                }
            }

            return node;
        }

        if(list.get(0).getType() == TokenEnum.OPENPAREN) {
            int countOpen = 1;
            int index = -1;

            for(int i = 1; i < list.size(); i++) {
                if(list.get(i).getType() == TokenEnum.OPENPAREN) {
                    countOpen++;
                } else if(list.get(i).getType() == TokenEnum.CLOSEPAREN) {
                    countOpen--;
                }


                if(countOpen == 0) {
                    index = i;
                    break;
                }
            }

            if(index == -1) {
                throw new GrammarRecognizeException("Grammar error: ) is expected, line " + getLineError());
            }

            list.remove(0);
            expNode = tryToRecognizeExpression(list.subList(0, index - 1));
            list.remove(0);
            return expNode;
        } else {
            switch (list.get(0).getType()) {
                case INTCONST:
                    expNode = new ConstNode(AbstractNode.CONST_NODE, list.get(0).getValue(), new PrimitiveType(AllType.INT));
                    list.remove(0);
                    break;
                case FLOATCONST:
                    expNode = new ConstNode(AbstractNode.CONST_NODE, list.get(0).getValue(), new PrimitiveType(AllType.FLOAT));

                    boolean isConstExist = false;
                    for(Map.Entry<Integer, Float> e : TranslatorTAC.floats.entrySet()) {
                        if(e.getValue().equals(Float.parseFloat(list.get(0).getValue()))) {
                            isConstExist = true;
                        }
                    }

                    if(isConstExist) {
                        list.remove(0);
                        break;
                    }

                    TranslatorTAC.floats.put(TranslatorTAC.curPointerFloat, Float.parseFloat(list.get(0).getValue()));
                    TranslatorTAC.curPointerFloat+=4;
                    list.remove(0);
                    break;
                case BOOLEANCONST:
                    expNode = new ConstNode(AbstractNode.CONST_NODE, list.get(0).getValue(), new PrimitiveType(AllType.BOOLEAN));
                    list.remove(0);
                    break;
                case STRINGCONST:
                    expNode = new ConstNode(AbstractNode.CONST_NODE, list.get(0).getValue(), new PrimitiveType(AllType.STRING));
                    list.remove(0);
                    break;
                case ID:
                    expNode = tryToRecognizeComplexOperand(false, list);//id, id[...], id.id, id.id[...].id, id() - типа функция, etc
                    break;
                default:
                    throw new GrammarRecognizeException("Grammar error, line: " + list.get(0).getLine());
            }

            return expNode;
        }
    }

    /*  COMPLETE
    *   To be noticed: also lval for complex assign
     */
    private ExpNode tryToRecognizeComplexOperand(boolean isLeftPartAssign, List<Token> list) throws SyntaxException, GrammarRecognizeException {
        if(!isLeftPartAssign && list.size() > 1 && list.get(1).getType() == TokenEnum.OPENPAREN) {
            String id = list.get(0).getValue();
            list.remove(0);
            list.remove(0);

            FuncId func = (FuncId)TableId.lookupUserIdTableRecursive(tableId, id, FuncId.class);

            if(func == null) {
                throw new SyntaxException("Syntax error: function " + id + " doesn't exist");
            }

            FuncCallNode node = new FuncCallNode(AbstractNode.FUNC_CALL_NODE);
            node.setTypeExp(func.returnType);
            node.func = func;

            while(list.size() > 0 && list.get(0).getType() != TokenEnum.CLOSEPAREN) {
                int index = -1;
                int countParen = 1;

                for(int i = 0; i < list.size(); i++) {
                    if(list.get(i).getType() == TokenEnum.OPENPAREN) {
                        countParen++;
                    }

                    if(list.get(i).getType() == TokenEnum.CLOSEPAREN) {
                        countParen--;
                    }

                    if(countParen == 0 && list.get(i).getType() == TokenEnum.CLOSEPAREN) {
                        index = i;
                        break;
                    }

                    if(countParen == 1 && list.get(i).getType() == TokenEnum.COMMA) {
                        index = i;
                        break;
                    }
                }

                if(index == -1) {
                    throw new GrammarRecognizeException("Grammar error: invalid call function, line " + list.get(0).getLine());
                }

                ExpNode exp = tryToRecognizeExpression(list.subList(0, index));
                node.args.add(exp);

                if(list.get(0).getType() == TokenEnum.COMMA) {
                    list.remove(0);
                }
            }

            if(node.args.size() != func.properties.size()) {
                throw new SyntaxException("Syntax error: incorrect count arguments in function call, line " + list.get(0).getLine());
            }

            List<TypeVar> args = new ArrayList<TypeVar>();
            for(Map.Entry<TypeVar, String> arg : func.properties.entrySet()) {
                args.add(arg.getKey());
            }

            for(int i = 0; i < node.args.size(); ++i) {
                if(!isTypesEqual(node.args.get(i).getTypeExp(), args.get(i))) {
                    throw new SyntaxException("Syntax error: incorrect type argument in function call, line " + list.get(0).getLine());
                }
            }

            if(tokens.size() == 0) {
                throw new GrammarRecognizeException("Grammar error: ) is expected");
            }

            list.remove(0);

            return node;
        }

        ComplexOperand node = new ComplexOperand(AbstractNode.COMPLEXE_OPERAND_NODE);

        List<ExpNode> expressions = new ArrayList<ExpNode>();

        node.operands = expressions;

        String id = list.get(0).getValue();
        list.remove(0);

        VarId idRow = (VarId)TableId.lookupUserIdTableRecursive(tableId, id, VarId.class);

        if(idRow == null) {
            throw new SyntaxException("Syntax error: id " + id + " isn't defiend, line " + (list.size() > 0 ? list.get(0).getLine() : "") );
        }

        IdNode idNode = new IdNode(AbstractNode.ID_NODE, idRow);
        idNode.setTypeExp(idRow.type);
        idNode.name = idRow.name;
        TypeVar curType = idRow.type.clone();

        node.operands.add(idNode);
        node.setTypeExp(idNode.getTypeExp());

        if(list.size() == 0) {
            node.setTypeExp(idNode.getTypeExp());
            return node;
        }

        while(list.size() > 0) {
            switch (list.get(0).getType()) {
                case DOT:
                    if (!(curType instanceof StructType)) {
                        throw new SyntaxException("Syntax error: . operator is only allowed for structs, line " + list.get(0).getLine());
                    }

                    StructId structId = ((StructType) curType).structId;

                    list.remove(0);

                    if (list.get(0).getType() == TokenEnum.ID) {
                        TypeVar type = null;

                        for (Map.Entry<TypeVar, String> entry : structId.properties.entrySet()) {
                            if (entry.getValue().equals(list.get(0).getValue())) {
                                type = entry.getKey();
                            }
                        }

                        if (type == null) {
                            throw new SyntaxException("Syntax error: member " + list.get(0).getValue() + " doesn't exist in struct " + structId.name + " line " + list.get(0).getLine());
                        }

                        curType = type.clone();



                        FieldNode fNode = new FieldNode(AbstractNode.FIELD_NODE);
                        fNode.setTypeExp(type.clone());
                        fNode.parent = structId;
                        fNode.propName = list.get(0).getValue();
                        node.operands.add(fNode);
                        node.setTypeExp(curType);

                        list.remove(0);
                    } else {
                        throw new GrammarRecognizeException("Grammar error: id is expected, line " + list.get(0).getLine());
                    }
                    break;
                case OPENSQUARE:
                    if (!(curType instanceof ArrayType)) {
                        throw new SyntaxException("Syntax error: [] operator is only allowed for arrays, line " + list.get(0).getLine());
                    }

                    list.remove(0);

                    int countOpen = 1;
                    int index = -1;
                    for (int i = 1; i < list.size(); i++) {
                        if (list.get(i).getType() == TokenEnum.OPENSQUARE) {
                            countOpen++;
                        } else if (list.get(i).getType() == TokenEnum.CLOSESQUARE) {
                            countOpen--;
                        }

                        if (countOpen == 0) {
                            index = i;
                            break;
                        }
                    }

                    if (index == -1) {
                        throw new GrammarRecognizeException("Grammar error: ] is expected, line " + getLineError());
                    }


                    ExpNode expNode = tryToRecognizeExpression(list.subList(0, index));

                    list.remove(0);

                    if ((expNode.getTypeExp() instanceof PrimitiveType) && ((PrimitiveType) expNode.getTypeExp()).type == AllType.INT) {
                        node.operands.add(expNode);
                        ArrayType arrType = (ArrayType)curType.clone();

                        if(arrType.levels > 1) {
                            arrType.levels--;
                            curType = arrType;
                        } else {
                            if(arrType.structId != null) {
                                StructType structType = new StructType();
                                structType.structId = arrType.structId;
                                curType = structType;
                            } else {
                                PrimitiveType primitiveType = new PrimitiveType();
                                primitiveType.type = ((ArrayType) curType).primitiveType;
                                curType = primitiveType;
                            }
                        }

                        node.setTypeExp(curType);
                    } else {
                        throw new SyntaxException("Syntax error: index of array must be int, line " + list.get(0).getLine());
                    }
                    break;
                default:
                    tryToRecognizeOperator(list);//find out: is next token operator, if is not - error
                    return node;
            }
        }

        return node;
    }

    ///COMPLETE
    private void tryToRecognizeDeclaration(List<StatementNode> statements, boolean isForFor) throws GrammarRecognizeException, SyntaxException {
        TypeVar type = tryToRecognizeType(false);

        if(tokens.get(0).getType() != TokenEnum.ID) {
            throw new GrammarRecognizeException("Grammar error: incorrect declaration, line " + getLineError());
        }

        VarId r = (VarId)tableId.lookupUserIdTable(tokens.get(0).getValue(), VarId.class);

        if(r != null) {
            throw new SyntaxException("Syntax error: variable " + tokens.get(0).getValue() + " is already declared, line " + getLineError());
        }

        VarId row = new VarId();
        row.type = type;
        row.name = tokens.get(0).getValue();
        Integer offset = TranslatorTAC.curOffsetFromStartStack.pop();
        row.offset = offset.intValue();
        offset += 4;
        TranslatorTAC.curOffsetFromStartStack.push(offset.intValue());

        removeFirstTokenSeveralTimes(1);

        if(tokens.get(0).getType() == TokenEnum.ASSIGN) {
            removeFirstTokenSeveralTimes(1);

            if(tokens.get(0).getType() == TokenEnum.SEMICOLON) {
                throw new GrammarRecognizeException("Grammar error: expression can't be empty, line " + getLineError());
            }

            int index = 0;
            for(int i = 1; i < tokens.size(); ++i) {
                if(tokens.get(i).getType() == TokenEnum.SEMICOLON) {
                    index = i;
                    break;
                }
            }

            if(index == 0) {
                throw new GrammarRecognizeException("Grammar error: ; is expected, line " + getLineError());
            }

            ExpNode expNode = tryToRecognizeExpression(tokens.subList(0, index));

            if(!isTypesEqual(expNode.getTypeExp(), type)) {
                throw new SyntaxException("Syntax error: incompatible types in declaration, line " + getLineError());
            }

            removeFirstTokenSeveralTimes(1);
            row.isInitialised = true;
            tableId.getIds().add(row);

            DeclareVarNode node = new DeclareVarNode(AbstractNode.DECLARE_VAR_NODE);
            node.var = row;
            node.expNode = expNode;
            node.type = type;
            statements.add(node);

            if(!isForFor) TranslatorTAC.createAssignStatement(row, expNode);
        } else if(tokens.get(0).getType() == TokenEnum.SEMICOLON) {
            if(isForFor) {
                throw new SyntaxException("Grammar error: declaration with assign is expected in for, line " + getLineError());
            }

            removeFirstTokenSeveralTimes(1);
            row.isInitialised = false;
            tableId.getIds().add(row);

            DeclareVarNode node = new DeclareVarNode(AbstractNode.DECLARE_VAR_NODE);
            node.var = row;
            node.type = type;

            statements.add(node);
        } else {
            throw new GrammarRecognizeException("Grammar error: incorrect declaration, line " + getLineError());
        }
    }

    //COMPLETE
    private boolean isTypesEqual(TypeVar type1, TypeVar type2) {
        if(type1 instanceof StructType && type2 instanceof StructType) {
            if(((StructType)type1).structId.name.equals(((StructType)type2).structId.name)) {
                return true;
            }
        }

        if(type1 instanceof ArrayType && type2 instanceof ArrayType) {
            ArrayType arr1 = (ArrayType)type1;
            ArrayType arr2 = (ArrayType)type2;
            if(arr1.levels == arr2.levels && ((arr1.structId != null && arr2.structId != null && arr1.structId.name.equals(arr2.structId.name)) || (arr1.primitiveType == arr2.primitiveType))) {
                return true;
            }
        }

        if(type1 instanceof PrimitiveType && type2 instanceof PrimitiveType) {
            if(((PrimitiveType)type1).type.equals(((PrimitiveType)type2).type)) {
                return true;
            }
        }

        return false;
    }

    //COMPLETE
    private void tryToRecognizeGoto(List<StatementNode> statements) throws GrammarRecognizeException, SyntaxException {
        if(tokens.get(1).getType() == TokenEnum.ID) {
            LabelNode labelNode = null;

            boolean isExist = false;
            for(LabelNode label : currentFunction.getLabels()) {
                if(label.getName().equals(tokens.get(1).getValue())) {
                    isExist = true;
                    labelNode = label;
                }
            }

            if(!isExist) {
                throw new SyntaxException("Syntax error: label " + tokens.get(1).getValue() + " doesn't exist, line " + getLineError());
            }

            GotoNode node = new GotoNode(AbstractNode.GOTO, tokens.get(1).getValue());
            statements.add(node);

            TranslatorTAC.createGoTOStatement(labelNode);

            removeFirstTokenSeveralTimes(3);
        } else {
            throw new GrammarRecognizeException("Grammar error: incorrect goto, line " + getLineError());
        }
    }

    //COMPLETE
    private void tryToRecognizeLabel(List<StatementNode> statements) throws GrammarRecognizeException, SyntaxException {
        if(tokens.get(1).getType() == TokenEnum.ID) {
            for(LabelNode label : currentFunction.getLabels()) {
                if(label.getName().equals(tokens.get(1).getValue())) {
                    throw new SyntaxException("Syntax error: label is exist, line " + getLineError());
                }
            }

            LabelNode node = new LabelNode(tokens.get(1).getValue(), AbstractNode.LABEL);
            statements.add(node);

            currentFunction.getLabels().add(node);

            TranslatorTAC.createLabelStatement(node);

            removeFirstTokenSeveralTimes(2);
        } else {
            throw new GrammarRecognizeException("Grammar error: incorrect label, line " + getLineError());
        }
    }

    //COMPLETE
    private void tryToRecognizeArgumentInList(LinkedHashMap<TypeVar, String> props, LinkedHashMap<String, IdRow> propIds) throws GrammarRecognizeException, SyntaxException, IndexOutOfBoundsException {
        TypeVar type = tryToRecognizeType(false);

        if(tokens.get(0).getType() == TokenEnum.ID) {
            if(props != null && isVariableExistInArgs(props, tokens.get(0).getValue())) {
                throw new SyntaxException("Syntax error: variable " + tokens.get(0).getValue() + " is already defined in function args, line " + getLineError());
            }

            props.put(type, tokens.get(0).getValue());

            if(tokens.get(1).getType() == TokenEnum.COMMA) {
                removeFirstTokenSeveralTimes(2);
            } else if(tokens.get(1).getType() == TokenEnum.CLOSEPAREN) {
                removeFirstTokenSeveralTimes(1);
            }
        } else {
            throw new GrammarRecognizeException("Grammar error: incorrect function arguments, line " + getLineError());
        }
    }

    //COMPLETE
    private void tryToRecognizeDeclarationWithoutAssign(LinkedHashMap<TypeVar, String> props) throws GrammarRecognizeException, SyntaxException, IndexOutOfBoundsException {
        if(tokens.size() < 3) throw new GrammarRecognizeException("Grammar error: incorrect declaration syntax, line " + getLineError());

        TypeVar type = tryToRecognizeType(false);

        if(tokens.size() > 1 && tokens.get(0).getType() == TokenEnum.ID && tokens.get(1).getType() == TokenEnum.SEMICOLON) {
            if(props != null && isVariableExistInArgs(props, tokens.get(0).getValue())) {
                throw new SyntaxException("Syntax error: variable " + tokens.get(1).getValue() + " is already defined in struct, line " + getLineError());
            }

            props.put(type, tokens.get(0).getValue());

            removeFirstTokenSeveralTimes(2);
        } else {
            throw new GrammarRecognizeException("Grammar error: incorrect declaration syntax, line " + tokens.get(0).getLine());
        }
    }

    //COMPLETE
    private TypeVar tryToRecognizeType(boolean isFunction) throws SyntaxException, GrammarRecognizeException, IndexOutOfBoundsException {
        if(AllType.getTypeByString(tokens.get(0).getType().getValue()) != null) {
            AllType type = AllType.getTypeByString(tokens.get(0).getType().getValue());
            removeFirstTokenSeveralTimes(1);

            if(!isFunction && type == AllType.VOID) {
                throw new SyntaxException("Syntax error: variable can't be void");
            }

            if(tokens.get(0).getType() == TokenEnum.OPENSQUARE && tokens.get(1).getType() == TokenEnum.CLOSESQUARE){
                ArrayType typeVar = new ArrayType();
                typeVar.primitiveType = type;
                typeVar.levels = 0;

                while(tokens.get(0).getType() == TokenEnum.OPENSQUARE) {
                    if(tokens.get(1).getType() == TokenEnum.CLOSESQUARE) {
                        typeVar.levels++;
                        removeFirstTokenSeveralTimes(2);
                    } else {
                        throw new SyntaxException("Syntax error: ] is expected, line " + getLineError());
                    }
                }

                return typeVar;
            } else {
                PrimitiveType typeVar = new PrimitiveType();
                typeVar.type = type;
                return typeVar;
            }
        } else if(tokens.get(0).getType() == TokenEnum.ID) {//struct or arraystruct
            StructId struct = (StructId)TableId.lookupUserIdTableRecursive(tableId, tokens.get(0).getValue(), StructId.class);

            if(struct == null) {
                throw new SyntaxException("Syntax error: struct " + tokens.get(0).getValue() + " doesn't exist, line " + getLineError());
            }

            removeFirstTokenSeveralTimes(1);

            if(tokens.get(0).getType() == TokenEnum.OPENSQUARE && tokens.get(1).getType() == TokenEnum.CLOSESQUARE){
                ArrayType typeVar = new ArrayType();
                typeVar.structId = struct;
                typeVar.levels = 0;

                while(tokens.get(0).getType() == TokenEnum.OPENSQUARE) {
                    if(tokens.get(1).getType() == TokenEnum.CLOSESQUARE) {
                        typeVar.levels++;
                        removeFirstTokenSeveralTimes(2);
                    } else {
                        throw new SyntaxException("Syntax error: ] is expected, line " + getLineError());
                    }
                }

                return typeVar;
            } else {
                StructType typeVar = new StructType();
                typeVar.structId = struct;
                return typeVar;
            }
        } else {
            throw new GrammarRecognizeException("Grammar error: incorrect type recognize, line " + getLineError());
        }
    }

    //COMPLETE
    private boolean isVariableExistInArgs(LinkedHashMap<TypeVar, String> props, String name) {
        for(String v : props.values()) {
            if(v.equals(name)) {
                return true;
            }
        }

        return false;
    }

    //COMPLETE
    private void removeFirstTokenSeveralTimes(int times) throws IndexOutOfBoundsException {
        for(int i = 0; i < times; ++i) {
            tokens.remove(0);
        }
    }

    //COMPLETE
    private int getLineError() {
        return tokens.size() == 0 ? countLines : tokens.get(0).getLine();
    }

    //COMPLETE
    private int getLineError(List<Token> list) {
        return list.size() == 0 ? countLines : list.get(0).getLine();
    }
}
