package com.aktimetrix.products.svm.ciq.cdmpc.measurement;

import com.aktimetrix.products.svm.core.Constants;
import com.aktimetrix.products.svm.core.model.StepInstance;
import com.aktimetrix.products.svm.core.stereotypes.Measurement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Measurement(code = "WT", stepCode = "RCS")
@RequiredArgsConstructor
public class RCSPlanWeightGenerator implements Meter {

    private final MeasurementInstanceService measurementInstanceService;

    @Override
    public MeasurementInstance measure(String tenant, String measurementCode, StepInstance bkdStepInstance, StepInstance stepInstance) {
        MeasurementInstance mi = new MeasurementInstance();
        mi.setTenant(tenant);
        mi.setCode(measurementCode);
        mi.setValue(String.valueOf(bkdStepInstance.getMetadata().get("reservationWeight")));
        mi.setStepInstanceId(stepInstance.getId());
        mi.setStepCode(stepInstance.getStepCode());
        mi.setProcessInstanceId(stepInstance.getProcessInstanceId());
        mi.setType(Constants.PLAN_MEASUREMENT_TYPE);
        mi.setUnit((String) bkdStepInstance.getMetadata().get("reservationWeightUnit"));
        mi.setMeasuredAt(stepInstance.getLocationCode());
        return measurementInstanceService.saveMeasurementInstance(mi);
    }
}
