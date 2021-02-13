#!/bin/bash

until /kafka/bin/kafka-topics.sh --zookeeper $ZOOKEEPER_SERVERS --list | fgrep -q $TOPIC ;do
    >&2 echo "XXX $0 topic $TOPIC is unavailable - waiting"
    sleep 1
done
echo XXX $0 OK topic taxi is available
echo "KAFKA_CLIENT_TYPE=$KAFKA_CLIENT_TYPE"
if [[ $KAFKA_CLIENT_TYPE = "producer" ]]
then 
  java -jar fcd-kafka-clients-1.0.0-jar-with-dependencies.jar producer $TOPIC http://feed.opendata.imet.gr:23577/fcd/gps.json
elif [[ $KAFKA_CLIENT_TYPE = "consumer" ]]
then 
  java -jar fcd-kafka-clients-1.0.0-jar-with-dependencies.jar consumer $TOPIC
else
  echo "The environment variable KAFKA_CLIENT_TYPE=$KAFKA_CLIENT_TYPE must be producer or consumer"
fi
