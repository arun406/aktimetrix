package com.aktimetrix.core.configurations;

import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.StepPlanInstance;
import com.google.common.collect.ArrayListMultimap;
import org.bson.Document;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.javatuples.Pair;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

@Configuration
@EnableMongoRepositories(basePackages = {"com.aktimetrix.core.repository",
        "com.aktimetrix.core.referencedata.repository",
        "com.aktimetrix.core.tenant.repository"})
public class MongoConfig {
    @Bean
    MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(
                Arrays.asList(
                        new StringToPairConverter(),
                        new PairToStringConverter(),
                        new ZonedDateTimeReadConverter(),
                        new ZonedDateTimeWriteConverter(),
                        new MultimapWriteConverter(),
                        new MultimapReadConverter(),
                        new StepPlanInstanceReadConverter(),
                        new StepPlanInstanceWriteConverter()));
    }

    @WritingConverter
    public static class PairToStringConverter implements Converter<Pair<String, String>, String> {
        @Override
        public String convert(Pair source) {
            return source.getValue0() + "#" + source.getValue1();
        }
    }

    @ReadingConverter
    public static class StringToPairConverter implements Converter<String, Pair<String, String>> {
        @Override
        public Pair<String, String> convert(String source) {
            final String[] split = source.split("#");
            return Pair.with(split[0], split[1]);
        }
    }

    @ReadingConverter
    public static class ZonedDateTimeReadConverter implements Converter<Date, ZonedDateTime> {
        @Override
        public ZonedDateTime convert(Date date) {
            return date.toInstant().atZone(ZoneOffset.UTC);
        }
    }

    @WritingConverter
    public static class ZonedDateTimeWriteConverter implements Converter<ZonedDateTime, Date> {
        @Override
        public Date convert(ZonedDateTime zonedDateTime) {
            return Date.from(zonedDateTime.toInstant());
        }
    }

    @WritingConverter
    public static class StepPlanInstanceWriteConverter implements Converter<StepPlanInstance, Document> {

        @Override
        public Document convert(StepPlanInstance source) {
            if (source == null)
                return null;
            Document Document = new Document();
            Document.put("stepCode", source.getStepCode());
            Document.put("stepInstanceId", source.getStepInstanceId());
            Document.put("processInstanceId", source.getProcessInstanceId());
            Document.put("status", source.getStatus());
            Document.put("measurements", source.getPlannedMeasurements());
            return Document;
        }
    }

    @ReadingConverter
    public class StepPlanInstanceReadConverter implements Converter<Document, StepPlanInstance> {

        @Override
        public StepPlanInstance convert(Document source) {
            if (source == null || source.isEmpty())
                return null;
            StepPlanInstance stepPlanInstance = new StepPlanInstance();
            stepPlanInstance.setStepInstanceId(source.getString("stepInstanceId"));
            stepPlanInstance.setProcessInstanceId(source.getString("processInstanceId"));
            stepPlanInstance.setStatus(source.getString("stepCode"));
            stepPlanInstance.setPlannedMeasurements((List<MeasurementInstance>) source.get("measurements"));
            return stepPlanInstance;
        }
    }

    @WritingConverter
    public static class MultimapWriteConverter implements Converter<ArrayListMultimap<String, StepPlanInstance>, Document> {
        @Override
        public Document convert(ArrayListMultimap<String, StepPlanInstance> source) {
            Document document = new Document();
            if (source == null || source.isEmpty()) {
                return null;
            }
            source.forEach((s, stepPlanInstance) -> {
                if (stepPlanInstance == null) {
                    return;
                }
                Document Document = new Document();
                List<Document> stepPlanInstances;
                if (document.containsKey(s)) {
                    stepPlanInstances = (List<Document>) document.get(s);
                } else {
                    stepPlanInstances = new ArrayList<>();
                }
                Document stepPlanInstanceDocument = new Document();
                stepPlanInstanceDocument.put("stepCode", stepPlanInstance.getStepCode());
                stepPlanInstanceDocument.put("stepInstanceId", stepPlanInstance.getStepInstanceId());
                stepPlanInstanceDocument.put("processInstanceId", stepPlanInstance.getProcessInstanceId());
                stepPlanInstanceDocument.put("status", stepPlanInstance.getStatus());
                List<MeasurementInstance> plannedMeasurements = stepPlanInstance.getPlannedMeasurements();
                if (plannedMeasurements != null && !plannedMeasurements.isEmpty()) {
                    List<Document> measurements = new ArrayList<>();
                    for (MeasurementInstance plannedMeasurement : plannedMeasurements) {
                        measurements.add(this.toDocument(plannedMeasurement));
                    }
                    stepPlanInstanceDocument.put("measurements", measurements);
                }

                stepPlanInstances.add(stepPlanInstanceDocument);
                document.put(s, stepPlanInstances);
            });
            return document;
        }

        private Document toDocument(MeasurementInstance measurementInstance) {
            Document Document = new Document();
            Document.put("id", measurementInstance.getId());
            Document.put("processInstanceId", measurementInstance.getProcessInstanceId());
            Document.put("stepInstanceId", measurementInstance.getStepInstanceId());
            Document.put("stepCode", measurementInstance.getStepCode());
            Document.put("measuredAt", measurementInstance.getMeasuredAt());
            Document.put("entityType", measurementInstance.getMetadata().get("documentType"));
            Document.put("entityId", measurementInstance.getMetadata().get("documentNumber"));
            Document.put("code", measurementInstance.getCode());
            Document.put("value", measurementInstance.getValue());
            Document.put("unit", measurementInstance.getUnit());
            Document.put("tenant", measurementInstance.getTenant());
            Document.put("type", measurementInstance.getType());
            return Document;
        }
    }

    @ReadingConverter
    public static class MultimapReadConverter implements Converter<Document, ArrayListMultimap<String, StepPlanInstance>> {

        @Override
        public ArrayListMultimap<String, StepPlanInstance> convert(Document source) {
            ArrayListMultimap<@Nullable String, @Nullable StepPlanInstance> multimap = ArrayListMultimap.create();
            if (!source.isEmpty()) {
                source.forEach((stepCode, stepPlanInstancesDocumentList) -> {
                    if (stepPlanInstancesDocumentList != null && stepPlanInstancesDocumentList instanceof List) {
                        List<Document> list = (List<Document>) stepPlanInstancesDocumentList;
                        list.forEach(s -> {
                            StepPlanInstance stepPlanInstance = new StepPlanInstance();
                            stepPlanInstance.setStepInstanceId(s.getString("stepInstanceId"));
                            stepPlanInstance.setProcessInstanceId(s.getString("processInstanceId"));
                            stepPlanInstance.setStatus(s.getString("stepCode"));
                            List<Document> measurements = (List<Document>) s.get("measurements");
                            if (measurements != null && !measurements.isEmpty()) {
                                for (Document measurement : measurements) {
                                    stepPlanInstance.getPlannedMeasurements().add(this.toMeasurementInstance(measurement));
                                }
                            }
                            multimap.put(stepCode, stepPlanInstance);
                        });
                    }
                });
            }
            return multimap;
        }


        private MeasurementInstance toMeasurementInstance(Document document) {
            MeasurementInstance measurementInstance = new MeasurementInstance();
            measurementInstance.setId(document.getString("id"));
            measurementInstance.setStepCode(document.getString("stepCode"));
            measurementInstance.setStepInstanceId(document.getString("stepInstanceId"));
            measurementInstance.setProcessInstanceId(document.getString("processInstanceId"));
            measurementInstance.setCode(document.getString("code"));
            measurementInstance.setValue(document.getString("value"));
            measurementInstance.setMeasuredAt(document.getString("measuredAt"));
            measurementInstance.setUnit(document.getString("unit"));
            measurementInstance.setType(document.getString("type"));
            measurementInstance.setTenant(document.getString("tenant"));
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("documentType", document.getString("entityType"));
            metadata.put("documentNumber", document.getString("entityId"));
            measurementInstance.setMetadata(metadata);
            return measurementInstance;
        }
    }
}
