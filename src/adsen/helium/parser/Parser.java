package adsen.helium.parser;

import adsen.helium.error.ExpressionError;
import adsen.helium.exec.imports.ImportHandler;
import adsen.helium.parser.expr.NodeExpr;
import adsen.helium.parser.expr.NodeIdentifier;
import adsen.helium.parser.expr.operator.Operator;
import adsen.helium.parser.expr.operator.OperatorType;
import adsen.helium.parser.statement.AssignStatement;
import adsen.helium.parser.statement.BreakStatement;
import adsen.helium.parser.statement.ContinueStatement;
import adsen.helium.parser.statement.DeclareStatement;
import adsen.helium.parser.statement.ExitStatement;
import adsen.helium.parser.statement.ForStatement;
import adsen.helium.parser.statement.FunctionCallStatement;
import adsen.helium.parser.statement.HeliumStatement;
import adsen.helium.parser.statement.IfStatement;
import adsen.helium.parser.statement.IncrementStatement;
import adsen.helium.parser.statement.ReturnStatement;
import adsen.helium.parser.statement.ScopeStatement;
import adsen.helium.parser.statement.StaticDeclareStatement;
import adsen.helium.parser.statement.WhileStatement;
import adsen.parser.statement.*;
import adsen.helium.tokeniser.Token;
import adsen.helium.tokeniser.Tokeniser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.function.Function;

import static adsen.helium.Helium.VERBOSE_FLAGS;
import static adsen.helium.tokeniser.TokenType.*;

/**
 * Turns list of tokens into Abstract Syntax Tree (AST)
 */
public class Parser {

    /**
     * The import handler for this program.
     * IDK if this should go here, but can't think of any other place to put it, so this will do for now
     */
    public static ImportHandler IMPORT_HANDLER;

    /**
     * The path to the directory containing the main file. This is used for imports.
     * Like {@link Parser#IMPORT_HANDLER}, this might also be moved elsewhere in the future
     */
    public static Path ROOT_DIRECTORY;


    /**
     * List of tokens to turn into AST
     */
    public final List<Token> tokens;

    /**
     * The program for which this is parsing
     */
    public final HeliumProgram program;

    /**
     * Experimental new way of storing statements in order to simplify and optimise scope creation.
     * TODO add helper functions to access top of stack later on when code gets repetitive
     */
    private final Stack<ParserScope> parserScopes = new Stack<>();

    /**
     * Current position within token list
     */
    private int pos = 0;

    public Parser(HeliumProgram program, Tokeniser tokeniser) {
        this.program = program;
        tokeniser.tokenise();
        this.tokens = Collections.unmodifiableList(tokeniser.tokens());
    }

    NodeExpr parseExpr() {
        return parseExpr(false);
    }

    /**
     * Gathers tokens for a possible expression, before sending them off to {@link ShuntingYard#parseExpr} to get
     * evaluated into a {@link NodeExpr}
     *
     * @param inBrackets Whether the expression is enclosed in a pair of brackets (for an if or while statement)
     */
    NodeExpr parseExpr(boolean inBrackets) {
        List<Token> exprTokens = new ArrayList<>();

        Token t = peek();
        int depth = 0;

        if (inBrackets) {
            if (t.type != OPEN_PAREN) throw new ExpressionError("Expected '('", t);
        }

        for (; hasNext() && peek().isValidExprToken() && !(inBrackets && depth == 1 && t.type == CLOSE_PAREN); t = consume()) {
            if (t.type == OPEN_PAREN) depth++;
            if (t.type == CLOSE_PAREN) depth--;
            exprTokens.add(t);
        }

        if (inBrackets) {
            if (t.type != CLOSE_PAREN) throw new ExpressionError("Expected ')' after expression", t);

            // Adding final closed bracket to the expression
            // Not checking for depth not being 1, since we already know that parentheses are matched by this point
            if (depth == 1) exprTokens.add(t);

        } else if (t.type != SEMICOLON) throw new ExpressionError("Expected ';' after expression", t);

        //This is only acceptable with an empty return statement, which is a case we handle before reaching this point
        if (exprTokens.isEmpty()) throw new ExpressionError("Tried to parse empty expression", t);

        return ShuntingYard.parseExpr(exprTokens);
    }

    public void parse() {
        boolean hasImports = false;
        boolean importsFinished = false;

        List<List<Token>> imports = new ArrayList<>();

        for (pos = 0; pos < tokens.size(); pos++) {
            Token t = tokens.get(pos);

            switch (t.type) {
                case IMPORT -> {
                    hasImports = true;
                    // If we've seen imports after function declarations
                    if (importsFinished) {
                        throw new ExpressionError("Unexpected import, should have occurred earlier", t);
                    }

                    List<Token> importLocation = new ArrayList<>();

                    importLocation.add(t);

                    for (Token next = consume(); next.type != SEMICOLON; next = consume()) {

                        if (next.isValidImportToken()) {
                            importLocation.add(next);
                        } else {
                            throw new ExpressionError("Unexpected token in import", next);
                        }

                        next = consume();

                        //If we reached the end, break
                        if (next.type == SEMICOLON) break;

                        if (next.type != BINARY_OPERATOR || !next.value.equals("/")) {
                            throw new ExpressionError("Improper import format, expected '/' here ", next);
                        }
                    }

                    imports.add(importLocation);
                }

                // Anticipating complex types
                case VOID, PRIMITIVE_TYPE, COMPOUND_TYPE, CLASS_TYPE -> {

                    // We just finished the imports
                    if (hasImports && !importsFinished) {
                        IMPORT_HANDLER.acceptImports(imports);
                    }

                    importsFinished = true;

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
                            //This might have to go, or maybe just change, once classes become a thing
                        } else throw new ExpressionError("Expected type", t);

                    }

                    HeliumFunction function = new HeliumFunction(returnType, functionName, signature);

                    if (peek(1).type != C_OPEN_PAREN)
                        throw new ExpressionError("Expected '{' after function declaration", peek(1));

                    parserScopes.push(new ParserScope());
                    function.andThen(parseFunction(pos + 1));
                    parserScopes.pop();
                    program.addFunction(function);
                }
                default -> throw new ExpressionError("Unexpected token: " + t, t);
            }
        }

        //TODO figure out better place for these, possibly in HeliumProgram
        IMPORT_HANDLER.loadNativeImportData();
        IMPORT_HANDLER.loadImportData();
    }

    List<HeliumStatement> parseFunction(int startPos) {

        //Empty case is to ensure we only grab one statement from a function (a scope statement whose contents we steal)
        for (pos = startPos; hasNext() && parserScopes.firstElement().statements.isEmpty(); pos++) {
            Token t = peek();

            //If we don't need the semicolon at the end, then don't look for it (e.g. in for statement incrementer)
            boolean needSemi = true; //used for if, else, while, etc.

            // Consuming token to jump to the next one, since all statements need it
            Token next = consume();

            HeliumStatement statement = switch (t.type) {
                case EXIT -> new ExitStatement(t, parseExpr());

                case RETURN -> { //Return statement
                    if (peek().type == SEMICOLON)
                        yield new ReturnStatement(t, null);

                    else yield new ReturnStatement(t, parseExpr());
                }

                case CONTINUE -> new ContinueStatement(t);

                case BREAK -> new BreakStatement(t);

                case PRIMITIVE_TYPE -> { //Static declaration
                    // 'next' holds the name of the identifier

                    if (next.type != VARIABLE)
                        throw new ExpressionError("Must have a variable name after '" + t.value + "'", next);

                    Token declarer = consume(); //Consuming identifier

                    if (peek().type != DECLARATION_OPERATION)
                        throw new ExpressionError("Expected a declaration after '" + next.value + "'", declarer);

                    consume(); //Consuming declarer operation

                    yield new StaticDeclareStatement(t, new NodeIdentifier(next), declarer, parseExpr());
                }
                case LET -> { // Normal declaration
                    // 'next' holds the name of the identifier

                    if (next.type != VARIABLE)
                        throw new ExpressionError("Must have an identifier after 'let'", next);

                    Token declarer = consume(); //Consuming identifier

                    if (declarer.type != DECLARATION_OPERATION)
                        throw new ExpressionError("Expected a declaration after '" + next.value + "'", declarer);

                    consume(); //Consuming declarer operation

                    yield new DeclareStatement(new NodeIdentifier(next), declarer, parseExpr());
                }
                case UNARY_OPERATOR -> {
                    OperatorType opType = Operator.operatorType.get(t.value);

                    if (opType != OperatorType.INCREMENT && opType != OperatorType.DECREMENT) {
                        throw new ExpressionError("Not a statement", t);
                    }

                    // 'next' holds the name of the identifier

                    if (next.type != VARIABLE) {
                        throw new ExpressionError("Expected a variable after " + t.value, next);
                    }
                    consume(); //Consuming identifier

                    yield new IncrementStatement(new NodeIdentifier(next), t, true);
                }
                case VARIABLE -> { // Variable assignment
                    // 'next' holds the name of the declaration operation or incrementer

                    //Could be increment or decrement
                    //Checking that the next token is a semicolon (single statement) or closed parenthesis (for loop incrementer)
                    if (next.type == UNARY_OPERATOR && (peek(1).type == SEMICOLON || peek(1).type == CLOSE_PAREN)) {
                        consume();//Consuming incrementor
                        yield new IncrementStatement(new NodeIdentifier(t), next, false);
                    }

                    if (next.type != DECLARATION_OPERATION) //Gonna add option for +=, -=, here eventually
                        throw new ExpressionError("Expected an assignment after '" + t.value + "'", next);

                    consume(); //Consuming assigner operation

                    yield new AssignStatement(new NodeIdentifier(t), next, parseExpr());
                }
                case C_OPEN_PAREN -> { //New scope
                    pos--; // Counteracting the consume() that we did right before the switch statement since we don't need it
                    needSemi = false;

                    //From now on we will read statements into the new scope
                    parserScopes.push(new ParserScope());

                    yield null;
                }

                case C_CLOSE_PAREN -> {
                    pos--; // Counteracting the consume() that we did right before the switch statement since we don't need it
                    needSemi = false;
                    ParserScope popped = parserScopes.pop();
                    if (!popped.statementRequests.isEmpty()) {
                        throw new ExpressionError("Still had statement requests in scope", t);
                    }

                    yield new ScopeStatement(popped.statements, t);
                }

                case IF -> {
                    Token ifT = t;

                    NodeExpr ifExpr = parseExpr(true);

                    needSemi = false; //don't need semicolon after the expression

                    addRequest((thenStatement) -> new IfStatement(ifT, ifExpr, thenStatement));

                    yield null;
                }
                case ELSE -> {
                    pos--; // Counteracting the consume() that we did right before the switch statement since we don't need it
                    if (scope().statements.getLast() instanceof IfStatement ifStmt) {
                        scope().statements.removeLast();//todo allow to avoid removing and re-adding same element
                        Token elseToken = t;
                        addRequest((elseStatement) -> {
                            ifStmt.addElse(elseToken, elseStatement);
                            return ifStmt;
                        });
                        needSemi = false;
                    } else {
                        throw new ExpressionError("Must have an if preceding else statement", t);
                    }
                    yield null;
                }

                case WHILE -> {
                    Token whileT = t;

                    NodeExpr whileCondition = parseExpr(true);

                    needSemi = false; //don't need semicolon after the expression

                    addRequest((executionStatement) -> {
                        if (executionStatement instanceof ScopeStatement scope) {
                            scope.setLoop();
                        }

                        return new WhileStatement(whileT, whileCondition, executionStatement);
                    });

                    yield null;
                }

                case FOR -> {
                    Token forT = t;
                    t = next;
                    if (t.type != OPEN_PAREN) throw new ExpressionError("Expected '(' after for", t);

                    //Todo add proper checks for assigner and incrementer being assignment statements
                    //LOL can't even remember what this to-do was about, but I'm afraid to remove it

                    StatementRequest assignerRequest = StatementRequest.get((assigner) -> {
                        if (!(assigner instanceof AssignStatement || assigner instanceof FunctionCallStatement || assigner instanceof DeclareStatement)) {
                            throw new ExpressionError("Invalid assigner expression in for statement", assigner.primaryToken());
                        }

                        Token _t = peek();
                        if (_t.type != SEMICOLON) throw new ExpressionError("Expected ';' after expression", _t);

                        consume(); //consuming semicolon

                        NodeExpr forCondition = parseExpr();

                        return new ForStatement(forT, assigner, forCondition);
                    });

                    StatementRequest incrementerRequest = StatementRequest.withoutSemi((incrementer) -> {
                        HeliumStatement stmt;
                        if ((stmt = scope().statements.getLast()) instanceof ForStatement) {
                            scope().statements.removeLast();
                            if (!(incrementer instanceof AssignStatement || incrementer instanceof FunctionCallStatement || incrementer instanceof DeclareStatement)) {
                                throw new ExpressionError("Invalid incrementer expression in for statement", stmt.primaryToken());
                            }
                            ((ForStatement) stmt).addIncrementer(incrementer);

                            return stmt;
                        } else {
                            throw new ExpressionError("Expected for statement", stmt.primaryToken());
                        }
                    });

                    StatementRequest executionStatementRequest = StatementRequest.get((execStatement) -> {
                        HeliumStatement stmt;
                        if ((stmt = scope().statements.getLast()) instanceof ForStatement) {
                            scope().statements.removeLast();
                            if (execStatement instanceof ScopeStatement scope) {
                                scope.setLoop();
                            }
                            ((ForStatement) stmt).addStatement(execStatement);
                            return stmt;
                        } else {
                            throw new ExpressionError("Expected for statement", stmt.primaryToken());
                        }
                    });

                    needSemi = false; //don't need semicolon after the expression

                    //Push them in reverse order, since top of the stack is read first
                    addRequest(executionStatementRequest);
                    addRequest(incrementerRequest);
                    addRequest(assignerRequest);

                    yield null;
                }

                case FUNCTION -> { //Function call
                    Token fCallTok = t;
                    t = next;

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
                        args.add(ShuntingYard.parseExpr(tempExpr));
                        tempExpr.clear();
                    }

                    consume(); //Consuming closed parenthesis

                    yield new FunctionCallStatement(fCallTok, args);
                }
                default -> throw new ExpressionError("Unknown statement", t);
            };

            // Must end statements with semicolon
            if (needSemi && !(hasNext() && peek().type == SEMICOLON) && (!scope().statementRequests.isEmpty() && scope().statementRequests.peek().needSemi)) {
                throw new ExpressionError("Must have ';' after statement", peek());
            }

            if (statement != null) {
                if (VERBOSE_FLAGS.contains("parser"))
                    System.out.println("Formed a statement: " + statement.asString() + " at scope depth: " + parserScopes.size());

                //If there are no if, while, etc. that want a statement
                if (scope().statementRequests.isEmpty() && VERBOSE_FLAGS.contains("parser")) {
                    System.out.println("That was directly added onto the statement stack\n");
                } else {
                    statement = scope().statementRequests.pop().apply(statement);
                    if (VERBOSE_FLAGS.contains("parser"))
                        System.out.println("That was consumed by a request, producing: " + statement.asString() + "\n");
                }
                scope().statements.add(statement);
            }
        }

        pos--;  // decrementing to the last valid token

        //Finishing up the function

        HeliumStatement funcScope;
        ParserScope firstScope;
        if (parserScopes.isEmpty()) {
            throw new ExpressionError("Empty parser scope, how did we get here?", null);
        } else if (parserScopes.size() > 1) {

            throw new ExpressionError("Didn't pop scopes correctly", scope().statements.getFirst().primaryToken());
        } else if ((firstScope = scope()).statements.isEmpty()) {

            throw new ExpressionError("Didn't close function properly", tokens.getLast());

        } else if (firstScope.statements.size() > 1) {

            throw new ExpressionError("Too many statements, impossible to reach this point", null);

        } else if (!((funcScope = firstScope.statements.getFirst()) instanceof ScopeStatement)) {
            throw new ExpressionError("Incorrect function declaration", funcScope.primaryToken());

        } else {
            return ((ScopeStatement) funcScope).statements;
        }
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

    ParserScope scope() {
        return parserScopes.peek();
    }

    void addRequest(StatementRequest request) {
        scope().statementRequests.push(request);
    }

    void addRequest(Function<HeliumStatement, HeliumStatement> request) {
        addRequest(StatementRequest.get(request));
    }
}

class ParserScope {
    public final List<HeliumStatement> statements = new ArrayList<>();
    public final Stack<StatementRequest> statementRequests = new Stack<>();
}


class StatementRequest {
    final Function<HeliumStatement, HeliumStatement> request;
    final boolean needSemi;

    private StatementRequest(boolean needSemi, Function<HeliumStatement, HeliumStatement> request) {
        this.needSemi = needSemi;
        this.request = request;
    }

    public static StatementRequest get(Function<HeliumStatement, HeliumStatement> request) {
        return new StatementRequest(true, request);
    }

    public static StatementRequest withoutSemi(Function<HeliumStatement, HeliumStatement> request) {
        return new StatementRequest(false, request);
    }

    HeliumStatement apply(HeliumStatement statement) {
        return request.apply(statement);
    }
}