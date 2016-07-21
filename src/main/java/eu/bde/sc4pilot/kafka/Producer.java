package eu.bde.sc4pilot.kafka;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import java.util.concurrent.Future;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Resources;

public class Producer {
  
  private static String topic = null;
  private static String sourceUri = null;
  private static final Logger log = LoggerFactory.getLogger(Producer.class);
	
	public static void main(String[] args) throws IOException {
	  
	  if (args.length < 3) {
      throw new IllegalArgumentException("A Kafka producer needs the URI of the data source. \n"
          + "It must be passed as third argument.");
    }
	  
	  topic = args[1];
	  sourceUri = args[2];
    
    // set up the producer
    KafkaProducer<String, String> producer;
    try (InputStream props = Resources.getResource("producer.props").openStream()) {
        Properties properties = new Properties();
        properties.load(props);
        producer = new KafkaProducer<>(properties);
    }
    
    try {
        
        String lastRecordSet = "";
    	  int recordSetNumber = 0;
        for (int i = 0; true; i++) {
            String recordSet = getRecordsString();
            if (! lastRecordSet.equals(recordSet) ) {
              RecordMetadata recordMetadata = producer.send(new ProducerRecord<String, String>(
                    topic,
                    Integer.toString(i), recordSet)).get();
              producer.flush();
              lastRecordSet = recordSet;
              log.info("\nSent recordset number " + recordSetNumber + "\nMetadata " + recordMetadata);
              recordSetNumber++;
            }
            else {
              System.out.print("-");
            }
            Thread.sleep(30000);
        }
    } 
    catch (Throwable throwable) {
        log.error(throwable.getStackTrace().toString());
        Thread.currentThread().interrupt();
    }
    finally {
        producer.close();
        
    }

  }
	
	public static String getRecordsString() {
		String recordsString = "";
		URLConnection certhConn = null;
    InputStream certhIs = null;
		try {
		      
        // Connect to CERTH WS
        URL certhUrl = new URL(sourceUri);
        certhConn = certhUrl.openConnection();
        certhConn.connect();
        certhIs = certhConn.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(certhIs));
        String inputLine;
        StringBuffer records = new StringBuffer();
        while ( (inputLine = reader.readLine()) != null ) {
        	records.append(inputLine);
        }
        
        recordsString = records.toString();
        
          
    }
		catch (IOException ioe) {
      ioe.printStackTrace();
    }
		finally {
      try {
        certhIs.close();
      } catch (IOException e) {        
        e.printStackTrace();
      }
    }
	
		return recordsString;
	}

}
