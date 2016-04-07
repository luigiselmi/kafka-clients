Kafka Clients [![Build Status](https://travis-ci.org/luigiselmi/kafka-clients.svg?branch=master)](https://travis-ci.org/luigiselmi/kafka-clients)
=====================
Producer and consumer of traffic data for Apache Kafka. 

## Description
The project creates a producer and a consumer of a Kafka topic. The producer fetches data from a source and sends the data 
to the topic. A consumer is able to read and parse the data from the topic for processing and storage.

##Documentation 
This project is a component of the pilot that address the 4th H2020 societal challenge: Smart Green and Integrated Transport. 
The pilot will provide a scalable and fault tolerant system to collect, process and store the data from sensors: GPS data from 
cabs and data from Bluetooth sensors all in the city of Thessaloniki, Greece.

##Requirements 
Producer and consumer write and read data from a Kafka topic, so they both depend on Kafka. Zookeeper and at least a Kafka server must be 
started before running a producer or consumer and the topic used by them must be created as in the Apache kafka documentation.
The project is based on Maven. The default topic for the cab data is "taxy".

##Build 
The software is built from the project root folder simply running the command Maven

    mvn install

##Install and Run 
The build creates a jar file with all the dependences and the configuration of the main class in the target folder. 
To start the producer from the root folder run the command

   java jar target/kafka-clients-0.0.1-SNAPSHOT-jar-with-dependencies.jar producer
   
The producer will start to read the traffic data from the source and write it to the topic "taxy". To start the consumer simply 
run again the same command as above passing "consumer" as argument instead of "producer".

##Usage 
section with a description and examples of the main use cases and the APIs exposed by the software

##License 
section about the type of license that applies 
