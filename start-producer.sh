#!/bin/bash

until /app/bin/kafka-topics.sh --zookeeper $ZOOKEEPER_SERVERS --list | fgrep -q taxi ;do
    >&2 echo "XXX $0 topic taxi is unavailable - waiting"
    sleep 1
done
echo XXX $0 OK topic taxi is available

java -jar pilot-sc4-kafka-producer-0.0.1-SNAPSHOT-jar-with-dependencies.jar producer taxi http://feed.opendata.imet.gr:23577/fcd/gps.json
