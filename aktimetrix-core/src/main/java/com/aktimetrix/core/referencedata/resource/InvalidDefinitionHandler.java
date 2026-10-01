package com.aktimetrix.core.referencedata.resource;

import com.aktimetrix.core.exception.InvalidDefinitionException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

/**
 * Answers a definition that fails validation with {@code 400 Bad Request} and the list of its problems.
 */
@RestControllerAdvice(basePackageClasses = InvalidDefinitionHandler.class)
public class InvalidDefinitionHandler {

    @ExceptionHandler(InvalidDefinitionException.class)
    public ResponseEntity<Map<String, List<String>>> invalid(InvalidDefinitionException e) {
        return ResponseEntity.badRequest().body(Map.of("problems", e.getProblems()));
    }
}
