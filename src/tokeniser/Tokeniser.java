package tokeniser;

import error.ExpressionError;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static tokeniser.TokenType.*;
import static tokeniser.Keywords.tokeniserKeywords;

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

        Stack<Character> parens = new Stack<>();

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

            //Int literal, Float literal, Hex literal, todo negative literal
            if (isDigit(c) || (c == '.' && isDigit(peek(1)))) {

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

                token.type = IDENTIFIER;

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
                parens.push(c);
            } else if (c == '[') {
                token.type = SQ_OPEN_PAREN;
                token.append(c);
                parens.push(c);
            } else if (c == '{') {
                token.type = C_OPEN_PAREN;
                token.append(c);
                parens.push(c);
            } else if (c == ')') {
                token.type = CLOSE_PAREN;
                token.append(c);
                if (parens.empty() || parens.pop() != '(') {
                    throw new ExpressionError("Mismatched parentheses", token);
                }
            } else if (c == ']') {
                token.type = SQ_CLOSE_PAREN;
                token.append(c);
                if (parens.empty() || parens.pop() != '[') {
                    throw new ExpressionError("Mismatched parentheses", token);
                }
            } else if (c == '}') {
                token.type = C_CLOSE_PAREN;
                token.append(c);
                if (parens.empty() || parens.pop() != '{') {
                    throw new ExpressionError("Mismatched parentheses", token);
                }
            } else { //Grabbing operators and maybe syntactic sugar later on
                while (hasNext() && !(Character.isWhitespace(c) || c == ',' || c == '(' || c == ')' || c == '[' || c == ']' || c == '{' || c == '}')) {
                    //Just grab everything until the next parenthesis, comma or whitespace
                    token.append(c);
                    c = consume();
                }
                pos--; //Overshooting by one again
                colpos--;

                token.type = OPERATOR;
            }

            colpos++; //to make sure column number advances correctly

            if (token.type != null) //Skipping over final whitespaces and comments in file
                tokens.add(token);
        }

        postProcessTokens();
    }

    /**
     * Additional optimisations to tokenisation once we have a list of valid tokens
     */
    private void postProcessTokens() {
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            if (token.type == IDENTIFIER) {

                if (tokeniserKeywords.containsKey(token.value)) {
                    token.type = tokeniserKeywords.get(token.value);
                } else if (tokens.get(i - 1).type == LET) {
                    //Actually should check for existing functions and variables first
                    //todo funcs and vars
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
