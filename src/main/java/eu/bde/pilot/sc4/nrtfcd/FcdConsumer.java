package eu.bde.pilot.sc4.nrtfcd;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;

public class FcdConsumer {
  
  private static String topic;
  private static final Logger log = LoggerFactory.getLogger(FcdConsumer.class);
	
	public static void main(String[] args) throws IOException {
	  
	  if (args.length < 1) {
      throw new IllegalArgumentException("A Kafka consumer needs the name of the topic where to read the data.\n");
    }
	  
	  topic = args[0];
	  
    // set up house-keeping
    ObjectMapper mapper = new ObjectMapper();
    
    // Set up the consumer
    KafkaConsumer<String, byte []> consumer;
    try (InputStream props = Resources.getResource("consumer.props").openStream()) {
        Properties properties = new Properties();
        properties.load(props);
        consumer = new KafkaConsumer<>(properties);
    }
    consumer.subscribe(Arrays.asList(topic));
    
    
    // Set up the schema of the messages that will be read from a kafka topic.
    Schema schema;
    try(InputStream schemaIs = Resources.getResource(FcdProducer.FCD_THESSALONIKI_SCHEMA).openStream()){
      Schema.Parser parser = new Schema.Parser();
      schema = parser.parse(schemaIs);
    }
    //Injection<GenericRecord, byte[]> recordInjection = GenericAvroCodecs.toBinary(schema);
    GenericDatumReader<GenericRecord> datumReader = new GenericDatumReader<GenericRecord>(schema);
    
    
    int timeouts = 0;
    //noinspection InfiniteLoopStatement
    while (true) {
        // read records with a short timeout. If we time out, we don't really care.
        ConsumerRecords<String, byte []> records = consumer.poll(100);
        if (records.count() == 0) {
            timeouts++;
        } else {
            System.out.printf("Got %d records after %d timeouts\n", records.count(), timeouts);
            timeouts = 0;
        }
        for (ConsumerRecord<String, byte []> record : records) {
            if(record.topic().equals(topic)) {
              String key = record.key();
              ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(record.value());
              Decoder decoder = DecoderFactory.get().binaryDecoder(byteArrayInputStream, null);
              GenericRecord recordMessage = datumReader.read(null, decoder);
            	int deviceId = (int) recordMessage.get("device_id");
            	String timestamp = recordMessage.get("timestamp").toString();
            	double lon = (double) recordMessage.get("lon");
            	double lat = (double) recordMessage.get("lat");
            	double altitude = (double) recordMessage.get("altitude");
            	double speed = (double) recordMessage.get("speed");
            	double orientation = (double) recordMessage.get("orientation");
            	int transfer = (int) recordMessage.get("transfer");
            	log.info("key: " + key + "\n device_id: " + deviceId + "\n timestamp: " + timestamp + "\n longitude: " 
            	  + lon + "\n latitude: " + lat + "\n speed: " + speed);
            }
            else {
                    throw new IllegalStateException("Shouldn't be possible to get message on topic " + record.topic());
            }
        }
      
      
    }
}


}
