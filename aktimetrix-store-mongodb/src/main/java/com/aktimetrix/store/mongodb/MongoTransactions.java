package com.aktimetrix.store.mongodb;

import com.aktimetrix.core.configurations.AktimetrixProperties;
import com.aktimetrix.core.configurations.AktimetrixProperties.Storage.TransactionMode;
import com.aktimetrix.core.store.AktimetrixTransactions;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs units of work in MongoDB transactions when the deployment supports them.
 * <p>
 * MongoDB supports multi-document transactions on replica sets and sharded clusters only. With
 * {@code aktimetrix.storage.transactions=auto} (the default), they are used when the deployment supports them;
 * otherwise units of work run without a transaction, with a warning logged once: a crash in the middle of one can then
 * keep its state without its outbound events.
 */
public class MongoTransactions implements AktimetrixTransactions {
    private static final Logger logger = LoggerFactory.getLogger(MongoTransactions.class);

    private final MongoTemplate mongoTemplate;
    private final TransactionMode mode;
    private final TransactionTemplate transactionTemplate;
    private volatile Boolean enabled;

    public MongoTransactions(MongoDatabaseFactory databaseFactory, MongoTemplate mongoTemplate,
                             AktimetrixProperties properties) {
        this.mongoTemplate = mongoTemplate;
        this.mode = properties.getStorage().getTransactions();
        this.transactionTemplate = new TransactionTemplate(new MongoTransactionManager(databaseFactory));
    }

    @Override
    public void run(Runnable work) {
        if (isAtomic()) {
            transactionTemplate.executeWithoutResult(status -> work.run());
        } else {
            work.run();
        }
    }

    /**
     * Whether units of work run in a transaction. In {@code auto} mode, decided on first use from the deployment.
     */
    @Override
    public boolean isAtomic() {
        Boolean decided = enabled;
        if (decided == null) {
            decided = decide();
            enabled = decided;
        }
        return decided;
    }

    private boolean decide() {
        switch (mode) {
            case ALWAYS:
                return true;
            case NEVER:
                logger.warn("MongoDB transactions are disabled (aktimetrix.storage.transactions=never): state and "
                        + "outbound events are not written atomically");
                return false;
            default:
                final boolean supported = supportsTransactions();
                if (supported) {
                    logger.info("MongoDB supports transactions: state and outbound events are written atomically");
                } else {
                    logger.warn("MongoDB is a standalone server, which has no transactions: state and outbound events "
                            + "are not written atomically. Run MongoDB as a replica set (a single node is enough) for "
                            + "atomic writes.");
                }
                return supported;
        }
    }

    private boolean supportsTransactions() {
        final Document hello = mongoTemplate.executeCommand(new Document("isMaster", 1));
        return hello.containsKey("setName") || "isdbgrid".equals(hello.get("msg"));
    }
}
