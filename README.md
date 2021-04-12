Distributed Traffic Monitoring [![Build Status](https://travis-ci.org/big-data-europe/pilot-sc4-kafka-producer.svg?branch=master)](https://travis-ci.org/big-data-europe/pilot-sc4-kafka-producer)
==============================
Producer and consumer of floating car data for Apache Kafka. 

## Description
The project creates a producer and a consumer of a Kafka topic. The producer harvests data from a web service, transforms 
the data into the Avro format and sends the data to the topic. A consumer reads and parse the data from the topic for 
processing and storage.

## Documentation 
This project started as one pilot of the [Big Data Europe](https://www.big-data-europe.eu/) project whose aim was to address 
the 4th H2020 societal challenge: Smart Green and Integrated Transport. The pilot's architecture has been designed to be a 
scalable and fault tolerant system to collect, process and store the data from taxis equipped with GPS devices from the city 
of Thessaloniki, Greece. The data is provided by the Hellenic Institute of Transport through its [open data portal](http://opendata.imet.gr/dataset/fcd-gps).

## Requirements 
A producer harvests data from a web service and writes the data to a Kafka topic in event time, while a consumer listen to 
the same topic. They both depend on Kafka broker that manages the topic. Zookeeper is used by Kafka and its topics producers 
and consumers as a name registry for the topics. Before running a producer the following components must be run from the root 
folder of an Apache kafka release as described in the documentation ([Apache Kafka Quick Start](http://kafka.apache.org/documentation.html#quickstart))

Start Zookeeper:    

    $ bin/zookeeper-server-start.sh config/zookeeper.properties

Start a Kafka broker (id=0, port=9092):        

    $ bin/kafka-server-start.sh config/server.properties

Create  a topic. Check if the topic has been already created

    $ bin/kafka-topics.sh --list --zookeeper localhost:2181
    
In case the topic doesn't already exist create one, e.g. "taxi"      

    $ bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic taxi

The topic, "taxi" in the above example, must be the same used when a producer is started. The producer and the consumer are 
configured to connect to the Kafka broker through port 9092 in their properties files. 
 
## Build 
The software is based on Maven and can be build from the project root folder simply running the command

    $ mvn install

## Run the producer 
The build creates a jar file with all the dependences and the configuration of the main class in the target folder. 
To start the producer three arguments must be passed to the application: the type of client, producer, the topic to which
the producer will write the data and the source URI from which it will fetch the data. As an example

    $ java -jar target/fcd-kafka-clients-1.0.0-jar-with-dependencies.jar producer taxi http://feed.opendata.imet.gr:23577/fcd/gps.json

The producer will start to read the traffic data from the source and write it to the topic "taxi". 

## Run the consumer
To start the consumer simply execute again the same command as above passing "consumer" as argument instead of "producer" and the topic name

    $ java -jar target/fcd-kafka-clients-1.0.0-jar-with-dependencies.jar consumer taxi

## Run the Elasticsearch Consumer
Another consumer to send the data to an Elasticsearch index can be used. An Elasticsearch docker container with an index already set up to store
the floating car data is available in the [docker-elasticsearch](https://github.com/luigiselmi/docker-elasticsearch) container. The Elasticsearch 
container must be running before starting this consumer. The command to start the Elasticsearch consumer is almost the same, the only difference 
is in the name of the consumer

   $ java -jar target/fcd-kafka-clients-1.0.0-jar-with-dependencies.jar consumer-elasticsearch taxi 

## Docker image
Build an image using this docker file. Run the following docker command

    $ docker build -t lgslm/fcd-kafka-clients:v1.0.0 .

The application consists of a producer container and a consumer container. Both containers need to connect to a Kafka topic so Kafka must be 
available and the topic already created. Use the [docker-kafka](https://github.com/luigiselmi/docker-kafka) project to build an image with Kafka 
(with Zookeeper) and create the topic used by the producer and the consumer. A docker-compose file is also available to start all the services. 
The image is also available on [DockerHub](https://hub.docker.com/repository/docker/lgslm/kafka).
 
### Consumer container
To test the consumer using the Docker image start a new container, e.g. call it fcd-consumer  and set the Kafka client type to consumer

    $ docker run --rm -d --network=kafka-clients-net --name fcd-consumer \
                          --env ZOOKEEPER_SERVERS=zookeeper:2181 \
                          --env KAFKA_CLIENT_TYPE=consumer \
                          --env TOPIC=taxi \
                          lgslm/fcd-kafka-clients:v1.0.0 bash

The option --network tells docker to add this container to the same network where Kafka is available so that the host name used in 
producer.props and consumer.props files in the bootstrap.servers=kafka:9092 can be resolved. The environment variable ZOOKEEPER_SERVERS tells 
the container the name of the Zookeeper server that will be used by a Kafka script to figure out whether the topic, whose name is provided with 
the TOPIC environment variable, has been created and is available. The KAFKA_CLIENT_TYPE environment variable is used to execute one of the two 
client types, i.e. producer or consumer. The consumer writes the data pulled from the topic to a log file that can be read from within the container. 
In order to log into the consumer container execute the command

    $ docker exec -it fcd-consumer bash

and then execute the command

    # tail -f kafka-client.log


### Producer container
Test the producer container for the FCD data using the command

    $ docker run --rm -d --network=kafka-clients-net --name fcd-producer \
                          --env ZOOKEEPER_SERVERS=zookeeper:2181 \
                          --env KAFKA_CLIENT_TYPE=producer \
                          --env TOPIC=taxi \
                          lgslm/fcd-kafka-clients:v1.0.0 bash

The application consist of a minimum set of 4 Docker containers, one container for Zookeeper, one for Kafka, one for the producer of the traffic 
data and one for the consumer.

## Traffic Visualization
The application uses the floating car data from the taxis as a proxy to monitor the traffic in the city of Thessaloniki. It consists of a certain 
number of docker containers. It can be deployed on a single node, such as a laptop with [Docker Engine](https://docs.docker.com/engine/install/ubuntu/) 
installed, or in a cluster of nodes, such as EC2 servers on the Amazon cloud. We start with the deployment on a single machine and in the following 
section is described how to set up a Docker swarm to distribute the containers across different nodes. All the docker images are available on Docker Hub 
so they do not have to be built on the local machine.

### Deploy to a single node 
The docker containers can be started using two docker-compose files. The first docker-compose file is used to set up the frameworks used by the Kafka 
producer and consumer: Zookeeper, Kafka, Elasticsearch and Kibana. We can start all of them with a single command:

    $ docker-compose -f docker-compose-fcd-thessaloniki.yml up -d

After all the architecture's components are up and running and the Elasticsearch index has been created we can open a tab in a browser and point it to 
the Kibana main page at http://localhost:5601. Once Kibana is ready we can create the index pattern "thessaloniki" so that Kibana will fetch the documents 
from that index in Elasticsearch. The index is still empty but now we can start the producer and the consumer. The producer will fetch the data from the 
CERTH web service and send it to a Kafka topic. The Elasticsearch consumer will fetch the records from the Kafka topic and send it to Elasticsearch for indexing.

    $ docker-compose up -d

After few seconds we should see from Kibana that the index now contains some documents. We can refresh Kibana from time to time. We can create map 
visulizations and filter the data by any of the properties of the taxis such as speed, timestamp and geohash.

![Kibana Map Visualization](/images/thessaloniki-20210224.png)

### Deploy to Docker swarm (cluster)
In order to distribute the containers in more than one node we need to install Docker Engine on each node that wil be part of the cluster. Once we are 
done with this step we have to choose one node as the manager of the cluster while the other will have the role of worker nodes. The Docker engine in 
the manager node wil have to be switched to swarm mode and the worker will have to join the swarm. How to create a docker swarm is described on the 
[Docker web site](https://docs.docker.com/engine/swarm/) and it's quite straightforward. The set up described in this section has been tested on a small 
cluster of three EC2 servers on the Amazon cloud. The following protocols and ports (inbound rules) must be allowed in the security group used by the EC2 
servers so that the swarm master and workers can communicate. We need also a rule to make the Kibana default port open

* TCP port 2377 for cluster management communications
* TCP and UDP port 7946 for communication among nodes
* UDP port 4789 for overlay network traffic
* TCP port 5601 Kibana

After the swarm has been created, with a manager and the workers, we can check that they are available and ready by executing the following command on the 
manager node

    $ docker node ls

All the containers in the cluster must be member of an overlay network in order to use a DNS and be able to use the host names instead of their IP addresses. 
We create the network, e.g. kafka-clients-net, from the manager node with the command

    $ docker network create -d overlay --attachable kafka-clients-net 

The services in the docker-compose files all use the same network name so it will be easier to just use it for the test.
The docker images used in the docker-compose files should be pulled automatically from Docker Hub. The pulling of the images should work in the manager node 
but it may fail in the worker EC2 nodes on the Amazon cloud. One easy way to bypass this potential problem is to manually pull the required images on each 
worker node. You may want to remove unused images before starting to pull the images for the application using the command

    $ docker rmi $(docker images -a -q)

When all the images are available on each node we can deploy the first stack of services (i.e. Zookeeper, Kafka, Elasticsearch and Kibana) on the swarm using 
the docker-compose file

    $ docker stack deploy --compose-file docker-compose-fcd-thessaloniki.yml frameworks-stack

We name this stack framework-stack. We can see the services started and in which node they have been deployed using the command

    $ docker stack ps frameworks-stack

After all the architecture's components are up and running and the Elasticsearch index has been created we can open a tab in a browser and point it to the 
Kibana main page using any of the public addresses that are available and the Kibana's defaul port. For example is the public address of one of the EC2 server 
in our cluster is 3.124.8.48, we can point our browser to the URL http://3.124.8.48:5601 (this example doesn't use the https protocol). The last step is to 
deploy the producer and the Elasticsearch consumer using the 2nd docker-compose file to create another stack, e.g. fcd-stack, and the command

    $ docker stack deploy --compose-file docker-compose.yml fcd-stack 

With this command the producer and consumer containers will be deployed in a different stack but in the same network as stated in the docker-compose file, so 
the services will be able to communicate. After some seconds we should be able to see the first data points in Kibana. To stop all the services use the command

    $ docker stack rm fcd-stack frameworks-stack

### Zookeeper, Kafka and Elasticsearch clusters configuration
A deployment with Zookeeper, Kafka and Elasticsearch in single node is useful for development and test an application but in order to scale and set it up as a 
reliable application we need to 

1. replicate the services, 
2. partition and distribute the data
3. replicate the partitions

For the example in this section a set of three EC2 instances has been used with Docker engine in swarm mode.

#### Zookeeper and Kafka clusters
The first component to be distributed is Zookeeper because it's the framework used by  Kafka to synchronize its brokers. The 2nd step is to start the Kafka brokers 
and connect them to the Zookeeper servers. A docker-compose file to start both Zookeeper and Kafka clusters is available in the 
[Kafka cluster](https://github.com/luigiselmi/docker-kafka#kafka-cluster) section of the docker-kafka repository.

#### Elasticsearch cluster
We can start a cluster of three Elasticsearch servers and one Kibana server following the instructions in the 
[cluster](https://github.com/luigiselmi/docker-elasticsearch#cluster) section of the docker-elasticsearch repository.

#### Producer and consumer
We can test the application using the same docker-compose file used in the previous examples to run the producer and the elasticsearch consumer

    $ docker stack deploy --compose-file docker-compose.yml fcd-stack

The application is deployed as three stacks and 12 containers.
### Troubleshooting
In case you know some documents have been indexed but you can't visualize the data on Kibana you may need to clear the cache of the index. From the main menu 
go to Stack Management and then Index Management, select the index "thessaloniki", and from the "Manage" button click "Clear index cache".
