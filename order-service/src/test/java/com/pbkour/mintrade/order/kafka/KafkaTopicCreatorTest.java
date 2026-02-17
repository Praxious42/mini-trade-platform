package com.pbkour.mintrade.order.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.common.KafkaFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KafkaTopicCreatorTest {

    @SuppressWarnings("unchecked")
    private static <T> KafkaFuture<T> mockFuture() {
        return (KafkaFuture<T>) mock(KafkaFuture.class);
    }

    @AfterEach
    void clearInterrupt() {
        Thread.interrupted();
    }

    @Test
    void run_skipsWhenNoTopicsConfigured() {
        KafkaTopicCreator creator = new KafkaTopicCreator();
        ReflectionTestUtils.setField(creator, "topicNames", null);
        ReflectionTestUtils.setField(creator, "bootstrapServers", "host:9092");

        try (MockedStatic<AdminClient> adminStatic = Mockito.mockStatic(AdminClient.class)) {
            creator.run(null);
            adminStatic.verify(() -> AdminClient.create((java.util.Properties) any()), never());
        }
    }

    @Test
    void run_skipsWhenTopicsAlreadyExist() throws Exception {
        KafkaTopicCreator creator = new KafkaTopicCreator();
        List<String> topics = List.of("orders", "orders-cancelled");
        ReflectionTestUtils.setField(creator, "topicNames", topics);
        ReflectionTestUtils.setField(creator, "bootstrapServers", "host:9092");

        AdminClient admin = mock(AdminClient.class);
        ListTopicsResult listTopicsResult = mock(ListTopicsResult.class);
        KafkaFuture<Set<String>> namesFuture = mockFuture();

        when(namesFuture.get()).thenReturn(new HashSet<>(List.of("orders", "orders-cancelled", "other")));
        when(listTopicsResult.names()).thenReturn(namesFuture);
        when(admin.listTopics(any(ListTopicsOptions.class))).thenReturn(listTopicsResult);

        try (MockedStatic<AdminClient> adminStatic = Mockito.mockStatic(AdminClient.class)) {
            adminStatic.when(() -> AdminClient.create((java.util.Properties) any())).thenReturn(admin);

            creator.run(null);

            verify(admin, times(1)).listTopics(any(ListTopicsOptions.class));
            verify(admin, never()).createTopics(any());
        }
    }

    @Test
    void run_createsMissingTopicsSuccessfully() throws Exception {
        KafkaTopicCreator creator = new KafkaTopicCreator();
        List<String> topics = List.of("a", "b");
        ReflectionTestUtils.setField(creator, "topicNames", topics);
        ReflectionTestUtils.setField(creator, "partitions", 2);
        ReflectionTestUtils.setField(creator, "replicationFactor", (short) 1);
        ReflectionTestUtils.setField(creator, "bootstrapServers", "host:9092");

        AdminClient admin = mock(AdminClient.class);
        ListTopicsResult listTopicsResult = mock(ListTopicsResult.class);
        KafkaFuture<Set<String>> namesFuture = mockFuture();
        CreateTopicsResult createResult = mock(CreateTopicsResult.class);
        KafkaFuture<Void> allFuture = mockFuture();

        when(namesFuture.get()).thenReturn(new HashSet<>(List.of("a")));
        when(listTopicsResult.names()).thenReturn(namesFuture);
        when(admin.listTopics(any(ListTopicsOptions.class))).thenReturn(listTopicsResult);

        Map<String, KafkaFuture<Void>> values = new HashMap<>();
        KafkaFuture<Void> topicFuture = mockFuture();
        values.put("b", topicFuture);
        when(createResult.values()).thenReturn(values);
        when(admin.createTopics(any())).thenReturn(createResult);

        try (MockedStatic<AdminClient> adminStatic = Mockito.mockStatic(AdminClient.class);
             MockedStatic<KafkaFuture> kafkaFutureStatic = Mockito.mockStatic(KafkaFuture.class)) {
            adminStatic.when(() -> AdminClient.create((java.util.Properties) any())).thenReturn(admin);
            kafkaFutureStatic.when(() -> KafkaFuture.allOf(any())).thenReturn(allFuture);
            when(allFuture.get()).thenReturn(null);

            creator.run(null);

            verify(admin).listTopics(any(ListTopicsOptions.class));
            verify(admin).createTopics(any());
            verify(allFuture).get();
        }
    }

    @Test
    void run_handlesInterruptedExceptionDuringWait() throws Exception {
        KafkaTopicCreator creator = new KafkaTopicCreator();
        List<String> topics = List.of("x");
        ReflectionTestUtils.setField(creator, "topicNames", topics);
        ReflectionTestUtils.setField(creator, "bootstrapServers", "host:9092");

        AdminClient admin = mock(AdminClient.class);
        ListTopicsResult listTopicsResult = mock(ListTopicsResult.class);
        KafkaFuture<Set<String>> namesFuture = mockFuture();
        CreateTopicsResult createResult = mock(CreateTopicsResult.class);
        KafkaFuture<Void> allFuture = mockFuture();

        when(namesFuture.get()).thenReturn(Collections.emptySet());
        when(listTopicsResult.names()).thenReturn(namesFuture);
        when(admin.listTopics(any(ListTopicsOptions.class))).thenReturn(listTopicsResult);

        when(createResult.values()).thenReturn(Map.of("x", mockFuture()));
        when(admin.createTopics(any())).thenReturn(createResult);

        try (MockedStatic<AdminClient> adminStatic = Mockito.mockStatic(AdminClient.class);
             MockedStatic<KafkaFuture> kafkaFutureStatic = Mockito.mockStatic(KafkaFuture.class)) {
            adminStatic.when(() -> AdminClient.create((java.util.Properties) any())).thenReturn(admin);
            kafkaFutureStatic.when(() -> KafkaFuture.allOf(any())).thenReturn(allFuture);

            when(allFuture.get()).thenThrow(new InterruptedException("interrupted"));

            creator.run(null);

            assertTrue(Thread.currentThread().isInterrupted(), "Thread should be interrupted after InterruptedException");

            Thread.interrupted();
        }
    }

    @Test
    void run_handlesExecutionExceptionDuringWait() throws Exception {
        KafkaTopicCreator creator = new KafkaTopicCreator();
        List<String> topics = List.of("y");
        ReflectionTestUtils.setField(creator, "topicNames", topics);
        ReflectionTestUtils.setField(creator, "bootstrapServers", "host:9092");

        AdminClient admin = mock(AdminClient.class);
        ListTopicsResult listTopicsResult = mock(ListTopicsResult.class);
        KafkaFuture<Set<String>> namesFuture = mockFuture();
        CreateTopicsResult createResult = mock(CreateTopicsResult.class);
        KafkaFuture<Void> allFuture = mockFuture();

        when(namesFuture.get()).thenReturn(Collections.emptySet());
        when(listTopicsResult.names()).thenReturn(namesFuture);
        when(admin.listTopics(any(ListTopicsOptions.class))).thenReturn(listTopicsResult);

        when(createResult.values()).thenReturn(Map.of("y", mockFuture()));
        when(admin.createTopics(any())).thenReturn(createResult);

        try (MockedStatic<AdminClient> adminStatic = Mockito.mockStatic(AdminClient.class);
             MockedStatic<KafkaFuture> kafkaFutureStatic = Mockito.mockStatic(KafkaFuture.class)) {
            adminStatic.when(() -> AdminClient.create((java.util.Properties) any())).thenReturn(admin);
            kafkaFutureStatic.when(() -> KafkaFuture.allOf(any())).thenReturn(allFuture);

            when(allFuture.get()).thenThrow(new ExecutionException(new RuntimeException("boom")));

            creator.run(null);

            assertFalse(Thread.currentThread().isInterrupted(), "Thread should not be interrupted on ExecutionException");
        }
    }

    @Test
    void run_handlesUnexpectedExceptionFromAdminCreate() {
        KafkaTopicCreator creator = new KafkaTopicCreator();
        List<String> topics = List.of("z");
        ReflectionTestUtils.setField(creator, "topicNames", topics);
        ReflectionTestUtils.setField(creator, "bootstrapServers", "host:9092");

        try (MockedStatic<AdminClient> adminStatic = Mockito.mockStatic(AdminClient.class)) {

            adminStatic.when(() -> AdminClient.create((java.util.Properties) any())).thenThrow(new RuntimeException("boom create"));

            assertDoesNotThrow(() -> creator.run(null));
        }
    }
}

