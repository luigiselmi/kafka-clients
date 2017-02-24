package eu.bde.pilot.sc4.nrtfcd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.api.watermark.Watermark;

import com.google.common.io.Resources;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.twitter.bijection.Injection;
import com.twitter.bijection.avro.GenericAvroCodecs;


public class NrtFcdTaxiSource implements SourceFunction<FcdTaxiEvent>{
	
  public final String FCD_THESSALONIKI_SCHEMA = "fcd-record-schema.avsc";
  //private final int watermarkDelayMSecs = 1000;
	private int maxDelayMsecs = 1000;
	private long watermarkDelayMSecs = (maxDelayMsecs < 10000) ? 10000 : maxDelayMsecs;
	private final String url;
	private final int servingSpeed;

	private transient BufferedReader reader;
	private transient InputStream is;
	
	
	public NrtFcdTaxiSource(String url, int maxEventDelaySecs, int servingSpeedFactor) {
		this.url = url;
		this.maxDelayMsecs = maxEventDelaySecs * 1000;
		this.servingSpeed = servingSpeedFactor;
	}
	
	@Override
	public void run(SourceContext<FcdTaxiEvent> sourceContext) throws Exception {
	  
    // We'll send requests to the web service every tot seconds
	  for (int i = 0; true; i++) { //infinite loop
        // add the events to the source context
    }
   
	}
	
	private void generateStream(SourceContext<FcdTaxiEvent> sourceContext) throws Exception {
		long servingStartTime = Calendar.getInstance().getTimeInMillis();
		long dataStartTime = 0;
	    long nextWatermark = 0;
	    long nextWatermarkServingTime = 0;

	    // read the first ride event
	    if (reader.ready()) {
	      String line = reader.readLine();
	      if (line != null) {
	        FcdTaxiEvent event = FcdTaxiEvent.fromString(line);

	        // set time of first event
	        dataStartTime = event.timestamp.getMillis();
	        // initialize watermarks
	        nextWatermark = dataStartTime + watermarkDelayMSecs;
	        nextWatermarkServingTime = toServingTime(servingStartTime, dataStartTime, nextWatermark);
	        // emit first event
	        sourceContext.collectWithTimestamp(event, event.timestamp.getMillis());
	      }
	    }
	    else {
	      return;
	    }

	    // read all following events
	    while (reader.ready()) {
	      String line = reader.readLine();
	      if (line != null) {

	        // read event
	        FcdTaxiEvent event = FcdTaxiEvent.fromString(line);

	        long eventTime = event.timestamp.getMillis();
	        long now = Calendar.getInstance().getTimeInMillis();
	        long eventServingTime = toServingTime(servingStartTime, dataStartTime, eventTime);

	        // get time to wait until event and next watermark needs to be emitted
	        long eventWait = eventServingTime - now;
	        long watermarkWait = nextWatermarkServingTime - now;

	        if (eventWait < watermarkWait) {
	          // wait to emit next event
	          Thread.sleep((eventWait > 0) ? eventWait : 0);
	        }
	        else if (eventWait > watermarkWait) {
	        	// wait to emit watermark
	            Thread.sleep((watermarkWait > 0) ? watermarkWait : 0);
	            // emit watermark
	            sourceContext.emitWatermark(new Watermark(nextWatermark));
	            // schedule next watermark
	            nextWatermark = nextWatermark + watermarkDelayMSecs;
	            nextWatermarkServingTime = toServingTime(servingStartTime, dataStartTime, nextWatermark);
	            // wait to emit event
	            long remainWait = eventWait - watermarkWait;
	            Thread.sleep((remainWait > 0) ? remainWait : 0);
	        }
	        else if (eventWait == watermarkWait) {
	        	// wait to emit watermark
	            Thread.sleep( (watermarkWait > 0) ? watermarkWait : 0);
	            // emit watermark
	            sourceContext.emitWatermark(new Watermark(nextWatermark - 1));
	            // schedule next watermark
	            nextWatermark = nextWatermark + watermarkDelayMSecs;
	            nextWatermarkServingTime = toServingTime(servingStartTime, dataStartTime, nextWatermark);
	        }
	        // emit event
	        sourceContext.collectWithTimestamp(event, event.timestamp.getMillis());
	      }
	}
	}
	
	public long toServingTime(long servingStartTime, long dataStartTime, long eventTime) {
		long dataDiff = eventTime - dataStartTime;
		return servingStartTime + (dataDiff / this.servingSpeed);
	}
	
	@Override
	public void cancel() {
		try {
			if (this.reader != null) {
				this.reader.close();
			}
			if (this.is != null) {
				this.is.close();
			}
		} catch(IOException ioe) {
			throw new RuntimeException("Could not cancel SourceFunction", ioe);
		} finally {
			this.reader = null;
			this.is = null;
		}
	}
	
	/*
   * This method connects to the web service and fetch the data as a set of JSON records
   * in a string
   */
  public String getRecordsString() {
    String recordsString = "";
    URLConnection conn = null;
    InputStream is = null;
    try {
          
      // Connect to CERTH WS
      URL url = new URL(this.url);
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
      ioe.printStackTrace();
    }
    finally {
      try {
        is.close();
      } catch (IOException e) {        
        e.printStackTrace();
      }
    }
  
    return recordsString;
  }
	
	/*
   * This method connects to the web service and fetch the data as a set of JSON records
   * in a string
   */
  public static String getRecordsString(BufferedReader reader) {
    String recordsString = "";
    
  
    return recordsString;
  }
	
  /**
   * Parse a string of json data and create a list of record strings
   * @param jsonString
   * @return
   */
  public ArrayList<String> getRecords(String jsonString) {
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
