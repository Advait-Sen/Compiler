package adsen.helium.parser;

import adsen.helium.error.ExpressionError;
import adsen.helium.parser.expr.primitives.NodePrimitive;
import adsen.helium.tokeniser.Token;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static adsen.helium.exec.Scope.MAIN_FUNCTION;

/**
 * Tbis class handles taking {@link Parser} objects, and extracting functions that can then be run or turned into generated code
 */
public class HeliumProgram {

    /**
     * Stores functions which are overloaded, which require the signature to be distinguished in order to identify them.
     * <p>
     * The first string in the list will be the name of the function, followed by its signature (as strings for simplicity)
     */
    private final Map<List<String>, HeliumFunction> signatureFunctions = new HashMap<>();

    /**
     * Stores functions which are not overloaded (with the assumption that this means most of them
     */
    private final Map<String, HeliumFunction> functions = new HashMap<>();

    private HeliumFunction getFunction(String name, Supplier<List<String>> typeSignatureSupplier, Token token) {
        if (lacksFunction(name))
            throw new ExpressionError("No such function '" + name + "'", token);

        //If the function exists and isn't overloaded
        if (functions.get(name) != null) return functions.get(name);

        List<String> typeSignature = new ArrayList<>(typeSignatureSupplier.get());
        typeSignature.addFirst(name);

        if (!signatureFunctions.containsKey(typeSignature)) {
            typeSignature.removeFirst();
            throw new ExpressionError("No such function '" + name + typeSignature + "'", token);
        }

        return signatureFunctions.get(typeSignature);
    }

    public HeliumFunction getFunction(Token functionNameToken, List<NodePrimitive> argValues) {
        Supplier<List<String>> typeSignatureSupplier = () -> argValues.stream().map(NodePrimitive::getTypeString).toList();

        return getFunction(functionNameToken.value, typeSignatureSupplier, functionNameToken);
    }

    public HeliumFunction mainFunction() {
        return getFunction(MAIN_FUNCTION, Collections::emptyList, null);
    }

    /**
     * Attempts to add a function directly to {@link HeliumProgram#functions}. If the function already exists, then both
     * get moved into {@link HeliumProgram#signatureFunctions}, with only a stub left behind to indicate that the function
     * exists over there.
     */
    public void addFunction(HeliumFunction function) {
        String name = function.name;

        //Simplest scenario, function doesn't exist yet
        if (!functions.containsKey(name)) {
            functions.put(name, function);
            return;
        }

        //Function's type signature (minus its name in the front)
        List<String> funcTypeSig = function.getTypeSignature();

        //The function hasn't already been overloaded
        //Check that the signatures don't match, then put them both in signatureFunctions
        if (functions.get(name) != null) {
            HeliumFunction other = functions.get(name);
            List<String> otherTypeSig = other.getTypeSignature();

            if (funcTypeSig.equals(otherTypeSig))
                throw new ExpressionError("Cannot have multiple functions with the same signature", function.token);

            //We have confirmed that they have different signatures, so we complete the signatures
            funcTypeSig.addFirst(name);
            otherTypeSig.addFirst(name);

            signatureFunctions.put(funcTypeSig, function);
            signatureFunctions.put(otherTypeSig, other);

            //This will tell future functions with this name that the method has already been overloaded
            functions.put(name, null);
            return;
        }

        // If we get here, the function has already been overloaded before

        //Completing the signature
        funcTypeSig.addFirst(name);

        if (signatureFunctions.containsKey(funcTypeSig))
            throw new ExpressionError("Cannot have multiple functions with the same signature", function.token);

        signatureFunctions.put(funcTypeSig, function);
    }

    /**
     * This works because even if functions have been overloaded, they will still have a {@code null} entry in
     * {@link HeliumProgram#functions}.
     * <p>
     * Used to be called {@code hasFunction}, but it was always inverted, so IDE recommended inverting it lol
     */
    public boolean lacksFunction(String name) {
        return !functions.containsKey(name);
    }

    public Collection<HeliumFunction> getFunctions() {
        List<HeliumFunction> allFunctions = new ArrayList<>();
        functions.values().stream().filter(Objects::nonNull).forEach(allFunctions::add);
        allFunctions.addAll(signatureFunctions.values());
        return allFunctions;
    }

    /**
     * Prints out all the function headers in the program
     * <p>
     * In future will contain boilerplate, imports, defines, etc.
     */
    public String asString() {
        return functions.values().stream().map(HeliumFunction::asString).reduce("", (s1, s2) -> s1 + "\n\n" + s2);
    }
}
