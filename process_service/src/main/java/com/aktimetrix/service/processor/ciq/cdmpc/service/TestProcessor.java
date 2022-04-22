package com.aktimetrix.service.processor.ciq.cdmpc.service;

import com.aktimetrix.core.api.ProcessContext;
import com.aktimetrix.core.impl.AbstractProcessor;

import java.util.HashMap;
import java.util.Map;

public class TestProcessor extends AbstractProcessor {
    @Override
    protected Map<String, Object> getStepMetadata(ProcessContext context) {
        return new HashMap<>();
    }

    @Override
    protected Map<String, Object> getProcessMetadata(ProcessContext context) {
        return new HashMap<>();
    }
}
