package com.aktimetrix.service.planner;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Test {
    public static void main(String[] args) {
        Set<Integer> measurements = Stream.of(1, 1, 1, 1, 2, 3, 4, 4).collect(Collectors.toSet());
        List<Integer> expected = Stream.of(1, 2, 3, 4).collect(Collectors.toList());
        final boolean b = measurements.stream().allMatch(expected::remove);
        System.out.println("result: " + b);
    }
}
