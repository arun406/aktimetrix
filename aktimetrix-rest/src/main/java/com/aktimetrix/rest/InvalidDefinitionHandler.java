package com.aktimetrix.rest;

import com.aktimetrix.core.exception.InvalidDefinitionException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Answers a definition that fails validation with {@code 400 Bad Request} and the list of its problems.
 */
@RestControllerAdvice(assignableTypes = {ProcessDefinitionResource.class, StepDefinitionResource.class})
public class InvalidDefinitionHandler {

    @ExceptionHandler(InvalidDefinitionException.class)
    public ResponseEntity<DefinitionProblems> invalid(InvalidDefinitionException e) {
        return ResponseEntity.badRequest().body(new DefinitionProblems(e.getProblems()));
    }
}
