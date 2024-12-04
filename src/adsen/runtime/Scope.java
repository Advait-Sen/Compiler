package adsen.runtime;

import adsen.parser.node.primitives.NodePrimitive;

import java.util.HashMap;
import java.util.Map;

public class Scope {
    public final String name;
    Map<String, NodePrimitive> variables;

    Scope(String name) {
        this.name = name;
        this.variables = new HashMap<>();
    }

    Scope(String name, Scope existing) {
        this.name = name;
        this.variables = existing.variables;
    }

    public static Scope empty(String name){
        return new Scope(name);
    }

    public static Scope filled(String name, Scope existing){
        return new Scope(name, existing);
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
