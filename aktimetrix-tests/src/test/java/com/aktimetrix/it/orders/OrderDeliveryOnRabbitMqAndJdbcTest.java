package com.aktimetrix.it.orders;

import com.aktimetrix.it.support.RabbitTestBroker;
import com.aktimetrix.it.support.TestBroker;
import com.aktimetrix.it.support.TestStore;

class OrderDeliveryOnRabbitMqAndJdbcTest extends OrderDeliveryScenario {
    @Override
    protected TestBroker broker(String eventsTopic) {
        return new RabbitTestBroker(eventsTopic);
    }

    @Override
    protected TestStore store() {
        return TestStore.jdbc();
    }
}
