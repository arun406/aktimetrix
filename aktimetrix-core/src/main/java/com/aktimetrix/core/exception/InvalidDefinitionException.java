package com.aktimetrix.core.exception;

import java.util.List;

/**
 * A process or step definition that cannot be saved, with every problem found in it, each as a sentence naming the
 * field.
 */
public class InvalidDefinitionException extends RuntimeException {

    private final List<String> problems;

    public InvalidDefinitionException(List<String> problems) {
        super("Invalid definition: " + String.join("; ", problems));
        this.problems = List.copyOf(problems);
    }

    public List<String> getProblems() {
        return problems;
    }
}
