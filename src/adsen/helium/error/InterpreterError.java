package adsen.helium.error;

import adsen.helium.parser.statement.HeliumStatement;


//TODO not make this extend parsing error once I get ahold of a reference to the InterpreterScope object
public class InterpreterError extends ParsingError {
    HeliumStatement statement;

    public InterpreterError(String message, HeliumStatement statement) {
        super(message, statement.primaryToken());
    }

    public String getMessage() {
        StringBuilder baseMessage = new StringBuilder(super.getMessage());
        baseMessage.append('\n');
        //todo get current InterpreterScope object, in order to get stack trace


        return baseMessage.toString();
    }
}
