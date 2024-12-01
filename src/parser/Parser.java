package parser;

import error.ExpressionError;
import parser.node.NodeExpr;
import parser.node.NodeProgram;
import parser.node.identifier.NodeIdentifier;
import parser.node.primitives.BoolPrimitive;
import parser.node.primitives.CharPrimitive;
import parser.node.primitives.FloatPrimitive;
import parser.node.primitives.IntPrimitive;
import parser.node.statement.DeclareStatement;
import parser.node.statement.ExitStatement;
import parser.node.statement.NodeStatement;
import parser.node.statement.StaticDeclareStatement;
import tokeniser.Token;
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

    public NodeProgram parse() {
        NodeProgram program = new NodeProgram();

        for (pos = 0; pos < tokens.size(); pos++) {
            Token t = peek();
            NodeStatement statement;
            if (t.type == EXIT) {
                consume(); //Consume exit token
                statement = new ExitStatement(t, parseExpr());
            } else if (t.type == PRIMITIVE_TYPE) {
                Token identifier = consume(); //Consuming primitive name

                if (identifier.type != IDENTIFIER)
                    throw new ExpressionError("Must have an identifier after '" + t.value + "'", identifier);

                Token declarer = consume(); //Consuming identifier

                if (peek().type != DECLARATION_OPERATION)
                    throw new ExpressionError("Expected a declaration after " + identifier.value, declarer);

                consume(); //Consuming declarer operation

                statement = new StaticDeclareStatement(t, new NodeIdentifier(identifier), declarer, parseExpr());

            } else if (t.type == LET) {
                Token identifier = consume(); //Consuming 'let' keyword

                if (identifier.type != IDENTIFIER)
                    throw new ExpressionError("Must have an identifier after 'let'", identifier);

                Token declarer = consume(); //Consuming identifier

                if (declarer.type != DECLARATION_OPERATION)
                    throw new ExpressionError("Expected a declaration after " + identifier.value, declarer);

                consume(); //Consuming declarer operation

                statement = new DeclareStatement(new NodeIdentifier(identifier), declarer, parseExpr());

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
