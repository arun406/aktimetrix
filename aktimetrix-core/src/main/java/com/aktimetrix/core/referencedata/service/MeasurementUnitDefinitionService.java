package com.aktimetrix.core.referencedata.service;

import com.aktimetrix.core.referencedata.model.MeasurementUnitDefinition;
import com.aktimetrix.core.referencedata.repository.MeasurementUnitDefinitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MeasurementUnitDefinitionService {

    @Autowired
    private MeasurementUnitDefinitionRepository repository;

    public void add(MeasurementUnitDefinition definition) {
        this.repository.save(definition);
    }

    public void add(List<MeasurementUnitDefinition> definitions) {
        this.repository.saveAll(definitions);
    }

    public List<MeasurementUnitDefinition> list() {
        return this.repository.findAll();
    }
}
