package com.aktimetrix.it;

import com.aktimetrix.it.support.RabbitTestBroker;
import com.aktimetrix.it.support.TestBroker;
import com.aktimetrix.it.support.TestStore;

class ParcelOnRabbitMqAndMongoDbTest extends ParcelScenario {
    @Override
    protected TestBroker broker(String eventsTopic) {
        return new RabbitTestBroker(eventsTopic);
    }

    @Override
    protected TestStore store() {
        return TestStore.mongodb();
    }
}
