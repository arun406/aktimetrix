package com.aktimetrix.store.jdbc;

import com.aktimetrix.core.referencedata.model.MeasurementTypeDefinition;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.store.DefinitionStore;
import com.aktimetrix.core.store.StoreDocuments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Definitions in relational tables, one row per definition.
 */
final class JdbcDefinitionStore implements DefinitionStore {

    private final JdbcTemplate jdbc;

    JdbcDefinitionStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static <T> RowMapper<T> rows(Class<T> type) {
        return (rs, n) -> StoreDocuments.fromJson(rs.getString("document"), type);
    }

    @Override
    public ProcessDefinition saveProcess(ProcessDefinition definition) {
        final Optional<ProcessDefinition> existing = findProcess(definition.getTenant(), definition.getProcessCode());
        if (existing.isPresent()) {
            definition.setId(existing.get().getId());
            jdbc.update("UPDATE aktimetrix_process_definition SET status = ?, document = ? WHERE id = ?",
                    definition.getStatus(), StoreDocuments.toJson(definition), definition.getId());
        } else {
            if (definition.getId() == null) {
                definition.setId(JdbcInstanceStores.newId());
            }
            jdbc.update("INSERT INTO aktimetrix_process_definition (id, tenant, process_code, status, document) "
                            + "VALUES (?, ?, ?, ?, ?)", definition.getId(), definition.getTenant(), definition.getProcessCode(),
                    definition.getStatus(), StoreDocuments.toJson(definition));
        }
        return definition;
    }

    @Override
    public Optional<ProcessDefinition> findProcess(String tenant, String processCode) {
        return jdbc.query("SELECT document FROM aktimetrix_process_definition WHERE tenant = ? AND process_code = ?",
                rows(ProcessDefinition.class), tenant, processCode).stream().findFirst();
    }

    @Override
    public List<ProcessDefinition> findProcesses() {
        return jdbc.query("SELECT document FROM aktimetrix_process_definition", rows(ProcessDefinition.class));
    }

    @Override
    public List<ProcessDefinition> findConfirmedProcessesStartedBy(String tenant, String eventCode) {
        return jdbc.query("SELECT document FROM aktimetrix_process_definition WHERE tenant = ? AND status = 'CONFIRMED'",
                        rows(ProcessDefinition.class), tenant).stream()
                .filter(d -> d.getStartEventCodes() != null && d.getStartEventCodes().contains(eventCode))
                .collect(Collectors.toList());
    }

    @Override
    public StepDefinition saveStep(StepDefinition definition) {
        final Optional<StepDefinition> existing = findStep(definition.getTenant(), definition.getStepCode());
        if (existing.isPresent()) {
            definition.setId(existing.get().getId());
            jdbc.update("UPDATE aktimetrix_step_definition SET document = ? WHERE id = ?",
                    StoreDocuments.toJson(definition), definition.getId());
        } else {
            if (definition.getId() == null) {
                definition.setId(JdbcInstanceStores.newId());
            }
            jdbc.update("INSERT INTO aktimetrix_step_definition (id, tenant, step_code, document) VALUES (?, ?, ?, ?)",
                    definition.getId(), definition.getTenant(), definition.getStepCode(), StoreDocuments.toJson(definition));
        }
        return definition;
    }

    @Override
    public Optional<StepDefinition> findStep(String tenant, String stepCode) {
        return jdbc.query("SELECT document FROM aktimetrix_step_definition WHERE tenant = ? AND step_code = ?",
                rows(StepDefinition.class), tenant, stepCode).stream().findFirst();
    }

    @Override
    public List<StepDefinition> findSteps() {
        return jdbc.query("SELECT document FROM aktimetrix_step_definition", rows(StepDefinition.class));
    }

    @Override
    public MeasurementTypeDefinition saveMeasurementType(MeasurementTypeDefinition definition) {
        if (definition.getId() == null) {
            definition.setId(JdbcInstanceStores.newId());
            jdbc.update("INSERT INTO aktimetrix_measurement_type (id, tenant, type_code, document) VALUES (?, ?, ?, ?)",
                    definition.getId(), definition.getTenant(), definition.getCode(), StoreDocuments.toJson(definition));
        } else {
            jdbc.update("UPDATE aktimetrix_measurement_type SET tenant = ?, type_code = ?, document = ? WHERE id = ?",
                    definition.getTenant(), definition.getCode(), StoreDocuments.toJson(definition), definition.getId());
        }
        return definition;
    }

    @Override
    public List<MeasurementTypeDefinition> findMeasurementTypes() {
        return jdbc.query("SELECT document FROM aktimetrix_measurement_type", rows(MeasurementTypeDefinition.class));
    }
}
