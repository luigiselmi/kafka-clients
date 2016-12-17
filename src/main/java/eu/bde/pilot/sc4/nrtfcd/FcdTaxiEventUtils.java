package eu.bde.pilot.sc4.nrtfcd;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class FcdTaxiEventUtils {
  
  private static transient DateTimeFormatter timeFormatter =
      DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");
  
  public static FcdTaxiEvent fromJsonString(String jsonString) {
    FcdTaxiEvent event = new FcdTaxiEvent();
    JsonParser parser = new JsonParser();
    JsonElement element = parser.parse(jsonString);
    JsonObject jsonRecord = element.getAsJsonObject();
    if( jsonRecord.get("device_random_id").getAsString().equals("")){
      return null;
    }
    else {
      event.deviceId = jsonRecord.get("device_random_id").getAsInt();  
    }
    String timestamp = jsonRecord.get("recorded_timestamp").getAsString();
    event.timestamp = timestamp;
    event.lon = jsonRecord.get("lon").getAsDouble();
    event.lat = jsonRecord.get("lat").getAsDouble();
    event.altitude = jsonRecord.get("altitude").getAsDouble();
    event.speed = jsonRecord.get("speed").getAsInt();
    event.orientation = jsonRecord.get("orientation").getAsDouble();
    event.transfer = jsonRecord.get("transfer").getAsInt();
    return event;
  }
  
  public static FcdTaxiEvent fromString(String line) {

    String[] tokens = line.split("\t");
    if (tokens.length != 8) {
      throw new RuntimeException("Invalid record: " + line);
    }

    FcdTaxiEvent event = new FcdTaxiEvent();

    try {
      event.deviceId = Integer.parseInt(tokens[0]);
      event.timestamp = tokens[1];
      event.lon = tokens[2].length() > 0 ? Double.parseDouble(tokens[2]) : 0.0;
      event.lat = tokens[3].length() > 0 ? Double.parseDouble(tokens[3]) : 0.0;
      event.altitude = tokens[4].length() > 0 ? Double.parseDouble(tokens[4]) : 0.0;
      event.speed = tokens[5].length() > 0 ? Double.parseDouble(tokens[5]) : 0.0;
      event.orientation = tokens[6].length() > 0 ? Double.parseDouble(tokens[7]) : 0.0;
      event.transfer = tokens[7].length() > 0 ? Integer.parseInt(tokens[7]) : 0;

    } catch (NumberFormatException nfe) {
      throw new RuntimeException("Invalid record: " + line, nfe);
    }

    return event;
  }
  
  
  
}
