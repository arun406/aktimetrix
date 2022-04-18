package com.aktimetrix.service.meter.core.meter.api;

import com.aktimetrix.service.meter.core.transferobjects.Measurement;
import com.aktimetrix.service.meter.core.transferobjects.Step;

public interface Meter {

    /**
     * @param tenant tenant code
     * @param step   step instance
     * @return Measurement instance
     */
    Measurement measure(String tenant, Step step);
}
