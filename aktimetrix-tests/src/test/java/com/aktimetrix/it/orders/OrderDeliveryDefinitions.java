package com.aktimetrix.it.orders;

import com.aktimetrix.core.definitions.Definitions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.aktimetrix.core.definitions.Planning.metadataTime;

import java.time.Duration;

/**
 * The order delivery process of the white paper (section 1.1), defined with the Java DSL, with its planning rules:
 * a priority customer's order is delivered within 4 hours of being created and completes within 1 day; others within
 * 2 and 3 days.
 */
@Configuration
public class OrderDeliveryDefinitions {

    @Bean
    Definitions orderDelivery() {
        return Definitions.tenant("SHOP")
                .process("ORDER_DELIVERY", order -> order
                        .entityType("order")
                        .startsOn("ORDER_CREATED")
                        .cancelledOn("ORDER_CANCELLED")
                        .planTime(o -> metadataTime(o, "createdAt").plus(Duration.ofDays(priority(o.getMetadata()) ? 1 : 3)))
                        .measure("COST", "deliveryCost", cost -> cost.value(8).unit("EUR").tolerance("10%")
                                .worseWhenHigher())
                        .metric("FUEL_PER_KM", "FUEL / DISTANCE", m -> m.unit("L/KM").tolerance("10%")
                                .worseWhenHigher())
                        .step("CONFIRM", step -> step.on("ORDER_CONFIRMED").within("PT5M"))
                        .step("PAY", step -> step.on("PAYMENT_CONFIRMED").after("CONFIRM").within("PT15M")
                                .tolerance("PT5M"))
                        .step("HANDOVER", step -> step.on("HANDED_TO_AGENT").within("PT2H"))
                        .step("ACCEPT", step -> step.on("AGENT_ACCEPTED").after("HANDOVER").within("PT15M"))
                        .step("TRAVEL", step -> step.startsOn("TRAVEL_STARTED").endsOn("ARRIVED")
                                .progressOn("LOCATION_UPDATED").after("ACCEPT").within("PT30M")
                                .measure("DISTANCE", "route.distanceKm", km -> km.value(5).unit("KM")
                                        .tolerance("20%").worseWhenHigher())
                                .measure("FUEL", "fuelLitres", litres -> litres.value(0.4).unit("L")
                                        .tolerance("25%").worseWhenHigher()))
                        .step("DELIVERED", step -> step.on("DELIVERED")
                                .planTime(s -> priority(s.getMetadata())
                                        ? metadataTime(s, "createdAt").plus(Duration.ofHours(4))
                                        : metadataTime(s, "createdAt").plus(Duration.ofDays(2)))
                                .measure("TEMPERATURE", "parcelTemperatureC", c -> c.value(30).unit("C")
                                        .tolerance("5").worseWhenHigher()))
                        .step("RATED", step -> step.on("RATED").optional()
                                .measure("RATING", "review.stars", stars -> stars.value(5).unit("STARS")
                                        .tolerance("1").worseWhenLower())))
                .build();
    }

    private static boolean priority(java.util.Map<String, Object> metadata) {
        return metadata != null && Boolean.TRUE.equals(metadata.get("priority"));
    }
}
