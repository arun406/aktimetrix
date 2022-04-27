package com.aktimetrix.service.processor.core.api;


import com.aktimetrix.service.processor.core.exception.DefinitionNotFoundException;

import java.util.List;

public interface DefinitionProvider<T> {

    /**
     * returns the  step or process definitions
     *
     * @return definition collection
     * @throws DefinitionNotFoundException
     */
    List<T> getDefinitions() throws DefinitionNotFoundException;
}
