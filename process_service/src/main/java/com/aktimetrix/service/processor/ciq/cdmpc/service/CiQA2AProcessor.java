package com.aktimetrix.service.processor.ciq.cdmpc.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.MetadataProvider;
import com.aktimetrix.core.api.ProcessContext;
import com.aktimetrix.core.api.ProcessType;
import com.aktimetrix.core.exception.DefinitionNotFoundException;
import com.aktimetrix.core.impl.AbstractProcessor;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.stereotypes.ProcessHandler;
import com.aktimetrix.service.processor.ciq.cdmpc.event.transferobjects.BKDEventDetails;
import com.aktimetrix.service.processor.ciq.cdmpc.event.transferobjects.Cargo;
import com.aktimetrix.service.processor.ciq.cdmpc.event.transferobjects.Itinerary;
import com.aktimetrix.service.processor.ciq.cdmpc.impl.CiQStepDefinitionProvider;
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
@ProcessHandler(processType = ProcessType.A2ATRANSPORT)
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
        final BKDEventDetails eventDetails = (BKDEventDetails) context.getProperty(Constants.EVENT_DATA);
        final Itinerary itinerary = eventDetails.getItineraries().get(0);
        return this.stepMetadataProvider.getMetadata(itinerary);
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
        final BKDEventDetails eventDetails = (BKDEventDetails) context.getProperty(Constants.EVENT_DATA);
        final Cargo entity = (Cargo) context.getProperty(Constants.ENTITY);

        final Itinerary itinerary = eventDetails.getItineraries().get(0);

        return new CiQStepDefinitionProvider(stepDefinitions, entity, itinerary).getDefinitions();
    }
}
