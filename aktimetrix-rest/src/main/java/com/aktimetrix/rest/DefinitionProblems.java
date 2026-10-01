package com.aktimetrix.rest;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * The answer to a definition that fails validation.
 */
@Schema(description = "Why a definition was refused; nothing was saved")
public class DefinitionProblems {

    @ArraySchema(schema = @Schema(description = "One problem, naming the definition and the field",
            example = "process ORDER_DELIVERY: startEventCodes is missing, so the process can never start"))
    private final List<String> problems;

    public DefinitionProblems(List<String> problems) {
        this.problems = List.copyOf(problems);
    }

    public List<String> getProblems() {
        return problems;
    }
}
