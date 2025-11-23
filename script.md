# Demo Script — Kafka Order Messages (Assignment EG/2020/4035)

Author: Kumari D.P.S.T  
Registration: EG/2020/4035 (4035)

Purpose
-------
This script documents a short, reproducible demo that covers the assignment requirements:

- Avro-serialized order messages
- Real-time aggregation (running average of prices)
- Retry logic for temporary failures
- Dead Letter Queue (DLQ) for permanently failed messages
- Demonstrate results for grading and include repository evidence

Prerequisites
-------------
- Docker and Docker Compose installed and running
- (Optional) Maven or use the Maven Docker image included in the instructions
- PowerShell (commands below are PowerShell-ready)

Quick overview of components
----------------------------
- `orders` topic — main orders produced by `OrderProducer`
- `orders-dlq` topic — dead-letter queue where permanently failed messages are written
- `OrderProducer` — generates Avro `Order` records and sends them to `orders`
- `OrderConsumer` — consumes `orders`, updates running average, retries, sends to DLQ
- `OrderDLQConsumer` — reads and prints messages from `orders-dlq`

Demo Steps (recommended order)
-----------------------------

1) Start the infrastructure (Zookeeper, Kafka broker, Schema Registry, Kafka UI)

```powershell
cd 'D:\Academic\Semester 8\EC8202 - Big Data and Analytics (GPA)\4035-big-data-kafka-order-messages'
docker-compose up -d zookeeper broker schema-registry kafka-ui
```

Give services ~30s to become healthy. You can watch health with:

```powershell
docker-compose ps
docker-compose logs -f schema-registry
```

2) (Optional) Build the app jar locally using the Maven container (no host Maven required)

```powershell
docker run --rm -v "${PWD}:/app" -w /app maven:3.8.5-openjdk-17 mvn clean package -DskipTests
```

After this you should have `target/kafka-assignment-4035-1.0-SNAPSHOT.jar`.

3) Start the application services (producer, consumer, dlq-consumer)

```powershell
docker-compose up --build producer consumer dlq-consumer
```

This builds the app image (uses Maven image in a build stage) and runs three containers:

- `producer` — produces orders
- `consumer` — consumes and processes orders (prints running average and retry logs)
- `dlq-consumer` — prints messages that arrive on the DLQ topic

4) Verify Schema Registry has the Avro schema registered

Open in browser: http://localhost:8081/subjects

Or query a specific subject (example):

```powershell
curl http://localhost:8081/subjects
curl http://localhost:8081/subjects/orders-value/versions/latest | jq
```

Expected: a JSON object that contains the Avro `Order` schema (fields orderId, product, price).

5) Observe real-time aggregation in consumer logs

Open a new terminal and tail the consumer logs:

```powershell
docker-compose logs -f consumer
```

What to show during demo:
- You should see lines like: `Processed order: Order{orderId=1001, product=Item2, price=45.5}, Running average: 47.30`
- The running average should update as more messages are processed.

6) Demonstrate retry logic and DLQ behavior

How the project simulates failures: the consumer throws an exception for particular messages (e.g., price > 80 or product == "FailItem"). The consumer retries up to 3 times and, if still failing, sends the message to `orders-dlq`.

To force and observe this behavior you can either:
- Wait until producer produces an order with price > 80 (messages randomized).
- Modify `OrderProducer` to produce a deterministic failing message (temporary for demo), rebuild, and restart the producer.

When a failure occurs you should see in the consumer logs:

```text
Failed to process order 1005, retry 1: Price too high: 95.4
Failed to process order 1005, retry 2: Price too high: 95.4
Failed to process order 1005, retry 3: Price too high: 95.4
Sent to DLQ: Failed order: Order{orderId=1005, product=Item1, price=95.4}
```

7) Inspect DLQ messages

Tail the DLQ consumer logs:

```powershell
docker-compose logs -f dlq-consumer
```

Expected output: logged DLQ message values showing the failed record content.

8) Use Kafka UI to inspect topics (optional, good for the demo)

Open http://localhost:8082 and select the `local` cluster. Inspect topic `orders` and `orders-dlq` to show messages and offsets.

9) Verify Avro deserialization is returning generated classes (no GenericRecord CCE)

If the consumer logs show no ClassCastException and the running average prints correctly, the consumer is receiving `com.assignment4035.Order` specific records. If you previously saw a ClassCastException, ensure `specific.avro.reader=true` is set in consumer properties and rebuild.

10) Show Git repository evidence (commits, README, source files)

Example commands to run and show on screen:

```powershell
git status --short
git log --oneline -n 5
git show --name-only HEAD
```

Checklist to include in your submission
--------------------------------------
- [x] `src/main/avro/order.avsc` included
- [x] Producer and consumer source files present and documented
- [x] Running average visible in consumer logs
- [x] Retry behavior visible in consumer logs
- [x] Failed messages end up in `orders-dlq` and are visible to `OrderDLQConsumer`
- [x] README contains run instructions and author info (present at repo root)

Optional: Create a short screen recording (20–60s) that includes
- Start of dockers/services and Kafka UI showing topics
- Producer producing messages (or show producer logs)
- Consumer logs showing running average and a retry+DLQ event
- DLQ consumer showing the failed message

Troubleshooting tips
--------------------
- If Docker build fails due to missing Confluent artifacts, ensure `pom.xml` contains the Confluent Maven repository:

```xml
<repositories>
  <repository>
    <id>confluent</id>
    <url>https://packages.confluent.io/maven/</url>
  </repository>
</repositories>
```

- If you see `http2: server: error reading preface from client //./pipe/docker_engine` restart Docker Desktop and retry.
- If `mvn` is not available on your host use the Maven container (see build step above).

What to record in the demo notes (one-liner for each demo item)
-----------------------------------------------------------
1. Infrastructure started via docker-compose — show `docker-compose ps` output.
2. Schema registered in Schema Registry — show `curl http://localhost:8081/subjects` or Schema Registry UI.
3. Producer producing Avro messages — show producer logs or Kafka UI.
4. Consumer computing running average — show consumer logs with average updating.
5. Retry and DLQ flow — highlight log lines showing retries and DLQ send.
6. DLQ consumer receives failed message — show DLQ consumer logs.
7. Git repo snapshot — `git log` and `README.md` present.

End of demo script

If you want I can also:

- Produce a short `run-demo.ps1` script that executes the above commands in sequence and captures logs to files for submission.
- Add a small deterministic failing message generator to `OrderProducer` (for a guaranteed DLQ event during demo).
