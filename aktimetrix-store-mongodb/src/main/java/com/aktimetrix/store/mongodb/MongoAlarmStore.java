package com.aktimetrix.store.mongodb;

import com.aktimetrix.core.model.Alarm;
import com.aktimetrix.core.store.AlarmStore;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.aktimetrix.store.mongodb.MongoCollections.ALARMS;
import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Alarms in MongoDB. An alarm is claimed with an atomic find-and-modify, one at a time, up to the batch size.
 */
final class MongoAlarmStore implements AlarmStore {

    private final MongoTemplate mongo;

    MongoAlarmStore(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public void schedule(Alarm alarm) {
        final Alarm unclaimed = new Alarm(alarm.getId(), alarm.getTenant(), alarm.getKind(), alarm.getTargetId(),
                alarm.getProcessInstanceId(), alarm.getDueAt(), null, alarm.getAttempts());
        mongo.save(unclaimed, ALARMS);
    }

    @Override
    public void cancel(String id) {
        mongo.remove(Query.query(where("_id").is(id)), ALARMS);
    }

    @Override
    public List<Alarm> claimDue(LocalDateTime now, Instant claimedAt, Instant leaseUntil, int limit) {
        final List<Alarm> claimed = new ArrayList<>();
        final Query due = Query.query(where("dueAt").lt(now).orOperator(
                where("lockedUntil").is(null), where("lockedUntil").lt(claimedAt))).with(Sort.by("dueAt"));
        final Update claim = new Update().set("lockedUntil", leaseUntil).inc("attempts", 1);
        while (claimed.size() < limit) {
            final Alarm alarm = mongo.findAndModify(due, claim, FindAndModifyOptions.options().returnNew(true),
                    Alarm.class, ALARMS);
            if (alarm == null) {
                break;
            }
            claimed.add(alarm);
        }
        return claimed;
    }

    @Override
    public long countPending() {
        return mongo.count(new Query(), ALARMS);
    }
}
