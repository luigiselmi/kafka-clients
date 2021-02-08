Pilot SC4 Kafka Producer & Consumer [![Build Status](https://travis-ci.org/big-data-europe/pilot-sc4-kafka-producer.svg?branch=master)](https://travis-ci.org/big-data-europe/pilot-sc4-kafka-producer)
===================================
Producer and consumer of floating car data for Apache Kafka. 

## Description
The project creates a producer and a consumer of a Kafka topic. The producer harvests data from a web service, transforms the data into the Avro format and sends the data 
to the topic. A consumer reads and parse the data from the topic for processing and storage.

## Documentation 
This project is a component of the pilot that address the 4th H2020 societal challenge: Smart Green and Integrated Transport. 
The pilot will provide a scalable and fault tolerant system to collect, process and store the data from taxis equipped with GPS devices from the city of Thessaloniki, Greece. The data is provided by the Hellenic Institute of Transport through its [open data portal](http://opendata.imet.gr/dataset/fcd-gps).

## Requirements 
A producer harvests data from a source and writes the data to a Kafka topic in event time, while a consumer listen to a topic. They both depend on Kafka broker that manages the topics. 
Zookeeper is used to set up a cluster for fault tolerance and scalability. Before running a producer the following components must be run from the root folder of an Apache kafka release
as described in the documentation ([Apache Kafka Quick Start](http://kafka.apache.org/documentation.html#quickstart))

Start Zookeeper:    

    $ bin/zookeeper-server-start.sh config/zookeeper.properties

Start a Kafka broker (id=0, port=9092):        

    $ bin/kafka-server-start.sh config/server.properties

Create  a topic. Check if the topic has been already created

    $ bin/kafka-topics.sh --list --zookeeper localhost:2181
    
In case the topic doesn't already exist create one, e.g. "taxi"      

    $ bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic taxi

The topic, "taxi" in the above example, must be the same used when a producer is started. The producer and the consumer are configured to connect to the Kafka broker
through port 9092 in their properties files. 
 
## Build 
The software is based on Maven and can be build from the project root folder simply running the command

    $ mvn install

## Run the producer 
The build creates a jar file with all the dependences and the configuration of the main class in the target folder. 
To start the producer three arguments must be passed to the application: the type of client, producer, the topic to which
the producer will write the data and the source URI from which it will fetch the data. As an example

    $ java -jar target/fcd-producer-1.0.0-jar-with-dependencies.jar producer taxi http://feed.opendata.imet.gr:23577/fcd/gps.json

The producer will start to read the traffic data from the source and write it to the topic "taxi". 

## Run the consumer
To start the consumer simply execute again the same command as above passing "consumer" as argument instead of "producer" and the topic name

    $ java -jar target/fcd-producer-1.0.0-jar-with-dependencies.jar consumer taxi


## Docker image
Build an image using this docker file. Run the following docker command

    $ docker build -t lgslm/fcd-producer:v1.0.0 .

The application consists of a producer container and a consumer container. Both containers need to connect to a Kafka topic so Kafka must be available and the topic
already created. Used the [docker-kafka](https://github.com/luigiselmi/docker-kafka) to build an image with Kafka (with Zookeeper) and create the topic used by the 
producer and the consumer. The same image is also available on [DockerHub](https://hub.docker.com/repository/docker/lgslm/fcd-producer).
 
### Consumer container
To test the consumer using the Docker image start a new container e.g. call it fcd-consumer  and the the Kafka client type to consumer

    $ docker run --rm -it --network=pilot-sc4-net --name fcd-consumer --env ZOOKEEPER_SERVERS=zookeeper:2181 --env KAFKA_CLIENT_TYPE=consumer lgslm/fcd-producer:v1.0.0 bash

The option --network tells docker to add this container to the same network where Kafka is available so that the host name used in producer.props and consumer.props files
in the bootstrap.servers=kafka:9092 can be resolved. The environment variable ZOOKEEPER_SERVERS tells the container the name of the Zookeeper server that
will be used by a Kafka script to figure out whether the topic has been created and is available. The KAFKA_CLIENT_TYPE environment variable is used to execute one of the
two client types, i.e. producer or consumer.

### Producer container
Test the producer container for the FCD data using the command

    $ docker run --rm -it --network=pilot-sc4-net --name fcd-producer --env ZOOKEEPER_SERVERS=zookeeper:2181 --env KAFKA_CLIENT_TYPE=producer lgslm/fcd-producer:v1.0.0 bash

