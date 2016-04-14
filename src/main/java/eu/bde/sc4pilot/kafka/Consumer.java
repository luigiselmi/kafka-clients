package eu.bde.sc4pilot.kafka;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;

public class Consumer {
	
	public static void main(String[] args) throws IOException {
        // set up house-keeping
        ObjectMapper mapper = new ObjectMapper();
        
        // and the consumer
        KafkaConsumer<String, String> consumer;
        try (InputStream props = Resources.getResource("consumer.props").openStream()) {
            Properties properties = new Properties();
            properties.load(props);
            if (properties.getProperty("group.id") == null) {
                properties.setProperty("group.id", "group-" + new Random().nextInt(100000));
            }
            consumer = new KafkaConsumer<>(properties);
        }
        consumer.subscribe(Arrays.asList("taxy"));
        int timeouts = 0;
        //noinspection InfiniteLoopStatement
        while (true) {
            // read records with a short timeout. If we time out, we don't really care.
            ConsumerRecords<String, String> records = consumer.poll(100);
            if (records.count() == 0) {
                timeouts++;
            } else {
                System.out.printf("Got %d records after %d timeouts\n", records.count(), timeouts);
                timeouts = 0;
            }
            for (ConsumerRecord<String, String> record : records) {
                if(record.topic().equals("taxy")) {
                        // the send time is encoded inside the message
                        /*
                	    JsonNode msg = mapper.readTree(record.value());
                        if (msg.get("type").asText().equals("test")) {
                                long latency = (long) ((System.nanoTime() * 1e-9 - msg.get("t").asDouble()) * 1000);
                        }
                        else {                           
                                throw new IllegalArgumentException("Illegal message type: " + msg.get("type"));
                        }
                        */
                	    
                	    System.out.println(record.key() + " - " + record.value());
                }
                else {
                        throw new IllegalStateException("Shouldn't be possible to get message on topic " + record.topic());
                }
            }
        }
    }


}
