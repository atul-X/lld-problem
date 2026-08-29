# Kafka

- Event-driven system
- Distributed data streaming platform

## Topics

- Append-only logs
- Kafka message fields:
  1. Value
  2. Key
  3. Timestamp
  4. Headers
  5. Topic
  6. Partition
  7. Offset

## Log Retention and Compaction

1. **Log retention** — delete old data / trim logs by size. Default retention: 7 days.
2. **Log compaction** — keep only the latest value per key.

## Partitioning

- Splits a topic into pieces (up to ~2 million partitions across a cluster).
- Messages without a key are distributed randomly across partitions.
- Ordering is guaranteed **only within a partition** — there is no ordering guarantee across partitions.

## Brokers

- A Kafka broker is a Kafka server process.
- Partitions are spread across brokers (i.e., across the Kafka cluster).
- Brokers hash the key to decide which partition a message goes to.
- **Replication:**
  - Governed by the replication factor; uses a leader/follower architecture.
  - Writes go to the leader; reads can come from a replica.
  - A new leader is elected if the current leader fails.
- In recent versions, Kafka has moved from Apache ZooKeeper to a consensus protocol (KRaft) for metadata management.

## Kafka Clients

- Responsible for reading from and writing to brokers.

### Producers

- Client applications that write to Kafka.
- Kafka provides SDKs for producers.

### Consumers

- Read data from Kafka.
- Subscribe to a list of topics.
- Read messages continuously (infinite loop).
- Report/commit consumer offsets to track read progress.

### Consumer Group

- Each message is read only once within a consumer group.
- Consumers in a group read in parallel, with each consumer assigned to different partitions.

## Kafka Connect

Kafka Connect is a free, open-source tool that is part of Apache Kafka. It streams data between
external systems and Kafka using pluggable connectors, letting you build data pipelines with
configuration files instead of custom code.

**Key components:**

- **Connectors** — plugins that define the logic to interact with an external system (e.g., a database or cloud storage).
  - **Source connectors** — read data from an external system (e.g., MySQL, S3) and write it into Kafka topics.
  - **Sink connectors** — read data from Kafka topics and write it to an external system (e.g., Elasticsearch, a data warehouse).
- **Workers** — the server processes (nodes) that run connectors and tasks.
- **Tasks** — worker threads that split up and process data streams in parallel.
- **Converters** — convert data to/from the byte format Kafka needs.

**Deployment modes:**

- **Standalone mode** — runs on a single machine/worker. Good for small tests or local development.
- **Distributed mode** — runs as a cluster of multiple workers, providing automatic scaling, load balancing, and fault tolerance if a node crashes.
