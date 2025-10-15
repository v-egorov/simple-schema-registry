package ru.vegorov.schemaregistry.service;

import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.Function;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for managing JSLT custom functions.
 * Functions registered here can be applied to JSLT expressions using the withFunction method.
 */
@Component
public class JsltFunctionRegistry {

    private final Map<String, Function> functions = new HashMap<>();

    /**
     * Register a custom function with the given name.
     *
     * @param name Function name that will be used in JSLT expressions
     * @param function The function implementation
     */
    public void register(String name, Function function) {
        functions.put(name, function);
    }

    /**
     * Unregister a function by name.
     *
     * @param name Function name to remove
     */
    public void unregister(String name) {
        functions.remove(name);
    }

    /**
     * Check if a function is registered.
     *
     * @param name Function name
     * @return true if function exists
     */
    public boolean isRegistered(String name) {
        return functions.containsKey(name);
    }

    /**
     * Get all registered function names.
     *
     * @return Set of function names
     */
    public java.util.Set<String> getFunctionNames() {
        return functions.keySet();
    }



    /**
     * Clear all registered functions.
     */
    public void clear() {
        functions.clear();
    }

    /**
     * Get all registered functions as a collection.
     *
     * @return Collection of all registered functions
     */
    public java.util.Collection<Function> getAllFunctions() {
        return functions.values();
    }

    /**
     * Get the number of registered functions.
     *
     * @return Number of functions
     */
    public int size() {
        return functions.size();
    }
}