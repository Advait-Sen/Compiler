import error.ExpressionError;
import parser.Parser;
import parser.node.NodeProgram;
import parser.node.statement.NodeStatement;
import tokeniser.Token;
import tokeniser.Tokeniser;

public class Main {
    public static void main(String[] args) {
        System.out.println("Initialising Tokeniser");
        String input = "exit (45) exit ((3.45))";
        Tokeniser tokeniser = new Tokeniser(input);

        System.out.println("Code:\n");
        System.out.println(input+"\n\n");

        try {
            tokeniser.tokenise();
        } catch (ExpressionError expressionError){
            System.out.println("Error in tokenisation:");
            System.out.println(expressionError.getMessage());
            System.exit(-1);
            return;
        }

        System.out.println("Tokens:");
        for (Token token : tokeniser.tokens()) {
            System.out.println("type = " + token.type.toString() + " value = "+token.value + " colpos = " + token.colpos);
        }

        Parser parser = new Parser(tokeniser);

        NodeProgram program = parser.parse();

        System.out.println("program.asString() = \n" + program.asString());


        System.exit(0);
    }
}
