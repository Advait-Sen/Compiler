package adsen.parser;

import adsen.error.ExpressionError;
import adsen.exec.imports.ImportHandler;
import adsen.parser.node.expr.NodeExpr;
import adsen.parser.node.expr.NodeIdentifier;
import adsen.parser.node.expr.operator.Operator;
import adsen.parser.node.expr.operator.OperatorType;
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

        return ShuntingYard.parseExpr(exprTokens);
    }

    public HeliumProgram parse() {
        HeliumProgram program = new HeliumProgram();

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
                    if (!importsFinished) {
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

                        } else throw new ExpressionError("Expected type", t);

                    }

                    HeliumFunction function = new HeliumFunction(returnType, functionName, signature);

                    if (peek(1).type != C_OPEN_PAREN)
                        throw new ExpressionError("Expected '{' after function declaration", peek(1));

                    function.andThen(((ScopeStatement) parseOneStatement(pos + 1)).statements);

                    program.addFunction(function.name, function);
                }
                default -> throw new ExpressionError("Unexpected token: " + t, t);
            }
        }

        //TODO figure out better place for these, possibly in HeliumProgram
        IMPORT_HANDLER.loadNativeImportData();
        IMPORT_HANDLER.loadImportData();

        return program;
    }

    /**
     * Old parse function which returns the {@link HeliumProgram} defining the entire program's AST.
     */
    public List<Statement> parseStatements() {
        return parseStatements(0, tokens.size()); // Statement count will always be less than token list size
    }

    /**
     * Method which returns the next parsed statement, since it seems to be such a popular request
     */
    Statement parseOneStatement(int startPos) {
        return parseStatements(startPos, 1).getFirst();
    }

    List<Statement> parseStatements(int startPos, int statementCount) {
        return parseStatements(startPos, statementCount, tokens.size(), false); //By default, looking at all the tokens
    }

    List<Statement> parseStatements(int startPos, int statementCount, int tokenCount, boolean ignoreSemi) {
        List<Statement> statements = new ArrayList<>();
        int endPos = pos + tokenCount;
        for (pos = startPos; hasNext() && pos < endPos && statements.size() < statementCount; pos++) {
            Token t = peek();
            Statement statement;
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

                    NodeExpr ifExpr = ShuntingYard.parseExpr(conditionTokens);

                    needSemi = false; //don't need semicolon after the expression

                    Statement thenStatement = parseOneStatement(pos + 1);

                    t = peek(1); //don't consume unless needed

                    if (t.type == ELSE) { //Parsing else statement right here
                        Statement elseStatement = parseOneStatement(pos + 2);

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

                    NodeExpr whileCondition = ShuntingYard.parseExpr(conditionTokens);

                    needSemi = false; //don't need semicolon after the expression

                    Statement executionStatement = parseOneStatement(pos + 1);

                    if (executionStatement instanceof ScopeStatement scope) {
                        scope.setLoop();
                    }

                    yield new WhileStatement(whileT, whileCondition, executionStatement);
                }
                case FOR -> { //Todo add proper checks for assigner and incrementer being assignment statements
                    Token forT = t;
                    t = consume();
                    if (t.type != OPEN_PAREN) throw new ExpressionError("Expected '(' after for", t);

                    Statement assigner = parseOneStatement(pos + 1);
                    if(!(assigner instanceof AssignStatement || assigner instanceof FunctionCallStatement || assigner instanceof DeclareStatement)) {
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

                    if(!(incrementer instanceof AssignStatement || incrementer instanceof FunctionCallStatement || incrementer instanceof DeclareStatement)) {
                        throw new ExpressionError("Invalid incrementer expression in for statement", t);
                    }

                    needSemi = false; //don't need semicolon after the expression

                    Statement executionStatement = parseOneStatement(pos + 1);

                    if (executionStatement instanceof ScopeStatement scope) {
                        scope.setLoop();
                    }

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
                        args.add(ShuntingYard.parseExpr(tempExpr));
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
