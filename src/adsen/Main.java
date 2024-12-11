package adsen;

import adsen.error.ExpressionError;
import adsen.parser.Parser;
import adsen.parser.node.NodeFunction;
import adsen.parser.node.NodeProgram;
import adsen.parser.node.primitives.IntPrimitive;
import adsen.parser.node.primitives.NodePrimitive;
import adsen.parser.node.statement.NodeStatement;
import adsen.runtime.interpreter.Interpreter;
import adsen.tokeniser.Token;
import adsen.tokeniser.Tokeniser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings({"FieldCanBeLocal", "SpellCheckingInspection"})
public class Main {
    public static RuntimeException throwError(String message) {
        return new RuntimeException(message);
    }

    /**
     * Flag for new function parsing style
     */
    private static boolean NEW_PARSE_FUNC;

    /**
     * Flag saying whether parsing is occurring
     */
    private static boolean DO_PARSE;
    /**
     * Flag to interpret
     */
    private static boolean INTERPRET;
    /**
     * Flag to compile
     */
    private static boolean COMPILE;

    /**
     * Flags for verbose messages
     */
    public static Set<String> VERBOSE_FLAGS;

    public static void main(String[] args) {

        if (args.length == 0) {
            throw throwError("Must have at least a file name to compile");
        }

        final String fileName = args[0];
        int verboseFlagIndex;
        //Gonna replace this with better flag search once more flags are added
        for (verboseFlagIndex = 1; verboseFlagIndex < args.length; verboseFlagIndex++) {
            if (args[verboseFlagIndex].equals("-verbose") || args[verboseFlagIndex].equals("-v")) {
                break;
            }
        }

        VERBOSE_FLAGS = new HashSet<>();

        for (verboseFlagIndex += 1; verboseFlagIndex < args.length; verboseFlagIndex++) {
            if (args[verboseFlagIndex].startsWith("-")) break;

            switch (args[verboseFlagIndex]) {
                case "t", "tokeniser" -> VERBOSE_FLAGS.add("tokeniser");
                case "p", "parser" -> VERBOSE_FLAGS.add("parser");
                case "i", "interpreter" -> VERBOSE_FLAGS.add("interpreter");
                case "g", "generator" -> VERBOSE_FLAGS.add("generator");
            }
        }

        VERBOSE_FLAGS = Collections.unmodifiableSet(VERBOSE_FLAGS); //Not rly necessary, but looks cool

        //Getting the compiler arguments
        Set<String> compilerArgs = Set.of(Arrays.copyOfRange(args, 1, args.length));

        DO_PARSE = !(compilerArgs.contains("-noparse") || compilerArgs.contains("-np"));
        NEW_PARSE_FUNC = compilerArgs.contains("-func") || compilerArgs.contains("-f");
        INTERPRET = compilerArgs.contains("-interpret") || compilerArgs.contains("-i");
        COMPILE = compilerArgs.contains("-compile") || compilerArgs.contains("-c");

        if (INTERPRET && COMPILE)
            throw throwError("Invalid flags, cannot compile and interpret at the same time");

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

        if (!VERBOSE_FLAGS.isEmpty()) { //If we have any verbose messages at all, then print out the code
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

        if (VERBOSE_FLAGS.contains("tokeniser")) {
            System.out.println("Tokens (" + tokeniser.tokens().size() + "):");
            for (Token token : tokeniser.tokens()) {
                System.out.println(token);
            }
        }

        if (NEW_PARSE_FUNC) {
            System.out.println("Initialising new function-based Parser");
            Parser parser = new Parser(tokeniser);

            NodeProgram program;
            try {
                program = parser.parse();
            } catch (ExpressionError expressionError) {
                //Not using throwError here, since it's the programmer's fault, not compiler's fault
                System.out.println("Error in new parsing:");
                System.out.println(expressionError.getMessage());
                System.exit(-1);
                return;
            }

            if (VERBOSE_FLAGS.contains("parser")) {
                System.out.println("\nprogram.asStringOld() =");
                program.functions.forEach(System.out::println);
            }
        }

        if (DO_PARSE) {
            System.out.println("Initialising Parser");

            Parser parser = new Parser(tokeniser);

            NodeProgram program;

            try {
                program = parser.parseOld();
            } catch (ExpressionError expressionError) {
                //Not using throwError here, since it's the programmer's fault, not compiler's fault
                System.out.println("Error in parsing:");
                System.out.println(expressionError.getMessage());
                System.exit(-1);
                return;
            }

            if (VERBOSE_FLAGS.contains("parser")) {
                System.out.println("\nprogram.asStringOld() =");
                for (NodeStatement statement : program.statements) {
                    System.out.println(statement.typeString() + " : " + statement.asString());
                }
            }

            //Run interpreter
            if (INTERPRET) {
                Interpreter interpreter = new Interpreter(program);
                NodePrimitive exitValue;

                try {
                    exitValue = interpreter.run();
                } catch (ExpressionError error) {
                    System.out.println(error.getMessage());
                    exitValue = IntPrimitive.of(-1);
                }

                if (VERBOSE_FLAGS.contains("interpreter")) {
                    System.out.println("\nProgram variables:");
                    interpreter.variables().forEach((s, np) -> System.out.println(s + ": " + np.asString()));
                }

                System.out.println("\nProcess finished with exit value " + exitValue.asString());
            }

        }

        System.exit(0);
    }
}
