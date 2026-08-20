# System Design: Back-of-the-Envelope Calculations & Database Selection

Back-of-the-envelope calculations are mathematical sanity checks used in software engineering and system design. We use them to estimate traffic, storage, and bandwidth to ensure our architecture can handle the expected load and to guide our technology choices.

## 1. Step-by-Step Capacity Planning

To figure out what technology you need, you first have to estimate your load. Always round your numbers to make the math easy (e.g., use 100,000 seconds in a day instead of 86,400).

### Estimate Traffic (QPS / TPS)
*   **Determine your Daily Active Users (DAU).**
*   **Estimate requests per user.**
*   **Total Daily Requests** = `DAU × Requests per user`
*   **Queries Per Second (QPS)** = `Total Daily Requests / 100,000` (e.g., 100 million requests / 100k = 1,000 QPS).

### Read-to-Write Ratio
Are users reading data mostly (like YouTube) or writing heavily (like a messaging app)? This severely impacts your database choice.

### Estimate Storage
*   **Estimate average size of a record** (e.g., user profile = 2 KB, photo = 2 MB).
*   **Daily Storage** = `Total Daily Writes × Average Object Size`.
*   Multiply by 365 for a 1-year estimate, and by 5 for a 5-year estimate.

## 2. Numbers to Memorize

Keep these approximations in your head for quick math:
*   **1 day** ≈ 100,000 seconds (actually 86,400)
*   **1 month** ≈ 2.5 million seconds
*   **1 byte** = 8 bits
*   **1 Million bytes** = 1 Megabyte (MB)
*   **1 Billion bytes** = 1 Gigabyte (GB)
*   **1 Trillion bytes** = 1 Terabyte (TB)

---

## 3. Translating Numbers into Database Decisions

Use your QPS, storage, and read/write ratios to pick your database.

| Metric Outcome | Technical Consequence | Typical Tech/Database Choice |
| :--- | :--- | :--- |
| **High Read QPS** (> 10k) | The DB will bottleneck on reads. Offload traffic. | **Cache** (Redis, Memcached) + SQL/NoSQL |
| **High Write QPS** (> 10k) | Traditional SQL struggles with massive concurrent writes. | **Wide-Column NoSQL** (Cassandra, DynamoDB) |
| **Complex Relationships & ACID** | Data integrity is paramount (e.g., financial). | **Relational DB** (PostgreSQL, MySQL) |
| **Massive Unstructured Storage** | Petabytes of images/videos will crash a standard DB. | **Blob/Object Storage** (AWS S3) + CDN |
| **Heavy Search Requirements** | Standard `LIKE` queries scan entire tables and timeout. | **Search Engine** (Elasticsearch, Solr) |

> **The Golden Rule:** Never optimize for a scale you won't hit for 5 years. Start simple (like a single PostgreSQL instance) unless your math proves it will break on day one.

---

## 4. Case Study: Worst-Case Calculation (1,000 Orders/Sec)

When dealing with a metric like "1,000 orders/sec," calculating the worst-case scenario means figuring out how to handle traffic spikes, ensure transaction safety, and manage the sheer amount of data.

### Calculating Peak (Worst-Case) Load
If 1,000 orders/sec is your **average** load, your worst-case (peak) load will be higher (e.g., Black Friday).
*   **Rule of Thumb:** Peak load is typically 2x to 5x the average load.
*   **Peak Formula:** `Average QPS × 5` = 5,000 orders/sec.
*   If 1,000 orders/sec is the *absolute worst-case limit*, design for that plus a 20% safety margin (1,200 orders/sec).

### Breaking Down the Impact (Storage & Writes)
An "order" is a database *write*.
*   **Data Size:** Assume one complete order payload is roughly 2 KB.
*   **Throughput:** 1,200 orders/sec × 2 KB = 2.4 Megabytes (MB) per second.
*   **Daily Storage:** 2.4 MB × 100,000 seconds = ~240 Gigabytes (GB) per day.
*   **Yearly Storage:** 240 GB × 365 = ~87 Terabytes (TB) per year.

### Tech and Database Decisions for High-Volume Writes
Orders require financial accuracy and strict ACID properties (Atomicity, Consistency, Isolation, Durability).

1.  **Database Choice (Relational DB):** A robust SQL DB (**PostgreSQL or MySQL**) is required. 1,200 writes/sec is achievable on a heavily provisioned single instance with fast SSDs. However, generating 87 TB/year will require database sharding, partitioning, or aggressive archiving.
2.  **Peak Protection (Message Queues):** To survive worst-case spikes, introduce an asynchronous message queue (**Apache Kafka or RabbitMQ**). The API accepts the order instantly, places it in the queue, and the database processes it at its maximum safe speed without crashing.
