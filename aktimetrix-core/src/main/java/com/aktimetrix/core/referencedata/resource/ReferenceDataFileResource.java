package com.aktimetrix.core.referencedata.resource;

import com.aktimetrix.core.referencedata.model.*;
import com.aktimetrix.core.referencedata.service.*;
import com.aktimetrix.core.referencedata.transferobjects.ReferenceDataFileUploadException;
import com.aktimetrix.core.referencedata.transferobjects.ReferenceDataType;
import com.aktimetrix.core.referencedata.transferobjects.UploadFileResponse;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/reference-data/")
@RequiredArgsConstructor
public class ReferenceDataFileResource {

    private final ReferenceDataFileParser parser;
    private final StepDefinitionService stepDefinitionService;
    private final ProcessDefinitionService processDefinitionService;
    private final EventTypeDefinitionService eventTypeDefinitionService;
    private final MeasurementTypeDefinitionService measurementTypeDefinitionService;
    private final MeasurementUnitDefinitionService measurementUnitDefinitionService;

    @PostMapping("{type}/actions/upload")
    public UploadFileResponse uploadFile(@PathVariable String type, @RequestParam("file") MultipartFile file)
            throws ReferenceDataFileUploadException {

        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        // Check if the file's name contains invalid characters
        if (fileName.contains("..")) {
            throw new ReferenceDataFileUploadException("Sorry! Filename contains invalid path sequence " + fileName);
        }
        try {
            switch (ReferenceDataType.valueOfName(type)) {
                case STEP:
                    final List<StepDefinition> stepDefinitions = parser.parseAndReturnStepDefinitions(file.getInputStream());
                    stepDefinitionService.add(stepDefinitions);
                    break;
                case PROCESS:
                    final List<ProcessDefinition> processDefinitions = parser.parseAndReturnProcessDefinitions(file.getInputStream());
                    processDefinitionService.add(processDefinitions);
                    break;
                case EVENT:
                    final List<EventTypeDefinition> eventTypeDefinitions = parser.parseAndReturnEventTypeDefinitions(file.getInputStream());
                    eventTypeDefinitionService.add(eventTypeDefinitions);
                    break;
                case MEASUREMENT_TYPE:
                    final List<MeasurementTypeDefinition> measurementTypeDefinitions = parser.parseAndReturnMeasurementTypeDefinitions(file.getInputStream());
                    measurementTypeDefinitionService.add(measurementTypeDefinitions);
                    break;
                case MEASUREMENT_UNIT:
                    final List<MeasurementUnitDefinition> measurementUnitDefinitions = parser.parseAndReturnMeasurementUnitDefinitions(file.getInputStream());
                    measurementUnitDefinitionService.add(measurementUnitDefinitions);
                    break;
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new ReferenceDataFileUploadException(e);
        } catch (CsvException e) {
            e.printStackTrace();
        } finally {

        }
        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("reference-data/actions/download/")
                .path(fileName)
                .toUriString();

        return new UploadFileResponse(fileName, fileDownloadUri,
                file.getContentType(), file.getSize());
    }
}
