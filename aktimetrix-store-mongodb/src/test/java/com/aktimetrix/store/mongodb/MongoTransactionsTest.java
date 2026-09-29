package com.aktimetrix.store.mongodb;

import com.aktimetrix.core.configurations.AktimetrixProperties;
import com.aktimetrix.core.configurations.AktimetrixProperties.Storage.TransactionMode;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MongoTransactionsTest {

    @Mock
    private MongoDatabaseFactory databaseFactory;
    @Mock
    private MongoTemplate mongoTemplate;

    @Test
    void autoUsesTransactionsOnAReplicaSet() {
        when(mongoTemplate.executeCommand(any(Document.class))).thenReturn(new Document("setName", "rs0"));
        MongoTransactions transactions = transactions(TransactionMode.AUTO);

        assertThat(transactions.isAtomic()).isTrue();
        assertThat(transactions.isAtomic()).isTrue();
        verify(mongoTemplate, times(1)).executeCommand(any(Document.class));
    }

    @Test
    void autoUsesTransactionsOnAShardedCluster() {
        when(mongoTemplate.executeCommand(any(Document.class))).thenReturn(new Document("msg", "isdbgrid"));

        assertThat(transactions(TransactionMode.AUTO).isAtomic()).isTrue();
    }

    @Test
    void autoWritesWithoutTransactionsOnAStandaloneServer() {
        when(mongoTemplate.executeCommand(any(Document.class))).thenReturn(new Document("ismaster", true));
        MongoTransactions transactions = transactions(TransactionMode.AUTO);

        assertThat(transactions.isAtomic()).isFalse();
        boolean[] ran = {false};
        transactions.run(() -> ran[0] = true);
        assertThat(ran[0]).isTrue();
    }

    @Test
    void neverAndAlwaysDoNotAskTheServer() {
        assertThat(transactions(TransactionMode.NEVER).isAtomic()).isFalse();
        assertThat(transactions(TransactionMode.ALWAYS).isAtomic()).isTrue();
        verify(mongoTemplate, never()).executeCommand(any(Document.class));
    }

    private MongoTransactions transactions(TransactionMode mode) {
        AktimetrixProperties properties = new AktimetrixProperties();
        properties.getStorage().setTransactions(mode);
        return new MongoTransactions(databaseFactory, mongoTemplate, properties);
    }
}
