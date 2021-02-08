#!/bin/bash

until /kafka/bin/kafka-topics.sh --zookeeper $ZOOKEEPER_SERVERS --list | fgrep -q taxi ;do
    >&2 echo "XXX $0 topic taxi is unavailable - waiting"
    sleep 1
done
echo XXX $0 OK topic taxi is available
echo "KAFKA_CLIENT_TYPE=$KAFKA_CLIENT_TYPE"
if [[ $KAFKA_CLIENT_TYPE = "producer" ]]
then 
  java -jar fcd-producer-1.0.0-jar-with-dependencies.jar producer taxi http://feed.opendata.imet.gr:23577/fcd/gps.json
elif [[ $KAFKA_CLIENT_TYPE = "consumer" ]]
then 
  java -jar fcd-producer-1.0.0-jar-with-dependencies.jar consumer taxi
else
  echo "The environment variable KAFKA_CLIENT_TYPE=$KAFKA_CLIENT_TYPE must be producer or consumer"
fi
