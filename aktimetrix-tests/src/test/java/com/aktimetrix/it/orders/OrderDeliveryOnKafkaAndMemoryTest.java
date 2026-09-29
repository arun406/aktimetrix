package com.aktimetrix.it.orders;

import com.aktimetrix.it.support.KafkaTestBroker;
import com.aktimetrix.it.support.TestBroker;
import com.aktimetrix.it.support.TestStore;

class OrderDeliveryOnKafkaAndMemoryTest extends OrderDeliveryScenario {
    @Override
    protected TestBroker broker(String eventsTopic) {
        return new KafkaTestBroker(eventsTopic);
    }

    @Override
    protected TestStore store() {
        return TestStore.memory();
    }
}
