package eu.bde.pilot.sc4.nrtfcd;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.common.io.Resources;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.twitter.bijection.Injection;
import com.twitter.bijection.avro.GenericAvroCodecs;


public class FcdTaxiEventBuilder {
	
  private static transient DateTimeFormatter timeFormatter =
			DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");
	
  public static final String FCD_THESSALONIKI_SCHEMA = "fcd-record-schema.avsc";
  private static Schema schema = null; // avro schema used to send the messages in binary format to a Kafka topic
	
	/*
	 * Creates one event from a json string
	 */
	public static FcdTaxiEvent fromJsonString(String jsonString) {
	  FcdTaxiEvent event = new FcdTaxiEvent();
    JsonParser parser = new JsonParser();
    JsonElement element = parser.parse(jsonString);
    JsonObject jsonRecord = element.getAsJsonObject();
    
    String timestamp = jsonRecord.get("recorded_timestamp").getAsString();
    event.timestamp = DateTime.parse(timestamp, timeFormatter);
    event.lon = jsonRecord.get("lon").getAsDouble();
    event.lat = jsonRecord.get("lat").getAsDouble();
    event.altitude = jsonRecord.get("altitude").getAsDouble();
    event.speed = jsonRecord.get("speed").getAsInt();
    event.orientation = jsonRecord.get("orientation").getAsDouble();
    
    return event;
	}
	
	/*
	 * Creates one event from a tab separated string. This is used for the historical data.
	 */
	public static FcdTaxiEvent fromString(String line) {
	  String[] tokens = line.split("\t");
    if (tokens.length != 8) {
      throw new RuntimeException("Invalid record: " + line);
    }

    FcdTaxiEvent event = new FcdTaxiEvent();

    try {
      event.deviceId = Integer.parseInt(tokens[0]);
      event.timestamp = DateTime.parse(tokens[1], timeFormatter);
      event.lon = tokens[2].length() > 0 ? Double.parseDouble(tokens[2]) : 0.0;
      event.lat = tokens[3].length() > 0 ? Double.parseDouble(tokens[3]) : 0.0;
      event.altitude = tokens[4].length() > 0 ? Double.parseDouble(tokens[4]) : 0.0;
      event.speed = tokens[5].length() > 0 ? Double.parseDouble(tokens[5]) : 0.0;
      event.orientation = tokens[6].length() > 0 ? Double.parseDouble(tokens[7]) : 0.0;

    } catch (NumberFormatException nfe) {
      throw new RuntimeException("Invalid record: " + line, nfe);
    }

    return event;
	}
	
	/*
   * Set up the schema of the messages that will be sent to a kafka topic.
   * Must be called before any transformation from json to binary or vice versa.
   */
  public static void setSchema() {
    try(
      InputStream schemaIs = Resources.getResource(FCD_THESSALONIKI_SCHEMA).openStream()){
      Schema.Parser parser = new Schema.Parser();
      schema = parser.parse(schemaIs);
    }
    catch(IOException ioe){
      ioe.printStackTrace();
    }
  }
  /*
   * Serialize the JSON data into the Avro binary format
   */
  public static byte [] toBinary(FcdTaxiEvent event) {
    if(schema == null) {
      setSchema();
    }
    GenericData.Record avroRecord = new GenericData.Record(schema);
    Injection<GenericRecord, byte[]> binaryRecord = GenericAvroCodecs.toBinary(schema);
    avroRecord.put("device_id", event.getDeviceId());
    avroRecord.put("timestamp", event.getTimestamp().toString(timeFormatter));
    avroRecord.put("lon", event.getLon());
    avroRecord.put("lat", event.getLat());
    avroRecord.put("altitude", event.getAltitude());
    avroRecord.put("speed", event.getSpeed());
    avroRecord.put("orientation", event.getOrientation());
    byte[] value = binaryRecord.apply(avroRecord);
    return value;
  }
  
  public static FcdTaxiEvent fromBinary(byte [] avro) {
    if(schema == null) {
      setSchema();
    }
    FcdTaxiEvent event = new FcdTaxiEvent();
    Injection<GenericRecord, byte []> recordInjection = GenericAvroCodecs.toBinary(schema);
    GenericRecord jsonRecord = recordInjection.invert(avro).get();
    int device_id = (Integer) jsonRecord.get("device_id");
    event.deviceId = device_id;
    String timestamp = ((Utf8)jsonRecord.get("timestamp")).toString();
    event.timestamp = DateTime.parse(timestamp, timeFormatter);
    event.lon = (Double) jsonRecord.get("lon");
    event.lat = (Double) jsonRecord.get("lat");
    event.altitude = (Double) jsonRecord.get("altitude");
    event.speed = (Double) jsonRecord.get("speed");
    event.orientation = (Double) jsonRecord.get("orientation");    
    return event;
  }
  
  /**
   * Parse a string of json data and create a list of json strings
   * @param jsonString
   * @return
   */
  public static ArrayList<String> getJsonRecords(String jsonString) {
    ArrayList<String> recordsList = new ArrayList<String>();
    JsonParser parser = new JsonParser();
    JsonElement element = parser.parse(jsonString);
    if (element.isJsonArray()) {
      JsonArray jsonRecords = element.getAsJsonArray();        
      for (int i = 0; i < jsonRecords.size(); i++) {   
        JsonObject jsonRecord = jsonRecords.get(i).getAsJsonObject();
        String recordString = jsonRecord.toString();
        recordsList.add(recordString);
      }
    }
    
    return recordsList;
  }
  

}
