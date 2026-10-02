package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Timeliness;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StepPlannerTest {

    private static final Instant BOOKED = LocalDateTime.of(2024, 1, 10, 9, 0).toInstant(ZoneOffset.UTC);

    private final StepPlanner planner = new StepPlanner();
    private final Map<String, StepDefinition> definitions = new HashMap<>();

    @Test
    void plansFromProcessStartAndSetsDeadlines() {
        StepInstance pickup = step("PICKUP", 1);
        pickup.setPlannedAt(BOOKED.plus(Duration.ofHours(1)));             // planned by a meter
        StepInstance sort = step("SORT", 2);
        StepInstance deliver = step("DELIVER", 3);
        define("PICKUP", null, null, "PT10M");
        define("SORT", null, "PT3H", null);
        define("DELIVER", "SORT", "PT5H", null);

        List<StepInstance> changed = planner.planNewSteps(List.of(pickup, sort, deliver), definitions, BOOKED);

        assertThat(changed).containsExactly(pickup, sort);
        assertThat(pickup.getLateAfter()).isEqualTo(BOOKED.plus(Duration.ofHours(1)).plus(Duration.ofMinutes(10)));
        assertThat(sort.getPlannedAt()).isEqualTo(BOOKED.plus(Duration.ofHours(3)));
        assertThat(sort.getLateAfter()).isEqualTo(BOOKED.plus(Duration.ofHours(3)));
        assertThat(deliver.getPlannedAt()).as("planned only when SORT completes").isNull();
    }

    @Test
    void plansFromTheActualTimeOfTheStepItCountsFrom() {
        StepInstance sort = step("SORT", 2);
        sort.setActualAt(BOOKED.plus(Duration.ofHours(3)).plus(Duration.ofMinutes(10)));
        StepInstance deliver = step("DELIVER", 3);
        define("SORT", null, "PT3H", null);
        define("DELIVER", "SORT", "PT5H", "PT30M");

        assertThat(planner.planAfter(sort, List.of(sort, deliver), definitions)).containsExactly(deliver);
        assertThat(deliver.getPlannedAt()).isEqualTo(BOOKED.plus(Duration.ofHours(8)).plus(Duration.ofMinutes(10)));
        assertThat(deliver.getLateAfter()).isEqualTo(BOOKED.plus(Duration.ofHours(8)).plus(Duration.ofMinutes(40)));
    }

    @Test
    void forecastsLaterStepsAndFlagsThoseBeyondTheirDeadline() {
        StepInstance pickup = step("PICKUP", 1);
        StepInstance sort = plannedStep("SORT", 2, BOOKED.plus(Duration.ofHours(3)), BOOKED.plus(Duration.ofHours(3)));
        StepInstance load = plannedStep("LOAD", 3, BOOKED.plus(Duration.ofHours(4)), BOOKED.plus(Duration.ofHours(6)));
        StepInstance deliver = plannedStep("DELIVER", 4, BOOKED.plus(Duration.ofHours(8)), BOOKED.plus(Duration.ofHours(8)));
        StepInstance done = plannedStep("EARLIER", 0, BOOKED, BOOKED);
        done.setStatus(Constants.STATUS_COMPLETED);
        define("PICKUP", null, null, null);
        define("SORT", null, "PT3H", null);
        define("LOAD", null, "PT4H", "PT2H");
        define("DELIVER", "PICKUP", "PT7H", null);

        List<StepInstance> atRisk = planner.forecast(pickup, Duration.ofMinutes(90),
                List.of(done, pickup, sort, load, deliver), definitions);

        assertThat(atRisk).containsExactly(sort);
        assertThat(sort.getTimeliness()).isEqualTo(Timeliness.AT_RISK);
        assertThat(load.getExpectedAt()).isEqualTo(BOOKED.plus(Duration.ofHours(5)).plus(Duration.ofMinutes(30)));
        assertThat(load.getTimeliness()).as("within its tolerance").isNull();
        assertThat(deliver.getExpectedAt()).as("planned from PICKUP's actual time already").isNull();
        assertThat(done.getExpectedAt()).isNull();
    }

    @Test
    void noForecastWithoutDelay() {
        StepInstance sort = plannedStep("SORT", 2, BOOKED.plus(Duration.ofHours(3)), BOOKED.plus(Duration.ofHours(3)));
        define("SORT", null, "PT3H", null);

        assertThat(planner.forecast(step("PICKUP", 1), Duration.ofMinutes(-5), List.of(sort), definitions)).isEmpty();
        assertThat(sort.getExpectedAt()).isNull();
    }

    @Test
    void judgesAgainstTheDeadline() {
        StepInstance sort = plannedStep("SORT", 2, BOOKED.plus(Duration.ofHours(3)), BOOKED.plus(Duration.ofHours(3)).plus(Duration.ofMinutes(15)));

        assertThat(planner.judge(sort, BOOKED.plus(Duration.ofHours(3)).plus(Duration.ofMinutes(15)))).isEqualTo(Timeliness.ON_TIME);
        assertThat(planner.judge(sort, BOOKED.plus(Duration.ofHours(3)).plus(Duration.ofMinutes(16)))).isEqualTo(Timeliness.LATE);
        assertThat(planner.judge(step("UNPLANNED", 5), BOOKED)).isNull();
    }

    private void define(String code, String plannedAfter, String plannedWithin, String tolerance) {
        StepDefinition definition = new StepDefinition();
        definition.setStepCode(code);
        definition.setPlannedAfter(plannedAfter);
        definition.setPlannedWithin(plannedWithin);
        definition.setTolerance(tolerance);
        definitions.put(code, definition);
    }

    private static StepInstance step(String code, int sequence) {
        StepInstance step = new StepInstance();
        step.setStepCode(code);
        step.setSequence(sequence);
        step.setStatus(Constants.STATUS_CREATED);
        return step;
    }

    private static StepInstance plannedStep(String code, int sequence, Instant plannedAt, Instant lateAfter) {
        StepInstance step = step(code, sequence);
        step.setPlannedAt(plannedAt);
        step.setLateAfter(lateAfter);
        return step;
    }
}
