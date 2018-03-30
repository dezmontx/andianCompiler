import beans.Token;
import beans.node.AbstractNode;
import parser.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by antonskripacev on 23.03.17.
 */
public class Andian {
    private static boolean isPrintTableToken = false;
    private static boolean isPrintTree = false;
    private static boolean isPrintLLVM = false;

    public static void main(String[] args) throws IOException {
        if(args.length > 0) {
            for(int i = 0; i < args.length; ++i) {
                if(args[i].equals("-t")) {
                    isPrintTableToken = true;
                }

                if(args[i].equals("-ast")) {
                    isPrintTree = true;
                }

                if(args[i].equals("-llvm")) {
                    isPrintLLVM = true;
                }
            }

            File f = new File(args[args.length - 1]);

            if(f.exists()) {
                List<AbstractNode> tree;
                try {
                    LinkedList<Token> listTokens = new TokenParser().parse(f);

                    if(isPrintTableToken) {
                        new Visualiser().visualiseTableToken(listTokens);
                    }

                    tree = new GrammarParser().parse(new ArrayList<Token>(listTokens));

                    if(isPrintTree) {
                        new Visualiser().visualiseAST(tree, 0);
                    }

                    if(isPrintLLVM) {
                        LLVMGenerator.generateLLVM(tree);
                    }

                    //optimization should start here

                    Codegen.startCodegen();
                } catch (Throwable e) {
                    e.printStackTrace();
                    System.out.println(e.getMessage());
                }


            } else {
                System.err.println("File " + args[args.length - 1] + " doesn't exist" );
            }
        } else {
            System.err.println("Please specify a path to the main file");
        }
    }
}
