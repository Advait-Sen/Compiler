package adsen.helium.parser;

import adsen.helium.error.ExpressionError;
import adsen.helium.parser.expr.FuncCallExpr;
import adsen.helium.parser.expr.NodeExpr;
import adsen.helium.parser.expr.NodeIdentifier;
import adsen.helium.parser.expr.operator.BinaryOperator;
import adsen.helium.parser.expr.operator.Operator;
import adsen.helium.parser.expr.operator.OperatorType;
import adsen.helium.parser.expr.operator.UnaryOperator;
import adsen.helium.parser.expr.primitives.BoolPrimitive;
import adsen.helium.parser.expr.primitives.CharPrimitive;
import adsen.helium.parser.expr.primitives.FloatPrimitive;
import adsen.helium.parser.expr.primitives.IntPrimitive;
import adsen.helium.tokeniser.Token;
import java.util.List;
import java.util.Stack;

import static adsen.helium.tokeniser.TokenType.BINARY_OPERATOR;
import static adsen.helium.tokeniser.TokenType.CLOSE_PAREN;
import static adsen.helium.tokeniser.TokenType.COMMA;
import static adsen.helium.tokeniser.TokenType.FUNCTION;
import static adsen.helium.tokeniser.TokenType.OPEN_PAREN;
import static adsen.helium.tokeniser.TokenType.UNARY_OPERATOR;

/**
 * A class with a single static method which deals with parsing expressions.
 * Its sole purpose is to decrease the cluttered code in {@link Parser}
 */
public class ShuntingYard {

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
                    FuncCallExpr func = new FuncCallExpr(funTok);
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
}
