package parser;

import beans.AllType;
import beans.tac.TAC;
import beans.tac.tacExt.*;
import beans.type.PrimitiveType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * Created by antonskripacev on 27.04.17.
 */
public class Codegen {
    private static File f;
    private static FileWriter fw;
    public static void startCodegen() throws IOException {
        f = new File("example.andmips");
        fw = new FileWriter(f);

        try {
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        fw.write(".data\n");
        fw.write("pointerHeap: .word 0\n");
        fw.write("heap:  .space  100000\n");
        fw.write("floats:  .float ");

        int index = 0;
        for(Map.Entry<Integer, Float> e : TranslatorTAC.floats.entrySet()) {
            fw.write(Float.toString(e.getValue()));

            if(index != TranslatorTAC.floats.size() - 1) {
                fw.write(", ");
            }

            index++;
        }

        fw.write("\n");

        fw.write(".text\n");
        fw.write("jal func_main\n");
        fw.write("addi $sp, $sp, -" + GrammarParser.mainFunc.sumOffset + "\n");
        fw.write("li $v0, 10\n" + "syscall\n");

        for(TAC tac : TranslatorTAC.tacs) {


            if(tac instanceof FuncDeclTAC) {
                writeFuncDeclCode((FuncDeclTAC)tac);
            }

            if(tac instanceof GoToTAC) {
                writeGoToCode((GoToTAC)tac);
            }

            if(tac instanceof LabelTAC) {
                writeLabelCode((LabelTAC) tac);
            }

            if(tac instanceof ParamTAC) {
                writeParamCode((ParamTAC) tac);
            }

            if(tac instanceof ReturnTAC) {
                writeReturnTac((ReturnTAC)tac);
            }

            if(tac instanceof AddHeapTAC) {
                writeAddToHeapCode((AddHeapTAC)tac);
            }

            if(tac instanceof BinOperTAC) {
                writeBinOperCode((BinOperTAC) tac);
            }
        }

        fw.close();
    }

    private static void writeAddToHeapCode(AddHeapTAC tac) throws IOException {
        fw.write("la $s0, pointerHeap\n");
        fw.write("la $s1, heap\n");
        fw.write("lw $s0, $s0\n");
        fw.write("add $s1, $s1, $s0\n");
        fw.write("sw " + tac.tempVar + ", 0($s1)\n");
        fw.write("addi $s0, $s0, 4\n");
        fw.write("la $s1, pointerHeap\n");
        fw.write("sw $s0, 0($s1)\n");
    }

    private static void writeBinOperCode(BinOperTAC tac) throws IOException {
        if(tac.operation != null) {
            switch (tac.operation) {
                case CALL:
                    fw.write("jar func_" + tac.fOp.refFunc.name + "\n");
                    break;
                case LOAD:
                    if(tac.sOp.constant == null) {
                        fw.write("lw $t5, 0($t5)\n");
                    } else {
                        fw.write("lw " + tac.result.tempVar + ", " + tac.sOp.constant + "(" + tac.fOp.tempVar + ")\n");
                    }
                    break;
                case IFTRUE:
                    fw.write("bgt " + tac.fOp.tempVar + ", $zero, " + tac.sOp.label + "\n");
                    break;
                case IFFALSE:
                    fw.write("beq " + tac.fOp.tempVar + ", $zero, " + tac.sOp.label + "\n");
                    break;
                case SAVE:
                    if(tac.result.tempVar == null) {
                        fw.write("sw " + tac.fOp.tempVar + " , " + tac.result.refVar.offset + "($sp)\n");
                    } else {
                        fw.write("sw " + tac.fOp.tempVar + " , 0(" + tac.result.tempVar + ")\n");
                    }
                    break;
                case ARRAY:
                    if(tac.offset != 0) {
                        fw.write("lw " + tac.result.tempVar + " , " + tac.offset + "(" + tac.result.tempVar + ")\n");
                    }
                    break;
                case NEW:
                    fw.write("\nla $s0, pointerHeap\n");
                    fw.write("lw $s0, $s0\n");
                    fw.write("move " + tac.result.tempVar + ", $s0\n");

                    if(tac.sOp.constant == null) {
                        fw.write("addi $s0, $s0, " + tac.fOp.constant + "\n");
                    } else {
                        fw.write("add $s0, $s0, " + tac.sOp.constant + "\n");
                    }

                    fw.write("la $s1, pointerHeap" + "\n");
                    fw.write("sw $s0, 0($s1)\n\n");

                    break;
                case PLUS:
                    PrimitiveType type = (PrimitiveType)tac.type;
                    if(tac.fOp.tempVar != null && tac.sOp.tempVar != null && tac.result.tempVar != null) {
                        if(type.type == AllType.INT) {
                           fw.write("add " + tac.result.tempVar + ", " + tac.fOp.tempVar + ", " + tac.sOp.tempVar + "\n");
                        } else if(type.type == AllType.FLOAT) {
                            String line = "add.s " + tac.result.tempVar + ", " + tac.fOp.tempVar + ", " + tac.sOp.tempVar + "\n";
                            line = line.replace("t", "f");
                            fw.write(line);
                        }
                    }
                default:
                    fw.write("unrecognized operation here\n");
                    break;
            }
        } else {
            fw.write("unrecognized operation here\n");
        }
    }

    private static void writeFuncDeclCode(FuncDeclTAC tac) throws IOException {
        fw.write(tac.name + ":\n");
        fw.write("sw $ra, 0($sp)\n");
    }

    private static void writeGoToCode(GoToTAC tac) throws IOException {
        fw.write("j " + tac.label + "\n");
    }

    private static void writeLabelCode(LabelTAC tac) throws IOException {
        fw.write(tac.label + "\n");
    }

    private static void writeParamCode(ParamTAC tac) throws IOException {
        fw.write("move $a" + tac.number + ", " + tac.tVar + "\n");
    }

    private static void writeReturnTac(ReturnTAC tac) throws IOException {
        fw.write("addi $sp, $sp, " + tac.func.sumOffset + "\n");
    }
}
