package com.assignment4035;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

public class OrderConsumer {
    private static final String TOPIC = "orders";
    private static final String DLQ_TOPIC = "orders-dlq";
    private static final String SCHEMA_REGISTRY_URL = System.getenv("SCHEMA_REGISTRY_URL") != null ? System.getenv("SCHEMA_REGISTRY_URL") : "http://localhost:8081";
    private static final String BOOTSTRAP_SERVERS = System.getenv("KAFKA_BOOTSTRAP_SERVERS") != null ? System.getenv("KAFKA_BOOTSTRAP_SERVERS") : "localhost:9092";

    private static final int MAX_RETRIES = 3;

    private static AtomicLong sum = new AtomicLong(0);
    private static AtomicLong count = new AtomicLong(0);

    public static void main(String[] args) {
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "order-consumer-group");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        consumerProps.put("schema.registry.url", SCHEMA_REGISTRY_URL);
        // Return generated SpecificRecord (com.assignment4035.Order) instead of GenericRecord
        consumerProps.put("specific.avro.reader", "true");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        KafkaConsumer<String, Order> consumer = new KafkaConsumer<>(consumerProps);

        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName()); // DLQ as string

        KafkaProducer<String, String> dlqProducer = new KafkaProducer<>(producerProps);

        consumer.subscribe(Collections.singletonList(TOPIC));

        try {
            while (true) {
                ConsumerRecords<String, Order> records = consumer.poll(Duration.ofMillis(100));
                for (var record : records) {
                    Order order = record.value();
                    boolean processed = false;
                    int retries = 0;
                    while (!processed && retries < MAX_RETRIES) {
                        try {
                            processOrder(order);
                            processed = true;
                        } catch (Exception e) {
                            retries++;
                            System.out.println("Failed to process order " + order.getOrderId().toString() + ", retry " + retries + ": " + e.getMessage());
                            if (retries >= MAX_RETRIES) {
                                // Send to DLQ
                                String dlqMessage = "Failed order: " + order.toString();
                                ProducerRecord<String, String> dlqRecord = new ProducerRecord<>(DLQ_TOPIC, order.getOrderId().toString(), dlqMessage);
                                dlqProducer.send(dlqRecord);
                                System.out.println("Sent to DLQ: " + dlqMessage);
                            }
                        }
                    }
                    if (processed) {
                        consumer.commitSync();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            consumer.close();
            dlqProducer.close();
        }
    }

    private static void processOrder(Order order) throws Exception {
        // Simulate failure if price > 80
        if (order.getPrice() > 80) {
            throw new Exception("Price too high: " + order.getPrice());
        }
        // Update running average
        sum.addAndGet((long) (order.getPrice() * 100)); // Use long for precision
        count.incrementAndGet();
        double average = (double) sum.get() / count.get() / 100.0;
        System.out.println("Processed order: " + order + ", Running average: " + average);
    }
}