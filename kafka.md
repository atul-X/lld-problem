Kafka
	-Event 
	-Data stream platfrom 
topics 
	-logs
	kfka message details
		1.value
		2.key
		3.timestamp
		4.headers
		5.topic
		6.partition
		7.offset
log retention and compaction
	1.log retention - delete old data or trim logs by size.default 7 days.
	2.log compaction - keep only the latest value per key.
Partitioning 
	Split topic into pices
	2 million pation 
	message with out key disterbuted randomly.
	order generntees only on partiotion level.
	no generntees of order across all partitions.
brokers
	kafka servicer process . basically server.
	Partitioning will spread across brokers  mean kafka cluster.
	does hashing which partiton to choose and send message.
	replications
		-relication factor.use leader and followe achture.
		-elect new leader. we write to leader . we can read from replica.
  In latest version for  kafka they have moved from Apache zookeeper  to conseses protocal like Kraft for metadata management.
Kafka client 
	responseable for read and write messages to brokoer.
producers:
	producers are client application that writes to kafka.
	we will have sdk for this.
Consumers:
	reading data from kafka.
	list of topics to subscribe.
	read message infinite.
	report conumer offser marking.
Consumer Group:
	consumer group read message only one time.
	consumer group read parrellely in more consumer with each partiton.
Kafka connect:
	integeration api.
	Kafka Connect is a free, open-source tool part of Apache Kafka that streams data between external systems and Kafka. It uses pluggable connectors to push data into Kafka or pull data out, letting you build large data pipelines using simple configuration files instead of custom code.Key ComponentsConnectors: Plugins that define the logic to interact with an outside system like a database or cloud storage.Source Connectors: Read data from an external system (like MySQL or S3) and write it into Kafka topics.Sink Connectors: Read data from Kafka topics and write it out to an external system (like Elasticsearch or a data warehouse).Workers: The separate server processes (nodes) that run the connectors and tasks.Tasks: The actual worker threads that split up and process the data streams in parallel.Converters: Change data into the right byte format for Kafka or back into the target format.Deployment ModesStandalone Mode: Runs on a single machine or worker. Good for small tests or simple local development.Distributed Mode: Runs as a cluster of multiple workers. It provides automatic scaling, load balancing, and fault tolerance if a node crashes.
