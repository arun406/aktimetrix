package com.aktimetrix.service.meter.core;

import com.aktimetrix.service.meter.core.transferobjects.StepEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.util.HashMap;
import java.util.Map;

public class Producer {

    public static void main(String... args) throws JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();
        Serde<StepEvent> domainEventSerde = new JsonSerde<>(StepEvent.class, mapper);

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.RETRIES_CONFIG, 0);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, domainEventSerde.serializer().getClass());

        final String step_created_event = "{\n" +
                "    \"tenantKey\": \"XX\",\n" +
                "    \"eventId\": \"51541182-81fa-4727-afd5-114acdf086b1\",\n" +
                "    \"eventName\": \"cargo movement event\",\n" +
                "    \"eventType\": \"SSU\",\n" +
                "    \"eventCode\": \"Step_Created_Event\",\n" +
                "    \"eventTime\": \"01-02-2021 09:10:00+0400\",\n" +
                "    \"eventUTCTime\": \"01-02-2021 05:10:00+0000\",\n" +
                "    \"source\": \"SVM\",\n" +
                "    \"entityId\": \"61d7392a5526db655f18b214\",\n" +
                "    \"entityType\": \"svm.cargo.process.step\",\n" +
                "    \"eventDetails\": {\n" +
                "        \"tenant\": \"XX\",\n" +
                "        \"id\": \"61d7392a5526db655f18b21e\",\n" +
                "        \"processCode\": \"A2ATRANSPORT\",\n" +
                "        \"processInstanceId\": \"61d7392a5526db655f18b213\",\n" +
                "        \"code\": \"RCS\",\n" +
                "        \"name\": \"Freight ready for carriage\",\n" +
                "        \"locationCode\": \"DXB\",\n" +
                "        \"groupCode\": null,\n" +
                "        \"responsiblePartyCode\": null,\n" +
                "        \"categoryCode\": null,\n" +
                "        \"subCategoryCode\": null,\n" +
                "        \"status\": \"Created\",\n" +
                "        \"sequence\": 0,\n" +
                "        \"version\": 1,\n" +
                "        \"functionalCtxCode\": null,\n" +
                "        \"locationalCtxCode\": null,\n" +
                "        \"metadata\": {\n" +
                "            \"boardPoint\": \"DXB\",\n" +
                "            \"offPoint\": \"BOM\",\n" +
                "            \"flightNumber\": \"XX1234A\",\n" +
                "            \"flightDate\": \"2021-06-25\",\n" +
                "            \"std\": \"2021-06-25 11:40\",\n" +
                "            \"etd\": \"2021-06-25 11:40\",\n" +
                "            \"atd\": null,\n" +
                "            \"sta\": \"2021-06-25 12:35\",\n" +
                "            \"eta\": \"2021-06-25 12:35\",\n" +
                "            \"ata\": null,\n" +
                "            \"pieces\": 5,\n" +
                "            \"wt\": 15.0,\n" +
                "            \"wtUnit\": \"K\",\n" +
                "            \"volUnit\": \"CM\",\n" +
                "            \"vol\": 1.0,\n" +
                "            \"acCategory\": \"RFS\",\n" +
                "            \"cargoType\": \"AWB\",\n" +
                "            \"shipmentDescription\": \"TOYS, GAMES\",\n" +
                "            \"cargoReference\": 12345678,\n" +
                "            \"eFreightCode\": \"EAW\",\n" +
                "            \"reservationWeight\": 15.0,\n" +
                "            \"commodity\": \"9203\",\n" +
                "            \"documentType\": \"AWB\",\n" +
                "            \"documentNumber\": \"888-55555555\",\n" +
                "            \"forwarderCode\": \"9XX\",\n" +
                "            \"origin\": \"DXB\",\n" +
                "            \"destination\": \"BOM\",\n" +
                "            \"reservationVolumeUnit\": \"CM\",\n" +
                "            \"jobReferenceNumber\": 12345678,\n" +
                "            \"shcs\": \"PER\",\n" +
                "            \"cargoCategory\": \"F\",\n" +
                "            \"productCode\": \"GCR\",\n" +
                "            \"reservationVolume\": 1.0,\n" +
                "            \"reservationPieces\": 5,\n" +
                "            \"reservationWeightUnit\": \"K\",\n" +
                "            \"eAWBIndicator\": false\n" +
                "        },\n" +
                "        \"createdOn\": \"2022-01-07 00:17:06\"\n" +
                "    }\n" +
                "}";

        System.out.println(step_created_event);
        StepEvent ddEvent = mapper.readValue(step_created_event, new TypeReference<StepEvent>() {
        });

        /*ddEvent.setEventId("12346");
        ddEvent.setEventType("thisisanevent");
        final Step bkd = new Step("BKD");
        bkd.setTenant("XX");
        bkd.setName("Booking");
        bkd.setLocationCode("DXB");
        bkd.setStatus("C");
        bkd.setProcessInstanceId("61d7392a5526db655f18b213");
        bkd.setId("61d7392a5526db655f18b21e");
        bkd.setVersion(1);
        final Map<String, Object> map = mapper.readValue("{\n" +
                "            \"boardPoint\": \"DXB\",\n" +
                "            \"offPoint\": \"BOM\",\n" +
                "            \"flightNumber\": \"XX1234A\",\n" +
                "            \"flightDate\": \"2021-06-25\",\n" +
                "            \"std\": \"2021-06-25T11:40:00.000\",\n" +
                "            \"etd\": \"2021-06-25T11:40:00.000\",\n" +
                "            \"atd\": null,\n" +
                "            \"sta\": \"2021-06-25T12:35:00.000\",\n" +
                "            \"eta\": \"2021-06-25T12:35:00.000\",\n" +
                "            \"ata\": null,\n" +
                "            \"pieces\": 5,\n" +
                "            \"wt\": 15.0,\n" +
                "            \"wtUnit\": \"K\",\n" +
                "            \"volUnit\": \"CM\",\n" +
                "            \"vol\": 1.0,\n" +
                "            \"acCategory\": \"RFS\",\n" +
                "            \"cargoType\": \"AWB\",\n" +
                "            \"shipmentDescription\": \"TOYS, GAMES\",\n" +
                "            \"cargoReference\": 12345678,\n" +
                "            \"eFreightCode\": \"EAW\",\n" +
                "            \"reservationWeight\": 15.0,\n" +
                "            \"commodity\": \"9203\",\n" +
                "            \"documentType\": \"AWB\",\n" +
                "            \"documentNumber\": \"888-55555555\",\n" +
                "            \"forwarderCode\": \"9XX\",\n" +
                "            \"origin\": \"DXB\",\n" +
                "            \"destination\": \"BOM\",\n" +
                "            \"reservationVolumeUnit\": \"CM\",\n" +
                "            \"jobReferenceNumber\": 12345678,\n" +
                "            \"shcs\": \"PER\",\n" +
                "            \"cargoCategory\": \"F\",\n" +
                "            \"productCode\": \"GCR\",\n" +
                "            \"reservationVolume\": 1.0,\n" +
                "            \"reservationPieces\": 5,\n" +
                "            \"reservationWeightUnit\": \"K\",\n" +
                "            \"eAWBIndicator\": false\n" +
                "        }", Map.class);
        bkd.setMetadata(map);
        ddEvent.setEntity(bkd);
*/
        DefaultKafkaProducerFactory<String, StepEvent> pf = new DefaultKafkaProducerFactory<>(props);
        KafkaTemplate<String, StepEvent> template = new KafkaTemplate<>(pf, true);
        template.setDefaultTopic("step-instance-in-0");

        template.sendDefault("51541182-81fa-4727-afd5-114acdf086b1", ddEvent);
    }
}
