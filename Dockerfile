# Dockerfile for the SC4 Pilot Floating Car Data producer 
#
# 1) Build an image using this docker file. Run the following docker command
#
#    $ docker build -t lgslm/fcd-producer:v1.0.0 .
#
# 2) Test the Kafka producer for the FCD data  in a container. Run the following docker command for testing
#
#    $ docker run --rm -it --network=pilot-sc4-net --name fcd-producer lgslm/fcd-producer:v1.0.0 bash
#
#    The option --network tells docker to add this container to the same network where Kafka is available so that the host name 
#    used in producer.props file in the bootstrap.servers=kafka:9092 can be resolved. 
#    The Kafka broker, to which the producers send the data, must be configured in the server.properties file to listen to the
#    host network address assigned to it by docker, that is docker0 (not eth0). As an example if docker binds the network docker0
#    to the address 172.17.0.1 then in the server.properties file used to start a broker the listeners property must be set to
#    listeners=PLAINTEXT://172.17.0.1:9092   
#

FROM openjdk:8
MAINTAINER Luigi Selmi <luigiselmi@gmail.com>

# Install  network tools (ifconfig, netstat, ping, ip)
RUN apt-get update && \
    apt-get install -y net-tools && \
    apt-get install -y iputils-ping && \
    apt-get install -y iproute2 && \
    apt-get install -y netcat

# Install vi for editing
RUN apt-get update && \
    apt-get install -y vim


# Move to project folder
WORKDIR /home/pilot-sc4/

COPY target/fcd-producer-1.0.0-jar-with-dependencies.jar .
COPY start-producer.sh .

# Run the FCD producer
#CMD [ "./start-producer.sh" ]

