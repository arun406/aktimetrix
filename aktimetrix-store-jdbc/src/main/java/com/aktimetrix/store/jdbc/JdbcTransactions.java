package com.aktimetrix.store.jdbc;

import com.aktimetrix.core.configurations.AktimetrixProperties;
import com.aktimetrix.core.configurations.AktimetrixProperties.Storage.TransactionMode;
import com.aktimetrix.core.store.AktimetrixTransactions;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

/**
 * Runs each unit of work in a database transaction, unless {@code aktimetrix.storage.transactions=never}.
 */
public class JdbcTransactions implements AktimetrixTransactions {

    private final TransactionTemplate transactionTemplate;
    private final boolean atomic;

    public JdbcTransactions(DataSource dataSource, AktimetrixProperties properties) {
        this.transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        this.atomic = properties.getStorage().getTransactions() != TransactionMode.NEVER;
        if (!atomic) {
            LoggerFactory.getLogger(JdbcTransactions.class).warn("Transactions are disabled "
                    + "(aktimetrix.storage.transactions=never): state and outbound events are not written atomically");
        }
    }

    @Override
    public void run(Runnable work) {
        if (atomic) {
            transactionTemplate.executeWithoutResult(status -> work.run());
        } else {
            work.run();
        }
    }

    @Override
    public boolean isAtomic() {
        return atomic;
    }
}
