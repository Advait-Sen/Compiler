package parser;

import error.ExpressionError;
import parser.node.NodeExpr;
import parser.node.NodeProgram;
import parser.node.identifier.NodeIdentifier;
import parser.node.operator.Operator;
import parser.node.operator.OperatorType;
import parser.node.primitives.BoolPrimitive;
import parser.node.primitives.CharPrimitive;
import parser.node.primitives.FloatPrimitive;
import parser.node.primitives.IntPrimitive;
import parser.node.statement.AssignStatement;
import parser.node.statement.ExitStatement;
import parser.node.statement.NodeStatement;
import parser.node.statement.StaticAssignStatement;
import tokeniser.Token;
import tokeniser.TokenType;
import tokeniser.Tokeniser;

import java.util.Collections;
import java.util.List;

import static tokeniser.TokenType.*;

/**
 * Turns list of tokens into Abstract Syntax Tree (AST)
 */
public class Parser {
    /**
     * List of tokens to turn into AST
     */
    public final List<Token> tokens;

    /**
     * Current position within token list
     */
    private int pos = 0;

    public Parser(List<Token> tokens) {
        this.tokens = Collections.unmodifiableList(tokens);
    }

    public Parser(Tokeniser tokeniser) {
        tokeniser.tokenise();
        this.tokens = Collections.unmodifiableList(tokeniser.tokens());
    }

    /**
     * Tries to read an expression from token list. Currently only gets primitives
     */
    NodeExpr parseExpr() {
        Token t = peek();
        NodeExpr expr;

        switch (t.type) {
            case INT_LITERAL, HEX_LITERAL -> expr = new IntPrimitive(t);
            case CHAR_LITERAL -> expr = new CharPrimitive(t);
            case FLOAT_LITERAL -> expr = new FloatPrimitive(t);
            case BOOL_LITERAL -> expr = new BoolPrimitive(t);
            //case STR_LITERAL -> expr = new String Object Type; todo classes here as well lol
            case IDENTIFIER -> expr = new NodeIdentifier(t);
            case OPEN_PAREN -> {
                consume(); //consume the open parenthesis
                expr = parseExpr();
                if (peek().type != CLOSE_PAREN) //this should not happen
                    throw new ExpressionError("Misplaced parentheses after tokenisation?", peek());
            }
            default -> throw new ExpressionError("Unexpected token in expression", t);
        }
        consume();

        return expr;
    }

    Operator parseOperator() {
        Token t = peek();
        Operator op = null;


        return op;
    }

    public NodeProgram parse() {
        NodeProgram program = new NodeProgram();

        for (pos = 0; pos < tokens.size(); pos++) {
            Token t = peek();
            NodeStatement statement;
            if (t.type == EXIT) {
                consume();
                statement = new ExitStatement(parseExpr());
            } else if (t.type == PRIMITIVE_TYPE) {
                consume();
                if (!hasNext() || peek().type != IDENTIFIER)
                    throw new ExpressionError("Must have an identifier after '" + t.value + "'", hasNext() ? peek() : t);

                Token identifier = peek();
                consume();

                if (!hasNext() || peek().type != ASSIGNMENT_OPERATOR)
                    throw new ExpressionError("Expected an assignment after " + identifier.value, hasNext() ? peek() : identifier);

                Token assigner = peek();
                consume();

                statement = new StaticAssignStatement(t, new NodeIdentifier(identifier), assigner, parseExpr());

            } else if (t.type == LET) {
                consume();
                if (!hasNext() || peek().type != IDENTIFIER)
                    throw new ExpressionError("Must have an identifier after 'let'", hasNext() ? peek() : t);

                Token identifier = peek();
                consume();

                if (!hasNext() || peek().type != ASSIGNMENT_OPERATOR)
                    throw new ExpressionError("Expected an assignment after " + identifier.value, hasNext() ? peek() : identifier);

                Token assigner = peek();
                consume();

                statement = new AssignStatement(new NodeIdentifier(identifier), assigner, parseExpr());

            } else {
                throw new ExpressionError("Unknown statement", t);
            }

            if (!hasNext() || peek().type != SEMICOLON) { // Must end all statements with semicolon
                throw new ExpressionError("Must have ';' after statement", peek());
            }

            program.statements.add(statement);
        }

        return program;
    }

    boolean hasNext() {
        return hasNext(0);
    }

    boolean hasNext(int offset) {
        return pos + offset < tokens.size();
    }

    Token peek() {
        return peek(0);
    }

    Token peek(int offset) {
        if (!hasNext(offset)) return null;
        return tokens.get(pos + offset);
    }

    Token consume() {
        pos++;
        return peek();
    }
}
