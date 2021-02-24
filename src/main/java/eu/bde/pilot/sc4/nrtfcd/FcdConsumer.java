package eu.bde.pilot.sc4.nrtfcd;
/**
 * This class is a Kafka consumer of near real-time floating car data. It fetches data from a Kafka topic
 * in avro binary format and write the records to a log file.
 *  
 * @author Luigi Selmi
 *
 */
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
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.google.common.io.Resources;

public class FcdConsumer {
  
  private static String topic;
  //private static final Logger log = LoggerFactory.getLogger(FcdConsumer.class);
  private static final Logger log = LogManager.getLogger(FcdConsumer.class);
	
	public static void main(String[] args) throws IOException {
	  
	  if (args.length < 2) {
      throw new IllegalArgumentException("A Kafka consumer needs the name of the topic where to read the data.\n");
    }
	  
	  topic = args[1];
	    
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
    
    try {
      int timeouts = 0;
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
            	String timestamp = recordMessage.get("timestamp").toString();
            	double lon = (double) recordMessage.get("lon");
            	double lat = (double) recordMessage.get("lat");
            	double altitude = (double) recordMessage.get("altitude");
            	double speed = (double) recordMessage.get("speed");
            	double orientation = (double) recordMessage.get("orientation");
            	log.info("\n timestamp: " + timestamp + 
            	    "\n longitude: " + lon + 
            	    "\n latitude: " + lat +
            	    "\n geohash: " + key +
            	    "\n altitude: " + altitude +
            	    "\n speed: " + speed + 
            	    "\n orientation: " + orientation);
            }
            else {
                    throw new IllegalStateException("Shouldn't be possible to get message on topic " + record.topic());
            }
        }
      }
    }
    catch (Throwable throwable) {
      log.error(throwable.getStackTrace().toString());
    }
    finally {
      consumer.close();  
    }
	}
}
