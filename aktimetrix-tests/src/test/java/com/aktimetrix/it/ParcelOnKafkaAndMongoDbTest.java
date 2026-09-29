package com.aktimetrix.it;

import com.aktimetrix.it.support.KafkaTestBroker;
import com.aktimetrix.it.support.TestBroker;
import com.aktimetrix.it.support.TestStore;

class ParcelOnKafkaAndMongoDbTest extends ParcelScenario {
    @Override
    protected TestBroker broker(String eventsTopic) {
        return new KafkaTestBroker(eventsTopic);
    }

    @Override
    protected TestStore store() {
        return TestStore.mongodb();
    }
}
