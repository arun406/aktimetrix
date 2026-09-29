package com.aktimetrix.core.referencedata.service;

import com.aktimetrix.core.referencedata.model.MeasurementTypeDefinition;
import com.aktimetrix.core.store.DefinitionStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MeasurementTypeDefinitionService {

    private final DefinitionStore store;

    public List<MeasurementTypeDefinition> list() {
        return store.findMeasurementTypes();
    }

    public MeasurementTypeDefinition add(MeasurementTypeDefinition definition) {
        return store.saveMeasurementType(definition);
    }
}
