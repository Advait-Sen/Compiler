package adsen.tokeniser;

import adsen.error.ExpressionError;
import adsen.parser.node.operator.Operator;
import adsen.parser.node.operator.OperatorType;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static adsen.tokeniser.Keywords.operatorTokens;
import static adsen.tokeniser.TokenType.*;
import static adsen.tokeniser.Keywords.tokeniserKeywords;

/**
 * Turns code string into list of tokens
 */
public class Tokeniser {
    /**
     * Code string
     */
    public final String input;
    /**
     * Overall position within input string
     */
    private int pos;
    /**
     * Line position within input string
     */
    private int linepos;
    /**
     * Column position within input string
     */
    private int colpos;


    public Tokeniser(String input) {
        this.input = input;
        pos = 0;
        linepos = 0;
        colpos = 0;
    }

    private final List<Token> tokens = new ArrayList<>();

    /**
     * Create list of tokens
     */
    public void tokenise() {
        if (!tokens.isEmpty()) return;

        Stack<Token> parens = new Stack<>();

        for (pos = 0; hasNext(); pos++) {
            char c = peek();
            Token token = new Token();

            //Skipping over whitespace
            while (hasNext() && Character.isWhitespace(c)) {
                if (c == '\n') {
                    colpos = 0;
                    linepos++;
                } else {
                    colpos++;
                }
                pos++;
                c = peek();
            }

            token.pos = this.pos;
            token.colpos = colpos;
            token.linepos = linepos;

            // Int literal, Float literal (even .456), Hex literal (0xab3c)
            if (isDigit(c) || (c == '.') && isDigit(peek(1))) {
                boolean isFloat = false;
                boolean isHex = c == '0' && hasNext() && peek(1) == 'x';

                while (hasNext() && (isHex && isHexDigit(c) || !isFloat && c == '.' || isDigit(c))) {
                    token.append(c);

                    if (c == '.') {
                        isFloat = true;
                    }

                    c = consume();
                }
                pos--;
                colpos--;
                token.type = isHex ? HEX_LITERAL : isFloat ? FLOAT_LITERAL : INT_LITERAL;

            } else if (c == '\'' || c == '"') { //Char or Str literal
                boolean isStr = c == '"';
                token.type = isStr ? STR_LITERAL : CHAR_LITERAL;

                while (hasNext()) {
                    c = consume();
                    //Check for end of literal
                    if (isStr && c == '"' || !isStr && c == '\'') {
                        break;
                    }
                    //escape characters
                    if (c == '\\') {
                        c = consume();

                        token.append(switch (c) { //Flexing new Java syntax
                            case 'n' -> '\n';
                            case 't' -> '\t';
                            case '\\' -> '\\';
                            case '"' -> '"';
                            case '\'' -> '\'';
                            default -> {
                                token.append("\\" + c);
                                throw new ExpressionError("Invalid escape character '\\" + c + "'", token);
                            }
                        });

                    } else {
                        token.append(c);
                    }
                }

                if (!isStr) { //Checking if char is too short or too long
                    if (token.value.isEmpty()) { //Copied error messages from Java
                        token.append("''");
                        throw new ExpressionError("Empty character literal", token);
                    } else if (token.value.length() != 1) {
                        throw new ExpressionError("Too many characters in character literal", token);
                    }
                }
                if (!hasNext())
                    throw new ExpressionError("Did not terminate " + (isStr ? "string" : "char"), token);

            } else if (Character.isLetter(c) || c == '_') { //Identifiers, Bools, Keywords

                while (hasNext() && (Character.isLetterOrDigit(c) || c == '_')) {
                    token.append(c);

                    c = consume();
                }
                pos--; //Because the last consume() overshoots by one
                colpos--;

                token.type = tokeniserKeywords.getOrDefault(token.value, IDENTIFIER);

            } else if (c == '/') { //Checking for comments (not division)
                c = consume();
                if (c == '/') { //Line comment, consume until end of line
                    while (c != '\n') {
                        c = consume(); //todo add newlines to linepos and colpos for proper error messages when scanning comments and strings
                    }
                } else if (c == '*') { //Block comment, consume until '*/'
                    do {
                        c = consume();
                    } while (hasNext(1) && !(c == '*' && peek(1) == '/'));

                    if (!hasNext(1)) { //Reached end of file without seeing '*/'
                        throw new ExpressionError("Unclosed block comment", token);
                    }
                }

                //Else the '/' is probably for division, so we'll handle it with the other operators below

            } else if (c == ';') {
                token.type = SEMICOLON;
                token.append(c);
            } else if (c == ',') {
                token.type = COMMA;
                token.append(c);
            } else if (c == '(') {
                token.type = OPEN_PAREN;
                token.append(c);
                parens.push(token);
            } else if (c == '[') {
                token.type = SQ_OPEN_PAREN;
                token.append(c);
                parens.push(token);
            } else if (c == '{') {
                token.type = C_OPEN_PAREN;
                token.append(c);
                parens.push(token);
            } else if (c == ')') {
                token.type = CLOSE_PAREN;
                token.append(c);
                if (parens.empty() || !parens.pop().value.equals("(")) {
                    throw new ExpressionError("Mismatched parentheses", token);
                }
            } else if (c == ']') {
                token.type = SQ_CLOSE_PAREN;
                token.append(c);
                if (parens.empty() || !parens.pop().value.equals("[")) {
                    throw new ExpressionError("Mismatched parentheses", token);
                }
            } else if (c == '}') {
                token.type = C_CLOSE_PAREN;
                token.append(c);
                if (parens.empty() || !parens.pop().value.equals("{")) {
                    throw new ExpressionError("Mismatched parentheses", token);
                }
            } else { //Grabbing operators and maybe syntactic sugar later on
                while (hasNext() && !(Character.isWhitespace(c) || Character.isLetterOrDigit(c) || c == '_' || c == '\'' || c == ';' || c == '.' || c == '"' || c == ',' || c == '(' || c == ')' || c == '[' || c == ']' || c == '{' || c == '}')) {
                    //Just grab everything until the next parenthesis, comma, whitespace, char, number, string, or identifier
                    token.append(c);
                    c = consume();
                }
                pos--; //Overshooting by one again
                colpos--;

                if (operatorTokens.containsKey(token.value)) {
                    token.type = operatorTokens.get(token.value);
                } else if (!token.value.isEmpty()) {
                    throw new ExpressionError("Unknown symbol", token);
                }
            }

            colpos++; //to make sure column number advances correctly

            if (token.type != null) //Skipping over final whitespaces and comments in file
                tokens.add(token);
        }

        if (!parens.empty())
            throw new ExpressionError("Mismatched parentheses", parens.getFirst());

        postProcessTokens();
    }

    /**
     * Additional optimisations to tokenisation once we have a list of valid tokens
     */
    private void postProcessTokens() {
        //Postprocessing from second to penulimate token, so we can always have previous and next tokens
        for (int i = 1; i < tokens.size() - 1; i++) {
            Token previous = tokens.get(i - 1);
            Token current = tokens.get(i);
            Token next = tokens.get(i + 1);

            if (current.type == BINARY_OPERATOR) {
                OperatorType opType = Operator.operatorType.get(current.value);

                //This is possibly a unary token
                //This implementation will require updating once new language features like [] and . are added
                if (!previous.isValueToken() && previous.type != CLOSE_PAREN && previous.type != UNARY_OPERATOR) {
                    if (opType == OperatorType.SUM) {
                        current.value = "u+";
                        current.type = UNARY_OPERATOR;
                    }
                    if (opType == OperatorType.DIFFERENCE) {
                        current.value = "u-";
                        current.type = UNARY_OPERATOR;
                    }
                }

            }
        }
    }


    public List<Token> tokens() {
        return tokens;
    }

    boolean hasNext() {
        return hasNext(0);
    }

    boolean hasNext(int offset) {
        return pos + offset < input.length();
    }

    char peek(int offset) {
        if (!hasNext(offset)) return (char) -1;

        return input.charAt(pos + offset);
    }

    /**
     * Returns currently looked at character
     */
    char peek() {
        return peek(0);
    }

    /**
     * Increments position counter and looks at next character in input
     */
    char consume() {
        pos++;
        colpos++;
        return peek();
    }

    static boolean isDigit(char c) {
        return Character.isDigit(c);
    }

    static boolean isHexDigit(char c) {
        return ('0' <= c && c <= '9') || ('a' <= c && c <= 'f') || ('A' <= c && c <= 'F') || c == 'x' || c == 'X';
    }
}
