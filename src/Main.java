import error.ExpressionError;
import parser.Parser;
import parser.node.NodeProgram;
import parser.node.statement.NodeStatement;
import tokeniser.Token;
import tokeniser.Tokeniser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {

        final String fileName = args[0];

        System.out.println("Reading from file: " + fileName);

        StringBuilder fileInput = new StringBuilder();

        try {
            BufferedReader bfr = new BufferedReader(new FileReader(fileName));

            for (String line = bfr.readLine(); line != null; line = bfr.readLine()) {
                fileInput.append(line).append('\n');
            }

            bfr.close();

        } catch (IOException e) {
            System.out.println("Could not read from file " + fileName);
            System.exit(-1);
            return;
        }

        String input = fileInput.toString();

        System.out.println(fileName + ":");
        System.out.println(input + "\nEnd of File\n");

        System.out.println("Initialising Tokeniser");
        Tokeniser tokeniser = new Tokeniser(input);

        try {
            tokeniser.tokenise();
        } catch (ExpressionError expressionError) {
            System.out.println("Error in tokenisation:");
            System.out.println(expressionError.getMessage());
            System.exit(-1);
            return;
        }

        System.out.println("Tokens (" + tokeniser.tokens().size() + "):");
        for (Token token : tokeniser.tokens()) {
            System.out.println(token.type.toString() + ":= " + token.value);
        }

        Parser parser = new Parser(tokeniser);

        NodeProgram program = parser.parse();

        System.out.println("\nprogram.asString() =");
        for (NodeStatement statement : program.statements) {
            System.out.println(statement.typeString() + " : " + statement.asString());
        }


        System.exit(0);
    }
}
