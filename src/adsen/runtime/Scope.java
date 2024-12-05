package adsen.runtime;

import adsen.parser.node.primitives.NodePrimitive;
import adsen.parser.node.statement.NodeStatement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scope {
    public final String name;
    Map<String, NodePrimitive> variables;
    private final List<NodeStatement> statements;
    private int pos;

    Scope(String name, List<NodeStatement> statements) {
        this.name = name;
        this.variables = new HashMap<>();
        this.statements = statements;
        pos = 0;
    }

    Scope(String name, Scope existing, List<NodeStatement> statements) {
        this.name = name;
        this.variables = new HashMap<>(existing.variables);
        this.statements = statements; //Statements are part of new scope code
        pos = 0;
    }

    public static Scope empty(String name, List<NodeStatement> statements){
        return new Scope(name, statements);
    }

    public static Scope filled(String name, Scope existing, List<NodeStatement> statements){
        return new Scope(name, existing, statements);
    }

    public NodeStatement getStatement(int i){
        pos = i;
        return statements.get(i);
    }

    public List<NodeStatement> getStatements(){
        return statements;
    }

    public int getPos() {
        return pos;
    }




    public void setVariable(String varName, NodePrimitive value){
        variables.put(varName, value);
    }

    public NodePrimitive getVariable(String varName){
        return variables.get(varName);
    }

    public Map<String, NodePrimitive> getVariables(){
        return variables;
    }

    public boolean hasVariable(String varName){
        return variables.containsKey(varName);
    }

}
