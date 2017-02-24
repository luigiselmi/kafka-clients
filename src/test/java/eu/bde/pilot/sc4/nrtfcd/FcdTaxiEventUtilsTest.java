package eu.bde.pilot.sc4.nrtfcd;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import com.google.common.io.Resources;

public class FcdTaxiEventUtilsTest {
  
  String jsonStringRecords;
 

  @Before
  public void setUp() throws Exception {
    InputStream is = Resources.getResource("gps-sample-data.json").openStream();
    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    String inputLine;
    StringBuffer records = new StringBuffer();
    while ( (inputLine = reader.readLine()) != null ) {
      records.append(inputLine);
    }
    
    jsonStringRecords = records.toString();
  }

  @Test
  public void testFromJsonString() {
    ArrayList<String> jsonRecords = FcdTaxiEventUtils.getJsonRecords(jsonStringRecords);
    String json = jsonRecords.get(0);
    FcdTaxiEvent event = FcdTaxiEventUtils.fromJsonString(json);
    assertTrue(event.deviceId == 79163);
  }
  
  @Test
  public void testGetJsonRecords() {
    ArrayList<String> jsonRecords = FcdTaxiEventUtils.getJsonRecords(jsonStringRecords);
    assertTrue(jsonRecords.size() > 0);
  }

}
