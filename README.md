# **Name**: Kumari D.P.S.T

**Registration Number**: EG/2020/4035

# Kafka Order Messages — Assignment

Demonstration Video Link : https://drive.google.com/file/d/18oHlfnwrOTRxdS237bsYrDATLzG_KsnQ/view?usp=sharing

This repository implements an asynchronous order-processing demo using Apache Kafka, Avro and Confluent Schema Registry. It is designed for a live demo and for submission with the following learning objectives:

- Produce Avro-serialized records to Kafka
- Consume and deserialize Avro records using Schema Registry
- Implement retry logic and a Dead Letter Queue (DLQ)
- Maintain a real-time running aggregation (average price)
- Package and run the app using Docker for reproducible demos

## Repository layout

- `src/main/avro/order.avsc` — Avro schema for the `Order` record
- `src/main/java/...` — Java producer, consumer, and DLQ consumer
- `pom.xml` — Maven build file (Avro plugin + dependencies)
- `Dockerfile` and `docker-compose.yml` — Dev/demo environment

## Order schema

The Avro schema (`src/main/avro/order.avsc`) defines an `Order` record with fields:

- `orderId`: string
- `product`: string
- `price`: float

## Architecture (components)

- **Zookeeper** — Kafka coordination (dev only)
- **Kafka broker** — message broker
- **Confluent Schema Registry** — stores Avro schemas and enables safe serialization/deserialization
- **Producer** — generates randomized orders and sends to topic `orders`
- **Consumer** — consumes `orders`, computes running average and retries on transient failures; sends permanently failed records to `orders-dlq`
- **DLQ Consumer** — reads from `orders-dlq` and logs failed orders

## How to run (Docker - recommended)

1. Start the core infrastructure:

```powershell
docker-compose up -d zookeeper broker schema-registry kafka-ui
```

2. Build and run the application services:

```powershell
docker-compose up --build producer consumer dlq-consumer
```

3. Open Kafka UI: `http://localhost:8082` to inspect topics and messages.

## Build locally (if you prefer)

Use a Maven container (no host Maven required):

```powershell
docker run --rm -v "${PWD}:/app" -w /app maven:3.8.5-openjdk-17 mvn clean package -DskipTests
```

Or use host Maven:

```powershell
mvn generate-sources
mvn clean package
```

After a successful build you can run a component locally (example):

```powershell
java -cp target/kafka-assignment-4035-1.0-SNAPSHOT.jar com.assignment4035.OrderProducer
```

## Behavior summary

- Producer: emits random orders (product, randomized price).
- Consumer: updates running average; simulates failures (e.g., price threshold), retries up to 3 times; on repeated failure the message is sent to `orders-dlq`.
- DLQ Consumer: reads and logs failed messages for manual inspection.
