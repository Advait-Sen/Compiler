package adsen.parser;

import adsen.error.ExpressionError;
import adsen.parser.node.expr.FuncCallExpr;
import adsen.parser.node.expr.NodeExpr;
import adsen.parser.node.NodeFunction;
import adsen.parser.node.NodeProgram;
import adsen.parser.node.expr.NodeIdentifier;
import adsen.parser.node.expr.operator.BinaryOperator;
import adsen.parser.node.expr.operator.Operator;
import adsen.parser.node.expr.operator.OperatorType;
import adsen.parser.node.expr.operator.UnaryOperator;
import adsen.parser.node.expr.primitives.BoolPrimitive;
import adsen.parser.node.expr.primitives.CharPrimitive;
import adsen.parser.node.expr.primitives.FloatPrimitive;
import adsen.parser.node.expr.primitives.IntPrimitive;
import adsen.parser.node.statement.AssignStatement;
import adsen.parser.node.statement.DeclareStatement;
import adsen.parser.node.statement.ExitStatement;
import adsen.parser.node.statement.ForStatement;
import adsen.parser.node.statement.FunctionCallStatement;
import adsen.parser.node.statement.IfStatement;
import adsen.parser.node.statement.IncrementStatement;
import adsen.parser.node.statement.NodeStatement;
import adsen.parser.node.statement.ReturnStatement;
import adsen.parser.node.statement.ScopeStatement;
import adsen.parser.node.statement.StaticDeclareStatement;
import adsen.parser.node.statement.WhileStatement;
import adsen.tokeniser.Token;
import adsen.tokeniser.Tokeniser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

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
     *
     * @param ignoreSemi Allows to ignore final semicolon if not required
     * @param endPos     End index until which to read tokens
     */
    NodeExpr parseExpr(boolean ignoreSemi, int endPos) {
        Token t;
        int tokens = endPos - pos;

        List<Token> exprTokens = new ArrayList<>();

        if (tokens == -1) {
            for (t = peek(); hasNext() && isValidExprToken(peek()); t = consume()) {
                exprTokens.add(t);
            }
        } else {
            t = peek();
            for (int i = 0; (i < tokens) && isValidExprToken(peek()); t = consume()) {
                exprTokens.add(t);
                i++;
            }
        }

        if (!ignoreSemi) {
            if (!hasNext())
                throw new ExpressionError("Expected ';' after expression", exprTokens.getLast());

            if (t.type != SEMICOLON)
                throw new ExpressionError("Expected ';' after expression", t);
        }

        return parseExpr(exprTokens);
    }

    /**
     * Tries to read an expression from token list.
     */
    static NodeExpr parseExpr(List<Token> exprTokens) {
        Token t;
        NodeExpr expr;

        if (exprTokens.size() == 1) { //Shortcut for short expressions
            t = exprTokens.getFirst();
            return switch (t.type) {
                case INT_LITERAL, HEX_LITERAL -> new IntPrimitive(t);
                case CHAR_LITERAL -> new CharPrimitive(t);
                case FLOAT_LITERAL -> new FloatPrimitive(t);
                case BOOL_LITERAL -> new BoolPrimitive(t);
                //case STR_LITERAL -> new String Object Type; gonna implement this as built-in complex type
                case VARIABLE -> new NodeIdentifier(t);

                default -> throw new ExpressionError("Unexpected token in expression", t);
            };
        }

        //Adapted Shunting Yard from this StackOverflow thread
        //https://stackoverflow.com/questions/21356772/abstract-syntax-tree-using-the-shunting-yard-algorithm
        //Adapted function handling from wikipedia: https://en.wikipedia.org/wiki/Shunting_yard_algorithm

        //My idea with this code is to have a simple yet complete implementation of shunting yard that is still
        // well-documented, with no steps skipped, so that I can come back to it later and understand how it works

        //List<NodeExpr> postfix = new ArrayList<>(); //Might not be necessary, but keeping it anyway in case bugs occur
        Stack<Token> operatorStack = new Stack<>();

        Stack<NodeExpr> astStack = new Stack<>();

        Runnable processOperator = () -> {
            Operator lastOp;
            Token opTok = operatorStack.pop();

            if (opTok.type == FUNCTION)
                throw new ExpressionError("Found a function token in the operator stack, it should have been dealt with already", opTok);

            OperatorType opType = Operator.operatorType.get(opTok.value);
            if (astStack.size() < opType.args)
                throw new ExpressionError("Too few arguments for operator '" + opTok.value + "'", opTok);
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

            //postfix.add(lastOp);
            astStack.push(lastOp);
        };

        for (int i = 0; i < exprTokens.size(); i++) {
            Token exprToken = exprTokens.get(i);

            NodeExpr temp = switch (exprToken.type) {
                case INT_LITERAL, HEX_LITERAL -> new IntPrimitive(exprToken);
                case CHAR_LITERAL -> new CharPrimitive(exprToken);
                case FLOAT_LITERAL -> new FloatPrimitive(exprToken);
                case BOOL_LITERAL -> new BoolPrimitive(exprToken);
                case VARIABLE -> new NodeIdentifier(exprToken);
                default -> null;
            };

            if (temp != null) { //If we hit a simple type, then that's it
                //postfix.add(temp);
                astStack.push(temp);
            } else if (exprToken.type == BINARY_OPERATOR || exprToken.type == UNARY_OPERATOR) {
                //If we hit an operator, first figure out what kind of operator it is
                OperatorType opType = Operator.operatorType.get(exprToken.value);

                //Then, we process all the operators currently in the operator stack with a precedence lower than this one
                while (!operatorStack.isEmpty() && operatorStack.peek().type != OPEN_PAREN && operatorStack.peek().type != FUNCTION && Operator.operatorType.get(operatorStack.peek().value).precedence >= opType.precedence) {
                    processOperator.run();
                }

                //Finally, we push this operator onto the stack
                operatorStack.push(exprToken);
            } else if (exprToken.type == FUNCTION) {
                //Calculate argcount for the function
                int args = 0;
                int parens = 1;//We already count the ( after the function name

                //We know the next token (i+1) will be an open parenthesis, and we also know that any mismatched
                // parentheses would have been caught by now, so there is at least a closed parenthesis after that
                //So we can safely query the token at (i+2)

                Token tok = exprTokens.get(i + 2);

                //If the very next token is a closed parenthesis, this is a 0-argument function
                if (tok.type != CLOSE_PAREN) {
                    args = 1; //We know there's at least one argument

                    //We start at i+3 because we were previously at i+2.
                    /*
                    The condition for the for loop is to see if we reach the matching closed parenthesis for the
                     opening parenthesis of the function, which also means that checking that we haven't reached the
                     end of exprTokens is unnecessary, since we can only get to this point if the function is opened
                     and closed properly. But having it doesn't hurt.
                     */

                    for (int j = i + 3; !(parens == 0 && tok.type == CLOSE_PAREN) && j < exprTokens.size(); j++) {
                        tok = exprTokens.get(j);
                        switch (tok.type) {
                            case OPEN_PAREN -> parens++;
                            case CLOSE_PAREN -> parens--;
                            case COMMA -> {
                                //In case of nested functions
                                if (parens == 1) args++;
                            }
                        }
                    }
                }

                //Encoding number of arguments in name of token so it can be safely extracted later when building AST stack
                exprToken.value += " " + args;

                operatorStack.push(exprToken);
            } else if (exprToken.type == OPEN_PAREN) {
                operatorStack.push(exprToken);
            } else if (exprToken.type == COMMA) {

                //If we hit a comma, process all operators until the last open parenthesis
                //This looks weird, but what it does is essentially every time we have completed an argument for a
                // function, we push it onto the AST stack so it can be handled later when the function comes along to
                // pick up its arguments, meaning every comma will always just see its argument and the opening parenthesis
                // in the operator stack
                while (!operatorStack.isEmpty() && operatorStack.peek().type != OPEN_PAREN) {
                    processOperator.run();
                }

            } else if (exprToken.type == CLOSE_PAREN) {
                if (operatorStack.empty()) throw new ExpressionError("Mismatched parentheses, but how?", exprToken);

                while (operatorStack.peek().type != OPEN_PAREN) {
                    processOperator.run();
                }
                operatorStack.pop(); //Popping the corresponding '(', which will be there due to while loop

                //Dealing with functions
                if (!operatorStack.isEmpty() && operatorStack.peek().type == FUNCTION) {
                    Token funTok = operatorStack.pop();
                    //Extracting argcount information from function token
                    String[] splitValue = funTok.value.split(" ");
                    funTok.value = splitValue[0];
                    int args = Integer.parseInt(splitValue[1]);
                    FuncCallExpr func = new FuncCallExpr(funTok, args);
                    //postfix.add(func);
                    //Popping function arguments from stack into function node expression
                    for (int j = 0; j < args; j++) {
                        func.addArgument(astStack.pop());
                    }
                    astStack.push(func);
                }
            }
        }

        while (!operatorStack.isEmpty()) {
            processOperator.run();
        }

        /* Printing out postfix
        System.out.println("Printing postfix:\n");
        for (NodeExpr nodeExpr : postfix) {
            System.out.println(nodeExpr.asString());
        }
        System.out.println("\nEnd of postfix\n");
        */

        if (astStack.size() > 1) //todo error messages which allow to get the whole expression code block
            throw new ExpressionError("Invalid expression", exprTokens.getFirst());

        expr = astStack.firstElement();

        return expr;
    }

    public NodeProgram parse() {
        NodeProgram program = new NodeProgram();

        for (pos = 0; pos < tokens.size(); pos++) {
            Token t = tokens.get(pos);

            switch (t.type) {
                // Anticipating complex types
                case VOID, PRIMITIVE_TYPE, COMPOUND_TYPE, CLASS_TYPE -> {
                    Token returnType = t;
                    Token functionName = consume();
                    if (functionName.type != FUNCTION)
                        throw new ExpressionError("Expected function declaration", functionName);

                    consume(); //This is the open parenthesis after the function name
                    List<Token> signature = new ArrayList<>();
                    for (t = consume(); t.type != CLOSE_PAREN; t = consume()) {

                        if (t.type == PRIMITIVE_TYPE || t.type == COMPOUND_TYPE || t.type == CLASS_TYPE) {
                            signature.add(t);
                            t = consume();

                            if (t.type == VARIABLE) {
                                signature.add(t);
                                t = consume();

                                if (t.type == CLOSE_PAREN) break; //Reached the end of the signature
                                if (t.type != COMMA) throw new ExpressionError("Expected ','", t);

                            } else throw new ExpressionError("Expected variable", t);

                        } else throw new ExpressionError("Expected type", t);

                    }

                    NodeFunction function = new NodeFunction(returnType, functionName, signature);

                    if (peek(1).type != C_OPEN_PAREN)
                        throw new ExpressionError("Expected '{' after function declaration", peek(1));

                    function.andThen(((ScopeStatement) parseOneStatement(pos + 1)).statements);

                    program.functions.put(function.name, function);
                }
                default -> throw new ExpressionError("Unexpected token: " + t, t);
            }
        }

        return program;
    }

    /**
     * Old parse function which returns the {@link NodeProgram} defining the entire program's AST.
     */
    public List<NodeStatement> parseStatements() {
        return parseStatements(0, tokens.size()); // Statement count will always be less than token list size
    }

    /**
     * Method which returns the next parsed statement, since it seems to be such a popular request
     */
    NodeStatement parseOneStatement(int startPos) {
        return parseStatements(startPos, 1).getFirst();
    }

    List<NodeStatement> parseStatements(int startPos, int statementCount) {
        return parseStatements(startPos, statementCount, tokens.size(), false); //By default, looking at all the tokens
    }

    List<NodeStatement> parseStatements(int startPos, int statementCount, int tokenCount, boolean ignoreSemi) {
        List<NodeStatement> statements = new ArrayList<>();
        int endPos = pos + tokenCount;
        for (pos = startPos; hasNext() && pos < endPos && statements.size() < statementCount; pos++) {
            Token t = peek();
            NodeStatement statement;
            //If we don't need the semicolon at the end, then don't look for it (eg. in for statement)
            boolean needSemi = !ignoreSemi; //used for if, else, while etc.

            statement = switch (t.type) {
                case EXIT -> { //Exit statement
                    consume(); //Consume exit token
                    yield new ExitStatement(t, parseExpr(ignoreSemi, endPos));
                }
                case RETURN -> { //Return statement
                    consume(); //Consume return token
                    yield new ReturnStatement(t, parseExpr(ignoreSemi, endPos));
                }
                case PRIMITIVE_TYPE -> { //Static declaration
                    Token identifier = consume(); //Consuming primitive name

                    if (identifier.type != VARIABLE)
                        throw new ExpressionError("Must have a variable name after '" + t.value + "'", identifier);

                    Token declarer = consume(); //Consuming identifier

                    if (peek().type != DECLARATION_OPERATION)
                        throw new ExpressionError("Expected a declaration after '" + identifier.value + "'", declarer);

                    consume(); //Consuming declarer operation

                    yield new StaticDeclareStatement(t, new NodeIdentifier(identifier), declarer, parseExpr(ignoreSemi, endPos));
                }
                case LET -> { // Normal declaration
                    Token identifier = consume(); //Consuming 'let' keyword

                    if (identifier.type != VARIABLE)
                        throw new ExpressionError("Must have an identifier after 'let'", identifier);

                    Token declarer = consume(); //Consuming identifier

                    if (declarer.type != DECLARATION_OPERATION)
                        throw new ExpressionError("Expected a declaration after '" + identifier.value + "'", declarer);

                    consume(); //Consuming declarer operation

                    yield new DeclareStatement(new NodeIdentifier(identifier), declarer, parseExpr(ignoreSemi, endPos));
                }
                case UNARY_OPERATOR -> {
                    OperatorType opType = Operator.operatorType.get(t.value);

                    if (opType != OperatorType.INCREMENT && opType != OperatorType.DECREMENT) {
                        throw new ExpressionError("Not a statement", t);
                    }

                    Token ident = consume(); //Consuming incrementor

                    if (ident.type != VARIABLE) {
                        throw new ExpressionError("Expected an identifier after " + t.value, ident);
                    }
                    consume(); //Consuming identifier

                    yield new IncrementStatement(new NodeIdentifier(ident), t, true);
                }
                case VARIABLE -> { // Variable assignment

                    Token declarer = consume(); //Consuming identifier

                    //Could be increment or decrement
                    //Checking that the next token is a semicolon (single statement) or closed parenthesis (for loop incrementer)
                    if (declarer.type == UNARY_OPERATOR && (peek(1).type == SEMICOLON || peek(1).type == CLOSE_PAREN)) {
                        consume();//Consuming incrementor
                        yield new IncrementStatement(new NodeIdentifier(t), declarer, false);
                    }

                    if (declarer.type != DECLARATION_OPERATION) //Gonna add option for +=, -=, here eventually
                        throw new ExpressionError("Expected an assignment after '" + t.value + "'", declarer);

                    consume(); //Consuming assigner operation

                    yield new AssignStatement(new NodeIdentifier(t), declarer, parseExpr(ignoreSemi, endPos));
                }
                case C_OPEN_PAREN -> { //New scope
                    needSemi = false;

                    if (peek(1).type == C_CLOSE_PAREN) { //Special case for empty scope
                        consume();//consuming closed curly bracket
                        yield new ScopeStatement(Collections.emptyList());
                    }

                    List<Token> scopeTokens = new ArrayList<>();
                    int c_bracket_counter = 1; //we have seen one open curly bracket

                    for (t = consume(); c_bracket_counter != 0 || t.type != C_CLOSE_PAREN; t = consume()) {
                        if (t.type == C_OPEN_PAREN) c_bracket_counter++;
                        if (peek(1).type == C_CLOSE_PAREN) c_bracket_counter--;
                        scopeTokens.add(t);
                    }
                    //todo check if this causes errors when creating error messages
                    yield new ScopeStatement(new Parser(scopeTokens).parseStatements(0, tokens.size()));
                }
                case IF -> {
                    Token ifT = t;
                    t = consume();
                    if (t.type != OPEN_PAREN) throw new ExpressionError("Must have condition after if", t);
                    List<Token> conditionTokens = new ArrayList<>();
                    int bracket_counter = 1; //we have seen one open bracket

                    for (t = consume(); (bracket_counter != 0 || t.type != CLOSE_PAREN) && isValidExprToken(t); t = consume()) {
                        if (t.type == OPEN_PAREN) bracket_counter++;
                        if (peek(1).type == CLOSE_PAREN) bracket_counter--;
                        conditionTokens.add(t);
                    }

                    if (!isValidExprToken(t)) throw new ExpressionError("Invalid token", t);

                    NodeExpr ifExpr = parseExpr(conditionTokens);

                    needSemi = false; //don't need semicolon after the expression

                    NodeStatement thenStatement = parseOneStatement(pos + 1);

                    t = peek(1); //don't consume unless needed

                    if (t.type == ELSE) { //Parsing else statement right here
                        NodeStatement elseStatement = parseOneStatement(pos + 2);

                        yield new IfStatement(ifT, ifExpr, thenStatement, t, elseStatement);
                    }

                    yield new IfStatement(ifT, ifExpr, thenStatement);
                }
                case WHILE -> {
                    Token whileT = t;
                    t = consume();
                    if (t.type != OPEN_PAREN) throw new ExpressionError("Must have condition after while", t);
                    List<Token> conditionTokens = new ArrayList<>();
                    int bracket_counter = 1; //we have seen one open bracket

                    for (t = consume(); (bracket_counter != 0 || t.type != CLOSE_PAREN) && isValidExprToken(t); t = consume()) {
                        if (t.type == OPEN_PAREN) bracket_counter++;
                        if (peek(1).type == CLOSE_PAREN) bracket_counter--;
                        conditionTokens.add(t);
                    }

                    if (!isValidExprToken(t)) throw new ExpressionError("Invalid token", t);

                    NodeExpr whileCondition = parseExpr(conditionTokens);

                    needSemi = false; //don't need semicolon after the expression

                    NodeStatement executionStatement = parseOneStatement(pos + 1);

                    yield new WhileStatement(whileT, whileCondition, executionStatement);
                }
                case FOR -> { //Todo add proper checks for assigner and incrementer being assignment statements
                    Token forT = t;
                    t = consume();
                    if (t.type != OPEN_PAREN) throw new ExpressionError("Expected '(' after for", t);
                    NodeStatement assigner = parseOneStatement(pos + 1);
                    List<Token> conditionTokens = new ArrayList<>();

                    for (t = consume(); isValidExprToken(t); t = consume()) {
                        conditionTokens.add(t);
                    }

                    if (t.type != SEMICOLON) throw new ExpressionError("Invalid token", t);

                    NodeExpr forCondition = parseExpr(conditionTokens);

                    int closed_bracket_pos;
                    int bracket_counter = 1; //we have seen one open bracket

                    for (closed_bracket_pos = 1; bracket_counter != 0; closed_bracket_pos++) {
                        if (peek(closed_bracket_pos).type == OPEN_PAREN) bracket_counter++;
                        if (peek(closed_bracket_pos + 1).type == CLOSE_PAREN) bracket_counter--;
                        conditionTokens.add(t);
                    }

                    NodeStatement incrementer = parseStatements(pos + 1, 1, closed_bracket_pos, true).getFirst();

                    needSemi = false; //don't need semicolon after the expression

                    NodeStatement executionStatement = parseOneStatement(pos + 1);

                    yield new ForStatement(forT, assigner, incrementer, forCondition, executionStatement);
                }
                case FUNCTION -> { //Function call
                    Token fCallTok = t;
                    t = consume();

                    if (t.type != OPEN_PAREN)
                        throw new ExpressionError("Expected '(' after '" + fCallTok.value + "'", t);

                    int parens = 1;
                    List<Token> tempExpr = new ArrayList<>();
                    List<NodeExpr> args = new ArrayList<>();


                    while (t.type != CLOSE_PAREN) { //Grab tokens into args separated by commas

                        t = consume(); //Consuming the open parenthesis, and subsequent commas

                        while (!(parens == 1 && (t.type == COMMA || t.type == CLOSE_PAREN))) {

                            if (t.type == OPEN_PAREN) parens++;
                            if (t.type == CLOSE_PAREN) parens--;

                            if (parens == 0) throw new ExpressionError("Unexpected ')'", t);

                            tempExpr.add(t);
                            t = consume();
                        }
                        // If no tokens have been found for the expression, then this might be a function with 0 args
                        // But if we already have args, then this is an error
                        if (tempExpr.isEmpty()) {
                            if (args.isEmpty() && t.type == CLOSE_PAREN) { //Function with 0 args
                                break;
                            } else {
                                throw new ExpressionError("Expected function argument", t);
                            }
                        }
                        args.add(parseExpr(tempExpr));
                        tempExpr.clear();
                    }

                    consume(); //Consuming closed parenthesis

                    yield new FunctionCallStatement(fCallTok, args);
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
    static boolean isValidExprToken(Token token) {
        return switch (token.type) {
            case LET, EXIT, IF, ELSE, SEMICOLON, C_OPEN_PAREN, C_CLOSE_PAREN, WHILE, FOR ->
                    false; //Simpler to go by exclusion, it seems
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
