# Dockerfile for the SC4 Pilot Floating Car Data producer 
#
# 1) Build an image using this docker file. Run the following docker command
#
#    $ docker build -t bde2020/pilot-sc4-fcd-producer:v0.0.1 .
#
# 2) Test the Kafka producer for the FCD data  in a container. Run the following docker command for testing
#
#    $ docker run --add-host=kafkahost:172.17.0.1 --rm -it --name fcd-producer bde2020/pilot-sc4-fcd-producer:v0.0.1 /bin/bash
#
#    The option --add-host tells docker to add kafkahost to /etc/hosts file so that the host name used in producer.props file
#    in the bootstrap.servers=kafkahost:9092 can be resolved. This configuration works when the producer is run within a docker
#    container and Kafka runs on the host.
#    The Kafka broker, to which the producers send the data, must be configured in the server.properties file to listen to the
#    host network address assigned to it by docker, that is docker0 (not eth0). As an example if docker binds the network docker0
#    to the address 172.17.0.1 then in the server.properties file used to start a broker the listeners property must be set to
#    listeners=PLAINTEXT://172.17.0.1:9092   
#
# 3) Start a container with the  using the config file in the data volume
#
#    $ docker run -d --name fcd-producer bde2020/pilot-sc4-fcd-producer:v0.0.1 
#
#
# we start from bde2020/kafka 
# because there all kafka command line tools and oracle java 8 are available
# see: https://github.com/big-data-europe/docker-kafka

FROM bde2020/kafka
MAINTAINER Luigi Selmi <luigiselmi@gmail.com>, Karl-Heinz Sylla <karl-heinz.sylla@iais.fraunhofer.de>

# Install  network tools (ifconfig, netstat, ping, ip)
RUN apt-get update \
    && apt-get install -y net-tools \
    && apt-get install -y iputils-ping \
    && apt-get install -y iproute2

# Put program/executable to application home
WORKDIR /home/pilot-sc4/

COPY target/pilot-sc4-kafka-producer-0.0.1-SNAPSHOT-jar-with-dependencies.jar .
COPY pilot-sc4-kafka-producer.sh .

# Run the FCD producer
CMD [ "./pilot-sc4-kafka-producer.sh" ]

