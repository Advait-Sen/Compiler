import error.ExpressionError;
import parser.Parser;
import parser.node.NodeProgram;
import parser.node.statement.NodeStatement;
import tokeniser.Token;
import tokeniser.Tokeniser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

@SuppressWarnings({"FieldCanBeLocal", "SpellCheckingInspection"})
public class Main {
    public static RuntimeException throwError(String message) {
        return new RuntimeException(message);
    }

    /**
     * Flag saying whether parsing is occurring
     */
    private static boolean DO_PARSE;
    /**
     * Flag for verbose messages
     */
    private static boolean VERBOSE;

    public static void main(String[] args) {

        if (args.length == 0) {
            throw throwError("Must have at least a file name to compile");
        }

        final String fileName = args[0];

        //Getting the compiler arguments
        Set<String> compilerArgs = Set.of(Arrays.copyOfRange(args, 1, args.length));

        DO_PARSE = !(compilerArgs.contains("-noparse") || compilerArgs.contains("-np"));
        VERBOSE = compilerArgs.contains("-verbose") || compilerArgs.contains("-v");

        System.out.println("Reading from file: " + fileName);

        StringBuilder fileInput = new StringBuilder();

        try {
            BufferedReader bfr = new BufferedReader(new FileReader(fileName));

            for (String line = bfr.readLine(); line != null; line = bfr.readLine()) {
                fileInput.append(line).append('\n');
            }

            bfr.close();

        } catch (IOException e) {
            throw throwError("Could not read from file " + fileName);
        }

        String input = fileInput.toString();

        if (VERBOSE) {
            System.out.println(fileName + ":");
            System.out.println(input + "\nEnd of File\n");
        }

        System.out.println("Initialising Tokeniser");
        Tokeniser tokeniser = new Tokeniser(input);

        try {
            tokeniser.tokenise();
        } catch (ExpressionError expressionError) {
            //Not using throwError here, since it's the programmer's fault, not compiler's fault
            System.out.println("Error in tokenisation:");
            System.out.println(expressionError.getMessage());
            System.exit(-1);
            return;
        }

        if (VERBOSE) {
            System.out.println("Tokens (" + tokeniser.tokens().size() + "):");
            for (Token token : tokeniser.tokens()) {
                System.out.println(token.type.toString() + ":= " + token.value);
            }
        }

        if (DO_PARSE) {
            Parser parser = new Parser(tokeniser);

            NodeProgram program;

            try {
                program = parser.parse();
            } catch (ExpressionError expressionError) {
                //Not using throwError here, since it's the programmer's fault, not compiler's fault
                System.out.println("Error in parsing:");
                System.out.println(expressionError.getMessage());
                System.exit(-1);
                return;
            }

            if (VERBOSE) {
                System.out.println("\nprogram.asString() =");
                for (NodeStatement statement : program.statements) {
                    System.out.println(statement.typeString() + " : " + statement.asString());
                }
            }
        }

        System.exit(0);
    }
}
