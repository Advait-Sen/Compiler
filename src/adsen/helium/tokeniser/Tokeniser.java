package adsen.helium.tokeniser;

import adsen.helium.error.ParsingError;
import adsen.helium.parser.expr.operator.Operator;
import adsen.helium.parser.expr.operator.OperatorType;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static adsen.helium.tokeniser.Keywords.operatorTokens;
import static adsen.helium.tokeniser.TokenType.*;
import static adsen.helium.tokeniser.Keywords.tokeniserKeywords;

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

    /**
     * List of valid tokens generated from a string input
     */
    private final List<Token> tokens = new ArrayList<>();

    /**
     * Create list of tokens
     */
    public void tokenise() {
        if (!tokens.isEmpty()) return;

        // To keep track of matching parentheses. It also gets done automatically later on the parser,
        // but this allows to catch errors earlier on, and I'm proud of this code
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
            if (isDigit(c) || (c == '.' && isDigit(peek(1)))) {
                boolean isFloat = false;
                boolean isHex = c == '0' && hasNext() && peek(1) == 'x';

                // While there are more characters to read,
                // and the next character is a hex digit, decimal point, or regular digit
                while (hasNext() && (isHex && isHexDigit(c) || !isFloat && c == '.' || isDigit(c))) {
                    token.append(c);

                    if (c == '.') {
                        isFloat = true;
                    }

                    c = consume();
                }
                //Backtracking since the while loop shoots one character further than necessary
                pos--;
                colpos--;

                token.type = isHex ? HEX_LITERAL : (isFloat ? FLOAT_LITERAL : INT_LITERAL);

            } else if (c == '\'' || c == '"') {//Char or Str literal
                boolean isStr = c == '"';
                token.type = isStr ? STR_LITERAL : CHAR_LITERAL;

                boolean reachedEnd = false;

                //Basically just grabbing all the characters that follow until the closure of the string or char
                while (!reachedEnd && hasNext()) {
                    c = consume();
                    if (c == '\n') {
                        //Incrementing line number after a newline
                        colpos = 0;
                        linepos++;
                    }

                    //Check for end of literal
                    if (isStr && c == '"' || !isStr && c == '\'') {
                        reachedEnd = true;

                    } else if (c == '\\') { //Checking for and handling escape characters
                        c = consume();

                        token.append(switch (c) { //Flexing new Java syntax
                            case 'n' -> '\n';
                            case 't' -> '\t';
                            case '\\' -> '\\';
                            case '"' -> '"'; //Allow to escape " in characters (so '\"') even tho it's unnecessary
                            case '\'' -> '\''; //And same deal with "\'" in strings
                            default -> {
                                token.append("\\" + c);
                                throw new ParsingError("Invalid escape 'character '\\" + c + "'", token);
                            }
                        });

                    } else { //If it's not an escape character, then it's safe to add
                        token.append(c);
                    }
                }

                if (!isStr) { //Checking if char is too short or too long
                    if (token.value.isEmpty()) { //Copied error messages from Java
                        token.append("''");
                        throw new ParsingError("Empty character literal", token);
                    } else if (token.value.length() != 1) {
                        throw new ParsingError("Too many characters in character literal", token);
                    }
                }
                if (!hasNext())
                    throw new ParsingError("Did not terminate " + (isStr ? "string" : "char"), token);

            } else if (Character.isLetter(c) || c == '_') {//Identifiers, Bools, Keywords, basically any word

                while (hasNext() && (Character.isLetterOrDigit(c) || c == '_')) {
                    token.append(c);

                    c = consume();
                }
                pos--; //Because the last consume() overshoots by one
                colpos--;

                // If we haven't already mapped a token type (so 'true', 'false', 'int', 'exit', etc.)
                // then it's an identifier, i.e. a function or variable name (so far)
                token.type = tokeniserKeywords.getOrDefault(token.value, IDENTIFIER);

            } else if (c == '/' && (peek(1) == '/' || peek(1) == '*')) {
                //Checking for comments

                c = consume();
                if (c == '/') { //Line comment, consume until end of line
                    while (c != '\n') {
                        c = consume();
                    }
                    //Incrementing the line after the end of the comment
                    colpos = 0;
                    linepos++;
                } else if (c == '*') { //Block comment, consume until '*/'
                    boolean commentFinished = false;

                    do {
                        c = consume();
                        if (c == '\n') {
                            //Incrementing the line after the end of the line
                            colpos = 0;
                            linepos++;
                        }
                        if (c == '*' && peek(1) == '/') commentFinished = true;

                    } while (hasNext(1) && !commentFinished);

                    if (!commentFinished) throw new ParsingError("Unclosed block comment", token);

                    consume(); //Consume the / at the end of the block comment
                }

            } else if (c == ';') { //Grabbing special characters that have their own tokens
                token.type = SEMICOLON;
                token.append(c);
            } else if (c == ',') {
                token.type = COMMA;
                token.append(c);
            } else if (c == '(') { //Open parentheses get pushed onto the stack
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
            } else if (c == ')') { //Closed parentheses pop off the stack, and if they don't match, we've got a problem
                token.type = CLOSE_PAREN;
                token.append(c);
                if (parens.empty() || parens.pop().type != OPEN_PAREN) {
                    throw new ParsingError("Mismatched parentheses", token);
                }
            } else if (c == ']') {
                token.type = SQ_CLOSE_PAREN;
                token.append(c);
                if (parens.empty() || parens.pop().type != SQ_OPEN_PAREN) {
                    throw new ParsingError("Mismatched parentheses", token);
                }
            } else if (c == '}') {
                token.type = C_CLOSE_PAREN;
                token.append(c);
                if (parens.empty() || parens.pop().type != C_OPEN_PAREN) {
                    throw new ParsingError("Mismatched parentheses", token);
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
                } else if (!token.value.isEmpty()) { //In case we ran into a comment or something that leaves an incomplete token
                    throw new ParsingError("Unknown symbol", token);
                }
            }

            colpos++; //to make sure column number advances correctly

            if (token.type != null) //Skipping over final whitespaces and comments in file
                tokens.add(token);
        }

        if (!parens.empty())
            throw new ParsingError("Mismatched parentheses", parens.getFirst());

        postProcessTokens();
    }

    /**
     * Additional optimisations to tokenisation once we have a list of valid tokens
     */
    private void postProcessTokens() {
        //Postprocessing from second to penultimate token, so we can always have previous and next tokens available
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
            } else if (current.type == IDENTIFIER) {
                //Making identifiers more specific (Gonna add class names here eventually)
                if (next.type == OPEN_PAREN) {
                    current.type = FUNCTION;
                } else {
                    current.type = VARIABLE;
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
