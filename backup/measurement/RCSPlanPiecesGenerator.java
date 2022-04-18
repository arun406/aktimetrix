package com.aktimetrix.products.svm.ciq.cdmpc.measurement;

import com.aktimetrix.products.svm.core.Constants;
import com.aktimetrix.products.svm.core.model.StepInstance;
import com.aktimetrix.products.svm.core.stereotypes.Measurement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Measurement(code = "PCS", stepCode = "RCS")
public class RCSPlanPiecesGenerator implements Meter {

    @Autowired
    private MeasurementInstanceService measurementInstanceService;

    @Override
    public MeasurementInstance measure(String tenant, String measurementCode, StepInstance bkdStepInstance,
                                       StepInstance stepInstance) {
        MeasurementInstance mi = new MeasurementInstance();
        mi.setTenant(tenant);
        mi.setCode(measurementCode);
        mi.setValue(String.valueOf((int) bkdStepInstance.getMetadata().get("reservationPieces")));
        mi.setStepInstanceId(stepInstance.getId());
        mi.setStepCode(stepInstance.getStepCode());
        mi.setProcessInstanceId(stepInstance.getProcessInstanceId());
        mi.setType(Constants.PLAN_MEASUREMENT_TYPE);
        mi.setMeasuredAt(stepInstance.getLocationCode());
        return measurementInstanceService.saveMeasurementInstance(mi);
    }
}
