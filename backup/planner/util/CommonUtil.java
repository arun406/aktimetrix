package com.aktimetrix.products.svm.ciq.cdmpc.planner.util;

import com.aktimetrix.products.svm.core.service.StepInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CommonUtil {

    private static final Logger logger = LoggerFactory.getLogger(CommonUtil.class);
    @Autowired
    private StepInstanceService stepInstanceService;

}
