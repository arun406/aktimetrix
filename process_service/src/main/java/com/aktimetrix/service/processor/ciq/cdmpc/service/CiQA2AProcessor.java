package com.aktimetrix.service.processor.ciq.cdmpc.service;

import com.aktimetrix.service.processor.ciq.cdmpc.impl.CiQStepDefinitionProvider;
import com.aktimetrix.service.processor.core.Constants;
import com.aktimetrix.service.processor.core.api.MetadataProvider;
import com.aktimetrix.service.processor.core.api.ProcessContext;
import com.aktimetrix.service.processor.core.exception.DefinitionNotFoundException;
import com.aktimetrix.service.processor.core.impl.AbstractProcessor;
import com.aktimetrix.service.processor.core.process.ProcessType;
import com.aktimetrix.service.processor.core.referencedata.model.StepDefinition;
import com.aktimetrix.service.processor.core.stereotypes.ProcessHandler;
import com.aktimetrix.service.processor.core.transferobjects.Cargo;
import com.aktimetrix.service.processor.core.transferobjects.Itinerary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @author arun kumar kandakatla
 */
@Component
@ProcessHandler(processType = ProcessType.CDMP_C)
public class CiQA2AProcessor extends AbstractProcessor {

    final private static Logger logger = LoggerFactory.getLogger(CiQA2AProcessor.class);

    @Autowired
    @Qualifier("stepMetadataProvider")
    private MetadataProvider<Itinerary> stepMetadataProvider;

    @Autowired
    @Qualifier("processMetadataProvider")
    private MetadataProvider<Cargo> processMetadataProvider;

    /**
     * provides CiQ Domain specific metadata
     *
     * @param context Process Context
     * @return metadata
     */
    @Override
    protected Map<String, Object> getStepMetadata(ProcessContext context) {
        return this.stepMetadataProvider.getMetadata((Itinerary) context.getProperty(Constants.EVENT_DATA));
    }

    /**
     * provides CiQ Domain specific metadata
     *
     * @param context Process Context
     * @return metadata
     */
    @Override
    protected Map<String, Object> getProcessMetadata(ProcessContext context) {
        return this.processMetadataProvider.getMetadata((Cargo) context.getProperty(Constants.ENTITY));
    }

    /**
     * CiQ Specific definitions
     *
     * @param context process context
     * @return step definitions
     * @throws DefinitionNotFoundException
     */
    @Override
    public List<StepDefinition> getStepDefinitions(ProcessContext context) throws DefinitionNotFoundException {
        final List<StepDefinition> stepDefinitions = super.getStepDefinitions(context);
        return new CiQStepDefinitionProvider(stepDefinitions, (Cargo) context.getProperty(Constants.ENTITY),
                (Itinerary) context.getProperty(Constants.EVENT_DATA))
                .getDefinitions();
    }
}
