package com.aktimetrix.store.jdbc;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Timeliness;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.store.MeasurementInstanceStore;
import com.aktimetrix.core.store.ProcessInstanceStore;
import com.aktimetrix.core.store.StepInstanceStore;
import com.aktimetrix.core.store.StoreDocuments;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Process, step and measurement instances in relational tables. A save of an existing instance is an update
 * conditional on its revision: when no row matches, the instance was changed since it was read.
 */
final class JdbcInstanceStores {

    private static final String OVERDUE = Timeliness.OVERDUE.name();

    private JdbcInstanceStores() {
    }

    static String newId() {
        return UUID.randomUUID().toString();
    }

    private static String timeliness(Timeliness timeliness) {
        return timeliness == null ? null : timeliness.name();
    }

    static final class Processes implements ProcessInstanceStore {
        private static final String COLUMNS = "id, revision, document";
        private final JdbcTemplate jdbc;
        private final RowMapper<ProcessInstance> rows = (rs, n) -> {
            final ProcessInstance instance = StoreDocuments.fromJson(rs.getString("document"), ProcessInstance.class);
            instance.setId(rs.getString("id"));
            instance.setRevision(rs.getLong("revision"));
            return instance;
        };

        Processes(JdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        @Override
        public ProcessInstance save(ProcessInstance instance) {
            if (instance.getId() == null || instance.getRevision() == null) {
                if (instance.getId() == null) {
                    instance.setId(newId());
                }
                instance.setRevision(0L);
                jdbc.update("INSERT INTO aktimetrix_process_instance (id, tenant, process_code, entity_type, entity_id, "
                                + "status, complete, late_after, timeliness, revision, run_number, document) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        instance.getId(), instance.getTenant(), instance.getProcessCode(), instance.getEntityType(),
                        instance.getEntityId(), instance.getStatus(), instance.isComplete(), JdbcTimes.utc(instance.getLateAfter()),
                        timeliness(instance.getTimeliness()), 0L, instance.getRun(), StoreDocuments.toJson(instance));
                return instance;
            }
            final long read = instance.getRevision();
            instance.setRevision(read + 1);
            final int updated = jdbc.update("UPDATE aktimetrix_process_instance SET status = ?, complete = ?, late_after = ?, "
                            + "timeliness = ?, revision = ?, document = ? WHERE id = ? AND revision = ?",
                    instance.getStatus(), instance.isComplete(), JdbcTimes.utc(instance.getLateAfter()),
                    timeliness(instance.getTimeliness()), read + 1, StoreDocuments.toJson(instance), instance.getId(), read);
            if (updated == 0) {
                instance.setRevision(read);
                throw new OptimisticLockingFailureException("Process instance " + instance.getId()
                        + " was changed since it was read, or does not exist");
            }
            return instance;
        }

        @Override
        public Optional<ProcessInstance> findById(String tenant, String id) {
            return jdbc.query("SELECT " + COLUMNS + " FROM aktimetrix_process_instance WHERE tenant = ? AND id = ?",
                    rows, tenant, id).stream().findFirst();
        }

        @Override
        public Optional<ProcessInstance> findByEntity(String tenant, String processCode, String entityType,
                                                      String entityId) {
            return jdbc.query("SELECT " + COLUMNS + " FROM aktimetrix_process_instance WHERE tenant = ? AND process_code = ? "
                    + "AND entity_type = ? AND entity_id = ? ORDER BY run_number DESC LIMIT 1", rows, tenant, processCode,
                    entityType, entityId).stream().findFirst();
        }

        @Override
        public List<ProcessInstance> findByEntityId(String tenant, String entityId) {
            return jdbc.query("SELECT " + COLUMNS + " FROM aktimetrix_process_instance WHERE tenant = ? AND entity_id = ?",
                    rows, tenant, entityId);
        }

        @Override
        public List<ProcessInstance> findNotCancelled(String tenant, String entityType, String entityId) {
            return jdbc.query("SELECT " + COLUMNS + " FROM aktimetrix_process_instance WHERE tenant = ? AND entity_type = ? "
                            + "AND entity_id = ? AND (status IS NULL OR status <> ?)", rows, tenant, entityType, entityId,
                    Constants.STATUS_CANCELLED);
        }

        @Override
        public List<ProcessInstance> findRunning(String tenant, String processCode) {
            return jdbc.query("SELECT " + COLUMNS + " FROM aktimetrix_process_instance WHERE tenant = ? AND process_code = ? "
                    + "AND complete = FALSE", rows, tenant, processCode);
        }

        @Override
        public List<ProcessInstance> findOverdue(Instant now) {
            return jdbc.query("SELECT " + COLUMNS + " FROM aktimetrix_process_instance WHERE complete = FALSE "
                    + "AND late_after < ? AND (timeliness IS NULL OR timeliness <> ?)", rows, JdbcTimes.utc(now), OVERDUE);
        }
    }

    static final class Steps implements StepInstanceStore {
        private static final String COLUMNS = "id, revision, document";
        private final JdbcTemplate jdbc;
        private final RowMapper<StepInstance> rows = (rs, n) -> {
            final StepInstance step = StoreDocuments.fromJson(rs.getString("document"), StepInstance.class);
            step.setId(rs.getString("id"));
            step.setRevision(rs.getLong("revision"));
            return step;
        };

        Steps(JdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        @Override
        public StepInstance save(StepInstance step) {
            if (step.getId() == null || step.getRevision() == null) {
                if (step.getId() == null) {
                    step.setId(newId());
                }
                step.setRevision(0L);
                jdbc.update("INSERT INTO aktimetrix_step_instance (id, tenant, process_instance_id, step_sequence, status, "
                                + "late_after, timeliness, revision, document) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        step.getId(), step.getTenant(), step.getProcessInstanceId(), step.getSequence(), step.getStatus(),
                        JdbcTimes.utc(step.getLateAfter()), timeliness(step.getTimeliness()), 0L, StoreDocuments.toJson(step));
                return step;
            }
            final long read = step.getRevision();
            step.setRevision(read + 1);
            final int updated = jdbc.update("UPDATE aktimetrix_step_instance SET status = ?, late_after = ?, timeliness = ?, "
                            + "revision = ?, document = ? WHERE id = ? AND revision = ?",
                    step.getStatus(), JdbcTimes.utc(step.getLateAfter()), timeliness(step.getTimeliness()), read + 1,
                    StoreDocuments.toJson(step), step.getId(), read);
            if (updated == 0) {
                step.setRevision(read);
                throw new OptimisticLockingFailureException("Step instance " + step.getId()
                        + " was changed since it was read, or does not exist");
            }
            return step;
        }

        @Override
        public Optional<StepInstance> findById(String id) {
            return jdbc.query("SELECT " + COLUMNS + " FROM aktimetrix_step_instance WHERE id = ?", rows, id).stream().findFirst();
        }

        @Override
        public List<StepInstance> findByProcessInstance(String tenant, String processInstanceId) {
            return jdbc.query("SELECT " + COLUMNS + " FROM aktimetrix_step_instance WHERE tenant = ? AND process_instance_id = ? "
                    + "ORDER BY step_sequence", rows, tenant, processInstanceId);
        }

        @Override
        public List<StepInstance> findOverdue(Instant now) {
            return jdbc.query("SELECT " + COLUMNS + " FROM aktimetrix_step_instance WHERE (status IS NULL OR status NOT IN (?, ?, ?)) "
                            + "AND late_after < ? AND (timeliness IS NULL OR timeliness <> ?)", rows,
                    Constants.STATUS_COMPLETED, Constants.STATUS_CANCELLED, Constants.STATUS_SKIPPED, JdbcTimes.utc(now), OVERDUE);
        }
    }

    static final class Measurements implements MeasurementInstanceStore {
        private final JdbcTemplate jdbc;
        private final RowMapper<MeasurementInstance> rows = (rs, n) -> {
            final MeasurementInstance measurement = StoreDocuments.fromJson(rs.getString("document"), MeasurementInstance.class);
            measurement.setId(rs.getString("id"));
            return measurement;
        };

        Measurements(JdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        @Override
        public MeasurementInstance save(MeasurementInstance measurement) {
            if (measurement.getId() == null) {
                measurement.setId(newId());
            }
            jdbc.update("INSERT INTO aktimetrix_measurement_instance (id, tenant, process_instance_id, step_instance_id, "
                            + "measurement_code, measurement_type, document) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    measurement.getId(), measurement.getTenant(), measurement.getProcessInstanceId(),
                    measurement.getStepInstanceId(), measurement.getCode(), measurement.getType(),
                    StoreDocuments.toJson(measurement));
            return measurement;
        }

        @Override
        public List<MeasurementInstance> findByProcessInstance(String tenant, String processInstanceId) {
            return jdbc.query("SELECT id, document FROM aktimetrix_measurement_instance WHERE tenant = ? "
                    + "AND process_instance_id = ? ORDER BY row_order", rows, tenant, processInstanceId);
        }

        @Override
        public List<MeasurementInstance> find(String tenant, String processInstanceId, String stepInstanceId,
                                              String code, String type) {
            final String sql = "SELECT id, document FROM aktimetrix_measurement_instance WHERE tenant = ? "
                    + "AND process_instance_id = ? AND measurement_code = ? AND measurement_type = ? AND ";
            return stepInstanceId == null
                    ? jdbc.query(sql + "step_instance_id IS NULL ORDER BY row_order", rows, tenant, processInstanceId, code, type)
                    : jdbc.query(sql + "step_instance_id = ? ORDER BY row_order", rows, tenant, processInstanceId, code,
                    type, stepInstanceId);
        }
    }
}
