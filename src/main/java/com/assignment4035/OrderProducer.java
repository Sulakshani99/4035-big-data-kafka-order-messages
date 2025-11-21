package com.assignment4035;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.Random;

public class OrderProducer {
    private static final String TOPIC = "orders";
    private static final String SCHEMA_REGISTRY_URL = System.getenv("SCHEMA_REGISTRY_URL") != null ? System.getenv("SCHEMA_REGISTRY_URL") : "http://localhost:8081";
    private static final String BOOTSTRAP_SERVERS = System.getenv("KAFKA_BOOTSTRAP_SERVERS") != null ? System.getenv("KAFKA_BOOTSTRAP_SERVERS") : "localhost:9092";

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put("schema.registry.url", SCHEMA_REGISTRY_URL);

        KafkaProducer<String, Order> producer = new KafkaProducer<>(props);

        Random random = new Random();
        String[] products = {"Item1", "Item2", "Item3", "Item4", "Item5"};

        try {
            for (int i = 1; i <= 100; i++) { // Produce 100 orders
                String orderId = String.valueOf(1000 + i);
                String product = products[random.nextInt(products.length)];
                float price = 10 + random.nextFloat() * 90; // Random price between 10 and 100

                Order order = new Order(orderId, product, price);
                ProducerRecord<String, Order> record = new ProducerRecord<>(TOPIC, orderId, order);

                producer.send(record);
                System.out.println("Produced: " + order);

                Thread.sleep(1000); // Wait 1 second between messages
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            producer.close();
        }
    }
}