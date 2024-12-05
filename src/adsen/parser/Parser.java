package adsen.parser;

import adsen.error.ExpressionError;
import adsen.parser.node.NodeExpr;
import adsen.parser.node.NodeProgram;
import adsen.parser.node.identifier.NodeIdentifier;
import adsen.parser.node.operator.BinaryOperator;
import adsen.parser.node.operator.Operator;
import adsen.parser.node.operator.OperatorType;
import adsen.parser.node.operator.UnaryOperator;
import adsen.parser.node.primitives.BoolPrimitive;
import adsen.parser.node.primitives.CharPrimitive;
import adsen.parser.node.primitives.FloatPrimitive;
import adsen.parser.node.primitives.IntPrimitive;
import adsen.parser.node.statement.AssignStatement;
import adsen.parser.node.statement.DeclareStatement;
import adsen.parser.node.statement.ExitStatement;
import adsen.parser.node.statement.IfStatement;
import adsen.parser.node.statement.NodeStatement;
import adsen.parser.node.statement.ScopeStatement;
import adsen.parser.node.statement.StaticDeclareStatement;
import adsen.tokeniser.Token;
import adsen.tokeniser.TokenType;
import adsen.tokeniser.Tokeniser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import static adsen.Main.VERBOSE_FLAGS;
import static adsen.tokeniser.TokenType.*;

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

    public Parser(List<Token> tokens) {
        this.tokens = Collections.unmodifiableList(tokens);
    }

    /**
     * Tries to read an expression from {@link Parser#tokens} list
     */
    NodeExpr parseExpr() {
        Token t;

        List<Token> exprTokens = new ArrayList<>();

        for (t = peek(); hasNext() && isValidExprToken(peek().type); t = consume()) {
            exprTokens.add(t);
        }

        if (!hasNext())
            throw new ExpressionError("Expected ';' after expression", exprTokens.getLast());

        if (t.type != SEMICOLON)
            throw new ExpressionError("Expected ';' after expression", t);

        return parseExpr(exprTokens);
    }

    /**
     * Tries to read an expression from token list.
     */
    static NodeExpr parseExpr(List<Token> exprTokens) {
        Token t;
        NodeExpr expr;

        if (exprTokens.size() == 1) { //Shortcut for single expression
            t = exprTokens.getFirst();
            return switch (t.type) {
                case INT_LITERAL, HEX_LITERAL -> new IntPrimitive(t);
                case CHAR_LITERAL -> new CharPrimitive(t);
                case FLOAT_LITERAL -> new FloatPrimitive(t);
                case BOOL_LITERAL -> new BoolPrimitive(t);
                //case STR_LITERAL -> new String Object Type; gonna implement this as built-in complex type
                case IDENTIFIER -> new NodeIdentifier(t);

                default -> throw new ExpressionError("Unexpected token in expression", t);
            };
        }

        //Adapted from this StackOverflow thread
        //https://stackoverflow.com/questions/21356772/abstract-syntax-tree-using-the-shunting-yard-algorithm
        List<NodeExpr> postfix = new ArrayList<>(); //Might not be necessary, but keeping it anyway in case bugs occur
        Stack<Token> operatorStack = new Stack<>();
        Stack<NodeExpr> astStack = new Stack<>();

        Runnable processOperator = () -> {
            Operator lastOp;
            Token opTok = operatorStack.pop();
            OperatorType opType = Operator.operatorType.get(opTok.value);

            //todo handle not leftToRight operators
            if (opType.type == UNARY_OPERATOR) {
                NodeExpr arg = astStack.pop();
                lastOp = new UnaryOperator(opType, arg);
            } else if (opType.type == BINARY_OPERATOR) {

                NodeExpr rightArg = astStack.pop();
                NodeExpr leftArg = astStack.pop();

                lastOp = new BinaryOperator(leftArg, opType, rightArg);
            } else
                throw new ExpressionError("Don't know how we got here, found unknown operator type", opTok);

            postfix.add(lastOp);
            astStack.push(lastOp);
        };

        for (Token exprToken : exprTokens) {
            NodeExpr temp = switch (exprToken.type) {
                case INT_LITERAL, HEX_LITERAL -> new IntPrimitive(exprToken);
                case CHAR_LITERAL -> new CharPrimitive(exprToken);
                case FLOAT_LITERAL -> new FloatPrimitive(exprToken);
                case BOOL_LITERAL -> new BoolPrimitive(exprToken);
                case IDENTIFIER -> new NodeIdentifier(exprToken);
                default -> null;
            };

            if (temp != null) {
                postfix.add(temp);
                astStack.push(temp);
            } else if (exprToken.type == BINARY_OPERATOR || exprToken.type == UNARY_OPERATOR) {
                OperatorType opType = Operator.operatorType.get(exprToken.value);

                //Gotta check if this hackfix with OPEN_PAREN is gonna work or not
                while (!operatorStack.isEmpty() && operatorStack.peek().type != OPEN_PAREN && Operator.operatorType.get(operatorStack.peek().value).precedence >= opType.precedence) {
                    processOperator.run();
                }

                operatorStack.push(exprToken);
            } else if (exprToken.type == OPEN_PAREN) {
                operatorStack.push(exprToken);
            } else if (exprToken.type == CLOSE_PAREN) {
                if (operatorStack.empty()) throw new ExpressionError("Mismatched parentheses, but how?", exprToken);

                while (operatorStack.peek().type != OPEN_PAREN) {
                    processOperator.run();
                }
                operatorStack.pop(); //Popping the corresponding '('
            }
        }

        while (!operatorStack.isEmpty()) {
            processOperator.run();
        }

        if (astStack.size() > 1) //todo error messages which allow to get the whole expression code block
            throw new ExpressionError("Invalid expression", exprTokens.getFirst());

        expr = astStack.firstElement();

        return expr;
    }

    /**
     * Main parse function which returns the {@link NodeProgram} defining the entire program's AST
     */
    public NodeProgram parse() {
        NodeProgram program = new NodeProgram();

        program.statements = parse(0, tokens.size()); //Statement count will always be less than token list size

        return program;
    }

    public List<NodeStatement> parse(int startPos, int count) {
        List<NodeStatement> statements = new ArrayList<>();

        for (pos = startPos; pos < tokens.size() && statements.size() < count; pos++) {
            Token t = peek();
            NodeStatement statement;
            boolean needSemi = true; //used for if, else, while etc.

            statement = switch (t.type) {
                case EXIT -> { //Exit statement
                    consume(); //Consume exit token
                    yield new ExitStatement(t, parseExpr());
                }
                case PRIMITIVE_TYPE -> { //Static declaration
                    Token identifier = consume(); //Consuming primitive name

                    if (identifier.type != IDENTIFIER)
                        throw new ExpressionError("Must have an identifier after '" + t.value + "'", identifier);

                    Token declarer = consume(); //Consuming identifier

                    if (peek().type != DECLARATION_OPERATION)
                        throw new ExpressionError("Expected a declaration after " + identifier.value, declarer);

                    consume(); //Consuming declarer operation

                    yield new StaticDeclareStatement(t, new NodeIdentifier(identifier), declarer, parseExpr());
                }
                case LET -> { // Normal declaration
                    Token identifier = consume(); //Consuming 'let' keyword

                    if (identifier.type != IDENTIFIER)
                        throw new ExpressionError("Must have an identifier after 'let'", identifier);

                    Token declarer = consume(); //Consuming identifier

                    if (declarer.type != DECLARATION_OPERATION)
                        throw new ExpressionError("Expected a declaration after " + identifier.value, declarer);

                    consume(); //Consuming declarer operation

                    yield new DeclareStatement(new NodeIdentifier(identifier), declarer, parseExpr());
                }
                case IDENTIFIER -> { // Variable assignment

                    Token declarer = consume(); //Consuming identifier

                    if (declarer.type != DECLARATION_OPERATION) //Gonna add option for +=, -=, here eventually
                        throw new ExpressionError("Expected an assignment after " + t.value, declarer);

                    consume(); //Consuming assigner operation

                    yield new AssignStatement(new NodeIdentifier(t), declarer, parseExpr());
                }
                case C_OPEN_PAREN -> { //New scope
                    List<Token> scopeTokens = new ArrayList<>();
                    int c_bracket_counter = 1; //we have seen one open curly bracket

                    for (t = consume(); c_bracket_counter != 0 || t.type != C_CLOSE_PAREN; t = consume()) {
                        if (t.type == C_OPEN_PAREN) c_bracket_counter++;
                        if (peek(1).type == C_CLOSE_PAREN) c_bracket_counter--;
                        scopeTokens.add(t);
                    }

                    needSemi = false;
                    yield new ScopeStatement(new Parser(scopeTokens).parse().statements);
                }
                case IF -> {
                    Token ifT = t;
                    t = consume();
                    //todo replace with flag in parseExpr() to remove extra closed bracket instead
                    if (t.type != OPEN_PAREN) throw new ExpressionError("Must have condition after 'if'", t);
                    List<Token> conditionTokens = new ArrayList<>();
                    int bracket_counter = 1; //we have seen one open bracket

                    for (t = consume(); (bracket_counter != 0 || t.type != CLOSE_PAREN) && isValidExprToken(t.type); t = consume()) {
                        if (t.type == OPEN_PAREN) bracket_counter++;
                        if (peek(1).type == CLOSE_PAREN) bracket_counter--;
                        conditionTokens.add(t);
                    }

                    if (!isValidExprToken(t.type)) throw new ExpressionError("Invalid token", t);

                    NodeExpr ifExpr = parseExpr(conditionTokens);

                    needSemi = false; //don't need semicolon after the expression

                    NodeStatement thenStatement = parse(pos + 1, 1).getFirst();

                    t = peek(1); //don't consume unless needed

                    if (t.type == ELSE) { //Parsing else statement right here
                        NodeStatement elseStatement = parse(pos + 2, 1).getFirst();

                        yield new IfStatement(ifT, ifExpr, thenStatement, t, elseStatement);
                    }

                    yield new IfStatement(ifT, ifExpr, thenStatement);
                }
                default -> throw new ExpressionError("Unknown statement", t);
            };


            if (needSemi && !(hasNext() && peek().type == SEMICOLON)) { // Must end statements with semicolon
                throw new ExpressionError("Must have ';' after statement", peek());
            }

            statements.add(statement);
        }

        pos--;  // decrementing to the last valid token

        return statements;
    }

    //Preparing for shunting yard
    static boolean isValidExprToken(TokenType type) {
        return switch (type) {
            case LET, EXIT, IF, ELSE, SEMICOLON, C_OPEN_PAREN, CLOSE_PAREN -> false; //Simpler to go by exclusion, it seems
            default -> true;
        };
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
