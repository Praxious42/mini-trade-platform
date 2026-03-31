package com.pbkour.mintrade.portfolio.services;


import lombok.Getter;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

class TestyTest {
    int threads = 16;
    int incrementsPerThread = 10_000_000;

    @Test
    void test() throws InterruptedException {
        ArrayList<Duration> durationsA = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            CounterA.resetSum();
            ExecutorService executorService = Executors.newFixedThreadPool(threads);

            Instant starTimeA = Instant.now();

            for (int j = 0; j < 16; j++) {
                executorService.submit(() -> {
                    for (int k = 0; k < incrementsPerThread; k++) {
                        CounterA.increment();
                    }
                });
            }

            executorService.shutdown();
            executorService.awaitTermination(1000000, TimeUnit.MILLISECONDS);
            Instant endTimeA = Instant.now();

            durationsA.add(Duration.between(starTimeA, endTimeA));
        }

        ArrayList<Duration> durationsB = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            CounterB.resetSum();
            ExecutorService executorService = Executors.newFixedThreadPool(threads);

            Instant starTimeB = Instant.now();
            for (int j = 0; j < 16; j++) {
                executorService.submit(() -> {
                    for (int k = 0; k < incrementsPerThread; k++) {
                        CounterB.increment();
                    }
                });
            }
            executorService.shutdown();
            executorService.awaitTermination(10000000, TimeUnit.MILLISECONDS);
            Instant endTimeB = Instant.now();

            durationsB.add(Duration.between(starTimeB, endTimeB));
        }

        ArrayList<Duration> durationsC = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            CounterC.resetSum();
            ExecutorService executorService = Executors.newFixedThreadPool(threads);

            Instant starTimeC = Instant.now();
            for (int j = 0; j < 16; j++) {
                executorService.submit(() -> {
                    for (int k = 0; k < incrementsPerThread; k++) {
                        CounterC.increment();
                    }
                });
            }
            executorService.shutdown();
            executorService.awaitTermination(10000000, TimeUnit.MILLISECONDS);
            Instant endTimeC = Instant.now();

            durationsC.add(Duration.between(starTimeC, endTimeC));
        }

        System.out.println("Duration A: " + durationsA.stream().reduce(Duration.ZERO, Duration::plus).dividedBy(10L));
        System.out.println("Duration B: " + durationsB.stream().reduce(Duration.ZERO, Duration::plus).dividedBy(10L));
        System.out.println("Duration C: " + durationsC.stream().reduce(Duration.ZERO, Duration::plus).dividedBy(10L));
    }


    @Getter
    class CounterA {
        private static int sum = 0;

        public static synchronized void increment() {
            sum++;
        }

        public static int getCount() {
            return sum;
        }

        public static void resetSum() {
            sum = 0;
        }
    }

    @Getter
    class CounterB {
        private static AtomicInteger sum = new AtomicInteger(0);

        public static void increment() {
            sum.incrementAndGet();
        }

        public static int getCount() {
            return sum.get();
        }

        public static void resetSum() {
            sum.set(0);
        }
    }

    @Getter
    class CounterC {
        private static LongAdder sum = new LongAdder();

        public static void increment() {
            sum.increment();
        }

        public static int getCount() {
            return sum.intValue();
        }

        public static void resetSum() {
            sum.reset();
        }
    }
}
