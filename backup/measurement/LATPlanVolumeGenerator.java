package com.aktimetrix.products.svm.ciq.cdmpc.measurement;

import com.aktimetrix.products.svm.core.Constants;
import com.aktimetrix.products.svm.core.model.StepInstance;
import com.aktimetrix.products.svm.core.stereotypes.Measurement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Measurement(code = "VOL", stepCode = "LAT")
public class LATPlanVolumeGenerator implements Meter {

    @Autowired
    MeasurementInstanceService measurementInstanceService;

    @Override
    public MeasurementInstance measure(String tenant, String measurementCode, StepInstance bkdStepInstance, StepInstance stepInstance) {
        MeasurementInstance mi = new MeasurementInstance();
        mi.setTenant(tenant);
        mi.setCode(measurementCode);
        mi.setValue(String.valueOf(bkdStepInstance.getMetadata().get("reservationVolume")));
        mi.setStepInstanceId(stepInstance.getId());
        mi.setStepCode(stepInstance.getStepCode());
        mi.setProcessInstanceId(bkdStepInstance.getProcessInstanceId());
        mi.setType(Constants.PLAN_MEASUREMENT_TYPE);
        mi.setMeasuredAt(stepInstance.getLocationCode());
        mi.setUnit((String) bkdStepInstance.getMetadata().get("reservationVolumeUnit"));
        return measurementInstanceService.saveMeasurementInstance(mi);
    }
}

