package adsen.parser;

import adsen.error.ExpressionError;
import adsen.exec.imports.ImportHandler;
import adsen.parser.expr.NodeExpr;
import adsen.parser.expr.NodeIdentifier;
import adsen.parser.expr.operator.Operator;
import adsen.parser.expr.operator.OperatorType;
import adsen.parser.statement.AssignStatement;
import adsen.parser.statement.BreakStatement;
import adsen.parser.statement.ContinueStatement;
import adsen.parser.statement.DeclareStatement;
import adsen.parser.statement.ExitStatement;
import adsen.parser.statement.ForStatement;
import adsen.parser.statement.FunctionCallStatement;
import adsen.parser.statement.IfStatement;
import adsen.parser.statement.IncrementStatement;
import adsen.parser.statement.Statement;
import adsen.parser.statement.ReturnStatement;
import adsen.parser.statement.ScopeStatement;
import adsen.parser.statement.StaticDeclareStatement;
import adsen.parser.statement.WhileStatement;
import adsen.tokeniser.Keywords;
import adsen.tokeniser.Token;
import adsen.tokeniser.Tokeniser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.function.Function;

import static adsen.tokeniser.TokenType.*;

/**
 * Turns list of tokens into Abstract Syntax Tree (AST)
 */
public class Parser {

    /**
     * The import handler for this program.
     * Idk if this should go here, but can't think of any other place to put it, so this will do for now
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
     * TODO add helper functions to access top of stack later on when code gets repetetive
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

    private Parser(HeliumProgram program, List<Token> tokens) {
        this.program = program;
        this.tokens = Collections.unmodifiableList(tokens);
    }

    private Parser newFromTokenList(List<Token> tokens) {
        return new Parser(program, tokens);
    }


    /**
     * Gathers tokens for a possible expression, before sending them off to {@link ShuntingYard#parseExpr} to get
     * evaluated into a {@link NodeExpr}
     *
     * @param ignoreSemi Allows to ignore final semicolon if not required
     * @param endPos     End index until which to read tokens
     */
    NodeExpr parseExpr(boolean ignoreSemi, int endPos) {
        Token t;
        int tokens = endPos - pos;

        List<Token> exprTokens = new ArrayList<>();

        //TODO figure out when this happens
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

                        if (isValidImportToken(next)) {
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
                    parseFunction(pos + 1);
                    function.andThen(parserScopes.pop().statements);

                    program.addFunction(function);
                }
                default -> throw new ExpressionError("Unexpected token: " + t, t);
            }
        }

        //TODO figure out better place for these, possibly in HeliumProgram
        IMPORT_HANDLER.loadNativeImportData();
        IMPORT_HANDLER.loadImportData();
    }

    /**
     * Old parse function which returns the {@link HeliumProgram} defining the entire program's AST.
     */
    @Deprecated
    public List<Statement> parseStatements() {
        return null;
    }

    List<Statement> parseFunction(int startPos) {

        //IDK maybe remove these
        int tokenCount = tokens.size(), statementCount = tokens.size();

        //For when we're evaluating only a certain number of tokens at a time
        int endPos = pos + tokenCount;


        //Empty case is for the beginning of functions
        for (pos = startPos; hasNext() && pos < endPos && parserScopes.firstElement().statements.isEmpty(); pos++) {
            Token t = peek();
            Statement statement;
            //If we don't need the semicolon at the end, then don't look for it (eg. in for statement incrementer)
            boolean needSemi = true; //used for if, else, while etc.

            statement = switch (t.type) {
                case EXIT -> { //Exit statement
                    consume(); //Consume exit token
                    yield new ExitStatement(t, parseExpr(false, endPos));
                }
                case RETURN -> { //Return statement
                    consume(); //Consume return token
                    if (peek().type == SEMICOLON)
                        yield new ReturnStatement(t, null);

                    else yield new ReturnStatement(t, parseExpr(false, endPos));
                }

                case CONTINUE -> { //Continue statement
                    consume();
                    yield new ContinueStatement(t);
                }

                case BREAK -> { //Break statement
                    consume();
                    yield new BreakStatement(t);
                }

                case PRIMITIVE_TYPE -> { //Static declaration
                    Token identifier = consume(); //Consuming primitive name

                    if (identifier.type != VARIABLE)
                        throw new ExpressionError("Must have a variable name after '" + t.value + "'", identifier);

                    Token declarer = consume(); //Consuming identifier

                    if (peek().type != DECLARATION_OPERATION)
                        throw new ExpressionError("Expected a declaration after '" + identifier.value + "'", declarer);

                    consume(); //Consuming declarer operation

                    yield new StaticDeclareStatement(t, new NodeIdentifier(identifier), declarer, parseExpr(false, endPos));
                }
                case LET -> { // Normal declaration
                    Token identifier = consume(); //Consuming 'let' keyword

                    if (identifier.type != VARIABLE)
                        throw new ExpressionError("Must have an identifier after 'let'", identifier);

                    Token declarer = consume(); //Consuming identifier

                    if (declarer.type != DECLARATION_OPERATION)
                        throw new ExpressionError("Expected a declaration after '" + identifier.value + "'", declarer);

                    consume(); //Consuming declarer operation

                    yield new DeclareStatement(new NodeIdentifier(identifier), declarer, parseExpr(false, endPos));
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

                    yield new AssignStatement(new NodeIdentifier(t), declarer, parseExpr(false, endPos));
                }
                case C_OPEN_PAREN -> { //New scope
                    needSemi = false;

                    /*
                    TODO Use scope depth int variable to keep track of scopes, instead of reading through the entire scope's tokens first
                    That would require major refactoring of this class, but it's the only way to scale this project and
                    actually allow for large Helium files to exist
                    The idea is that instead of tallying up the tokens in a scope and sending them off to a new Parser
                    object to be processed, we simply increment a curly bracket counter here. Increment it each time we
                    see an open curly bracket, close it each time we close one, etc.
                    And we also store the statements by their layer, probably in a Stack, but idk exactly.
                    So when we find a curly bracket, we can take all the preceding statements until the previous curly
                    bracket, then enclose them into a scope, and stick that onto the statements list for the previous
                    scope depth / open curly bracket.
                    Scope depth would be initialised to 0 at the beginning of a function, and then checked to see if it's
                    still 0 at the end. If it isn't then that'll be a fun headache, either for me or the programmer, idk.

                    Maybe use a Stack<List<NodeStatement>>, where each time we push a new List onto the stack, then always
                    add to the list at the top of the stack. Then we pop it off, insert it into a new ScopeStatement, and
                    shove that onto the list that's now at the top of the stack.
                    So instead of having an int declaring depth, we combine depth-checking with adding statements.
                    I'm a genius!
                    */
                    //Everything from now on will be statements in the new scope
                    parserScopes.push(new ParserScope());

                    System.out.println("Created new scope (" + parserScopes.size() + ")");

                    yield null;
                }

                case C_CLOSE_PAREN -> {
                    System.out.println("Closed scope (" + parserScopes.size() + ")");
                    needSemi = false;
                    ParserScope popped = parserScopes.pop();
                    if (!popped.statementRequests.isEmpty()) {
                        throw new ExpressionError("Still had statement requests in scope", t);
                    }
                    List<Statement> scopeStatments = popped.statements;

                    yield new ScopeStatement(scopeStatments);
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

                    //This means we broke out of the loop due to a bad token
                    if (!isValidExprToken(t)) throw new ExpressionError("Invalid token", t);

                    NodeExpr ifExpr = ShuntingYard.parseExpr(conditionTokens);

                    needSemi = false; //don't need semicolon after the expression

                    Function<Statement, Statement> thenGetter = (thenStatement) -> {
                        return new IfStatement(ifT, ifExpr, thenStatement);
                    };
                    parserScopes.peek().statementRequests.push(thenGetter);

                    System.out.println("Pushed an if onto the request stack");

                    yield null;
                }
                case ELSE -> {
                    if (parserScopes.peek().statements.getLast() instanceof IfStatement ifStmt) {
                        parserScopes.peek().statements.removeLast();
                        Token elseToken = t;
                        Function<Statement, Statement> elseGetter = (elseStatement) -> {
                            return new IfStatement(ifStmt.token, ifStmt.getCondition(), ifStmt.thenStatement(), elseToken, elseStatement);
                        };
                        parserScopes.peek().statementRequests.push(elseGetter);
                    } else {
                        throw new ExpressionError("Must have an if preceding else statement", t);
                    }
                    yield null;
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

                    //This means we broke out of the loop due to a bad token
                    if (!isValidExprToken(t)) throw new ExpressionError("Invalid token", t);

                    NodeExpr whileCondition = ShuntingYard.parseExpr(conditionTokens);

                    needSemi = false; //don't need semicolon after the expression

                    Function<Statement, Statement> whileRequest = (executionStatement) -> {
                        if (executionStatement instanceof ScopeStatement scope) {
                            scope.setLoop();
                        }

                        return new WhileStatement(whileT, whileCondition, executionStatement);
                    };

                    parserScopes.peek().statementRequests.push(whileRequest);
                    yield null;
                }
                /* temporarily disabling for statements
                case FOR -> { //Todo add proper checks for assigner and incrementer being assignment statements
                    Token forT = t;
                    t = consume();
                    if (t.type != OPEN_PAREN) throw new ExpressionError("Expected '(' after for", t);

                    Statement assigner = parseOneStatement(pos + 1);
                    if (!(assigner instanceof AssignStatement || assigner instanceof FunctionCallStatement || assigner instanceof DeclareStatement)) {
                        throw new ExpressionError("Invalid assigner expression in for statement", t);
                    }

                    List<Token> conditionTokens = new ArrayList<>();

                    for (t = consume(); isValidExprToken(t); t = consume()) {
                        conditionTokens.add(t);
                    }

                    if (t.type != SEMICOLON) throw new ExpressionError("Invalid token", t);

                    NodeExpr forCondition = ShuntingYard.parseExpr(conditionTokens);

                    int closed_bracket_pos;
                    int bracket_counter = 1; //we have seen one open bracket

                    for (closed_bracket_pos = 1; bracket_counter != 0; closed_bracket_pos++) {
                        if (peek(closed_bracket_pos).type == OPEN_PAREN) bracket_counter++;
                        if (peek(closed_bracket_pos + 1).type == CLOSE_PAREN) bracket_counter--;
                        conditionTokens.add(t);
                    }

                    Statement incrementer = parseStatements(pos + 1, 1, closed_bracket_pos, true).getFirst();

                    if (!(incrementer instanceof AssignStatement || incrementer instanceof FunctionCallStatement || incrementer instanceof DeclareStatement)) {
                        throw new ExpressionError("Invalid incrementer expression in for statement", t);
                    }

                    needSemi = false; //don't need semicolon after the expression

                    Statement executionStatement = parseOneStatement(pos + 1);

                    if (executionStatement instanceof ScopeStatement scope) {
                        scope.setLoop();
                    }

                    yield new ForStatement(forT, assigner, incrementer, forCondition, executionStatement);
                }
                 */
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
                        args.add(ShuntingYard.parseExpr(tempExpr));
                        tempExpr.clear();
                    }

                    consume(); //Consuming closed parenthesis

                    yield new FunctionCallStatement(fCallTok, args);
                }
                default -> throw new ExpressionError("Unknown statement", t);
            };

            //This will change when I reimplement for statement
            if (needSemi && !(hasNext() && peek().type == SEMICOLON)) { // Must end statements with semicolon
                throw new ExpressionError("Must have ';' after statement", peek());
            }

            if (statement != null) {
                System.out.println("Formed a statement: " + statement.asString() + " at scope depth: " + parserScopes.size());

                //If there are no if, while, etc. that want a statement
                if (parserScopes.peek().statementRequests.isEmpty()) {
                    System.out.println("That was directly added onto the statement stack");
                } else {
                    statement = parserScopes.peek().statementRequests.pop().apply(statement);
                    System.out.println("That was consumed by a request, producing: " + statement.asString());
                }
                parserScopes.peek().statements.add(statement);
            }
        }

        pos--;  // decrementing to the last valid token
        System.out.println("Finished with statementStack having a depth of: " + parserScopes.size() + "\n\n");
        //TODO make sure this is a scope statement, and return its contents instead
        return parserScopes.peek().statements;
    }

    //Preparing for shunting yard
    static boolean isValidExprToken(Token token) {
        return switch (token.type) {
            case LET, EXIT, IF, ELSE, SEMICOLON, C_OPEN_PAREN, C_CLOSE_PAREN, WHILE, FOR, CONTINUE ->
                    false; //Simpler to go by exclusion, it seems
            default -> true;
        };
    }

    //Since imports could contain keywords
    static boolean isValidImportToken(Token token) {
        return token.type == VARIABLE || Keywords.tokeniserKeywords.containsKey(token.value);
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

class ParserScope {
    public final List<Statement> statements = new ArrayList<>();
    public final Stack<Function<Statement, Statement>> statementRequests = new Stack<>();
}
