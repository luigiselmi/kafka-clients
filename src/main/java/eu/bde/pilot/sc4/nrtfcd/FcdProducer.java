package eu.bde.pilot.sc4.nrtfcd;
/**
 * This class is a Kafka producer of near real-time floating car data. It fetches data from a web service
 * and send the records in a Kafka topic using the avro binary format. 
 * @author Luigi Selmi
 *
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.google.common.io.Resources;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.bde.pilot.sc4.nrtfcd.utils.Geohash;

public class FcdProducer {
  
  private static String topic = null;
  private static String sourceUrl = null;
  public static final String FCD_THESSALONIKI_SCHEMA = "fcd-record-schema.avsc";
  
  //private static final Logger log = LoggerFactory.getLogger(FcdProducer.class);
  private static final Logger log = LogManager.getLogger(FcdProducer.class);
  
  private static transient DateTimeFormatter timeFormatter =
      DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");
	
  public static void main(String[] args) throws IOException {
	  
	  if (args.length < 2) {
      throw new IllegalArgumentException("A Kafka producer needs the URI of the data source.\n");
    }
	 
	  topic = args[1];
	  sourceUrl = args[2];
	  //Integer partition = 0;
    
	  // Set up the producer<key,value>. The key is a string value computed as the geohash of the coordinates pair.
	  // It can be used to select the topic's partition to which a message will be sent.
    KafkaProducer<String, byte []> producer;
    try (InputStream props = Resources.getResource("producer.props").openStream()) {
        Properties properties = new Properties();
        properties.load(props);
        producer = new KafkaProducer<String, byte []>(properties);
    }
    
    try {
      String lastJsonString = "";
      int recordSetNumber = 0;
      String jsonString = ""; 
      while(true) {//infinite loop
         jsonString = getRecordsString();
         if(jsonString != null && ! "".equals(jsonString) && jsonString.startsWith("[{")) {
           ArrayList<String> recordsList = getRecords(jsonString);
           if (! lastJsonString.equals(jsonString) ) {
             Iterator<String> irecords = recordsList.iterator();
             while(irecords.hasNext()) {
               FcdTaxiEvent event = FcdTaxiEventUtils.fromJsonString(irecords.next());
               byte[] value = event.toBinary();
               Long timestamp = event.timestamp.getMillis();
               String key = Geohash.encodeBase32(event.lat, event.lon, 20); // bits = 20 -> precision 4
               ProducerRecord<String, byte []> record = new ProducerRecord<String, byte []>(topic, key, value);
               RecordMetadata metadata = producer.send(record).get();
               producer.flush();
               lastJsonString = jsonString;
               log.info("\nSent recordset number " + recordSetNumber + " timestamp: " + timestamp);
               recordSetNumber++;
             }
           }
           else {
              log.warn("\nNo records.");
           }
          
           Thread.sleep(30000); //wait for the new data to be available
         }
      }
    } 
    catch (Throwable throwable) {
        log.error(throwable.getStackTrace().toString());
        //Thread.currentThread();
    }
    finally {
        producer.close();
        
    }

  }
	/*
	 * This method connects to the web service and fetch the data as a set of JSON records
	 * in a string
	 */
	public static String getRecordsString() {
	  String recordsString = null;
	  URLConnection conn = null;
	  InputStream is = null;
	  try {
		      
		  // Connect to CERTH WS
		  URL url = new URL(sourceUrl);
		  conn = url.openConnection();
		 
		  conn.connect();
		  is = conn.getInputStream();
		  BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		  String inputLine;
		  StringBuffer records = new StringBuffer();
		  while ( (inputLine = reader.readLine()) != null ) {
		  	records.append(inputLine);
		  }
		  
		  recordsString = records.toString();
	      
		}
			catch (IOException ioe) {
			  log.debug(ioe.getMessage());
		}
	    finally {
		  try {
		    is.close();
		  } catch (IOException e) {        
		    log.debug(e.getMessage());
		  }
		}
	
		return recordsString;
	}
	
	/**
	 * Parse a string of json data and create a list of record strings
	 * @param jsonString
	 * @return
	 */
	public static ArrayList<String> getRecords(String jsonString) {
    ArrayList<String> recordsList = new ArrayList<String>();
    JsonParser parser = new JsonParser();
    JsonElement element = parser.parse(jsonString);
    if (element.isJsonArray()) {
      JsonArray jsonRecords = element.getAsJsonArray();        
      for (int i = 0; i < jsonRecords.size(); i++) {   
        JsonObject jsonRecord = jsonRecords.get(i).getAsJsonObject();
        String recordString = jsonRecord.toString();
        log.debug(recordString);
        recordsList.add(recordString);
      }
    }
    
    return recordsList;
  }
	
}
