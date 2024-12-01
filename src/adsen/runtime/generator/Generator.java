package adsen.runtime.generator;

import adsen.error.ExpressionError;
import adsen.parser.node.NodeExpr;
import adsen.parser.node.NodeProgram;
import adsen.parser.node.statement.DeclareStatement;
import adsen.parser.node.statement.ExitStatement;
import adsen.parser.node.statement.NodeStatement;
import adsen.runtime.Context;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import static adsen.runtime.Context.*;

/**
 * Classes which generate assembly from code will inherit from this class.
 * <p>
 * In order to make it easier (and since the language is still pretty simple, a lot of work is standardised
 * This means that classes inheriting from {@link Generator} will simply need to implement the direct assembly-translating methods
 */
public abstract class Generator {
    /**
     * The program from which we are generating assembly
     */
    public final NodeProgram program;
    /**
     * Stack used to keep track of variables and expressions at compiletime
     */
    private final Stack<NodeExpr> exprStack = new Stack<>();
    /**
     * Final code generated by {@link Generator#generateProgramCode()}
     */
    private StringBuilder code;
    /**
     * Map used to keep track of variables
     */
    private Set<String> variables;

    public Generator(NodeProgram program) {
        this.program = program;
    }

    public void generateProgramCode() {
        code = new StringBuilder();

        appendBlock(boilerplate());
        appendBlock(startMain());

        variables = new HashSet<>();

        for (NodeStatement statement : program.statements) {
            code.append("    "); //Maybe remove this, or make it a flag or smth
            generateStatement(statement);
            code.append('\n');
        }

        appendBlock(syscall(EXIT, 0));
    }

    private void generateStatement(NodeStatement statement) {
        if (statement instanceof ExitStatement exit) {
            generateExpr(exit.expr(), EXIT);
            syscall(EXIT);
        } else if (statement instanceof DeclareStatement assign) {
            //todo distinguish when adding +=, -=, etc.
            String variableName = assign.identifier().asString();
            if (variables.contains(variableName)) {
                //Exact copy of Java message
                throw new ExpressionError("Variable '" + variableName + "' is already defined in the scope", assign.identifier().token);
            }

            generateExpr(assign.expr(), ASSIGNMENT);
            assignVariable(variableName);
        }


        //NOT ACTUAL CODE OFC
        code.append(statement.asString());
    }

    /**
     * Appends a block of code generated as a {@code String[]}, adding in newlines but not indentation
     */
    protected void appendBlock(String[] block) {
        for (String s : block) {
            code.append(s).append('\n');
        }
    }

    //Assembly generating code

    abstract void assignVariable(String variableName);

    abstract void generateExpr(NodeExpr expr, Context context);


    /**
     * This method will mark the beginning of the main program.
     * For example, in Linux it would be something like:
     * <pre>
     * {@code
     * .globl MAIN
     * MAIN:
     * }
     * </pre>
     * The {@code String[]} array contains each individual assembly statement
     */
    abstract String[] startMain();



    /**
     * This method will generate the eventual boilerplate code required by each assembly language
     * The {@code String[]} array contains each individual assembly statement
     */
    abstract String[] boilerplate();


    /**
     * This method will be overwritten by each implementor class to fit in their own ways of running syscalls
     * Takes a {@link Context} in order to determine the type of syscall to make
     */
    abstract String[] syscall(Context context, long... params);
}
