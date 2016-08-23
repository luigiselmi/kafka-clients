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

# Pull base image
FROM ubuntu
MAINTAINER Luigi Selmi <luigiselmi@gmail.com>

# Install Java 8.
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y  software-properties-common && \
    add-apt-repository ppa:webupd8team/java -y && \
    apt-get update && \
    echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections && \
    apt-get install -y oracle-java8-installer && \
    apt-get clean

# Define JAVA_HOME environment variable
ENV JAVA_HOME /usr/lib/jvm/java-8-oracle

# Install  network tools (ifconfig, netstat, ping, ip)
RUN apt-get update && \
    apt-get install -y net-tools && \
    apt-get install -y iputils-ping && \
    apt-get install -y iproute2

# Copy the FCD producer jar file in a container folder 
COPY target/pilot-sc4-kafka-producer-0.0.1-SNAPSHOT-jar-with-dependencies.jar /home/pilot-sc4/

# Start the FCD producer
WORKDIR /home/pilot-sc4/
CMD java -jar target/pilot-sc4-kafka-producer-0.0.1-SNAPSHOT-jar-with-dependencies.jar producer taxi http://feed.opendata.imet.gr:23577/fcd/gps.json

