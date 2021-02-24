# Dockerfile for the SC4 Pilot Floating Car Data producer 
#
# 1) Build an image using this docker file. Run the following docker command
#
#    $ docker build -t lgslm/fcd-kafka-clients:v1.0.0 .
#
# 2) Test the Kafka producer for the FCD data  in a container. Run the following docker command for testing
#
#    $ docker run --rm -it --network=kafka-clients-net --name fcd-producer \
#                   --env ZOOKEEPER_SERVERS=zookeeper:2181 \
#                   --env KAFKA_CLIENT_TYPE=producer \
#                   --env TOPIC=taxi \
#                   lgslm/fcd-kafka-clients:v1.0.0 bash
#
#    The option --network tells docker to add this container to the same network where Kafka is available so that the host name 
#    used in producer.props file in the bootstrap.servers=kafka:9092 can be resolved. 
#    The environment variable ZOOKEEPER_SERVERS tells the container the name of the Zookeeper server that will be used by a Kafka script
#    to figure out whether the topic has been created and is available.
#    To start a consumer, start a new container e.g. call it fcd-consumer  and the the Kafka client type to consumer
#
#    $ docker run --rm -it --network=kafka-clients-net --name fcd-consumer \
#                    --env ZOOKEEPER_SERVERS=zookeeper:2181 \
#                    --env KAFKA_CLIENT_TYPE=consumer \
#                    --env TOPIC=taxi \
#                    lgslm/fcd-kafka-clients:v1.0.0 bash
#
#    The Kafka broker, to which the producers send the data, must be configured in the server.properties file to listen to the
#    host network address assigned to it by docker, that is docker0 (not eth0). As an example if docker binds the network docker0
#    to the address 172.17.0.1 then in the server.properties file used to start a broker the listeners property must be set to
#    listeners=PLAINTEXT://172.17.0.1:9092   
#

FROM openjdk:8
MAINTAINER Luigi Selmi <luigi@datiaperti.it>

# Install  network tools (ifconfig, netstat, ping, ip)
RUN apt-get update && \
    apt-get install -y net-tools && \
    apt-get install -y iputils-ping && \
    apt-get install -y iproute2 && \
    apt-get install -y netcat

# Install vi for editing
RUN apt-get update && \
    apt-get install -y vim

# Install Apache Kafka (we use a script to check the topics availability)
WORKDIR /usr/local/
RUN wget https://mirror.efect.ro/apache/kafka/2.7.0/kafka_2.13-2.7.0.tgz && \ 
    tar xvf kafka_2.13-2.7.0.tgz && \
    rm kafka_2.13-2.7.0.tgz

ENV KAFKA_HOME=/usr/local/kafka_2.13-2.7.0

# Create a simbolic link to Kafka
RUN ln -s $KAFKA_HOME /kafka

# Move to project folder
WORKDIR /home/pilot-sc4/

COPY target/fcd-kafka-clients-1.0.0-jar-with-dependencies.jar .
COPY start-client.sh .

# Run the FCD producer
ENTRYPOINT [ "./start-client.sh" ]

