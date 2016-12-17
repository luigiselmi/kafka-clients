package eu.bde.pilot.sc4.nrtfcd;

import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class FcdTaxiEvent {
	
	public int deviceId = -1;
	public String timestamp;
	public double lon = 0.0;
	public double lat = 0.0;
	public double altitude = 0.0;
	public double speed = 0;
	public double orientation = 0.0;
	public int transfer = 0;
	
	public FcdTaxiEvent(int deviceId, 
			            String timestamp, 
			            double lon, 
			            double lat, 
			            double altitude, 
			            double speed, 
			            double orientation, 
			            int transfer) {
		this.deviceId = deviceId;
		this.timestamp = timestamp;
		this.lon = lon;
		this.lat = lat;
		this.altitude = altitude;
		this.speed = speed;
		this.orientation = orientation;
		this.transfer = transfer;
	}

	public FcdTaxiEvent() {}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(deviceId).append("\t");
		sb.append(timestamp + "\t");
		sb.append(lon).append("\t");
		sb.append(lat).append("\t");
		sb.append(altitude).append("\t");
		sb.append(speed).append("\t");
		sb.append(orientation).append("\t");
		sb.append(transfer);

		return sb.toString();
	}
	
	@Override
	public boolean equals(Object other) {
		return other instanceof FcdTaxiEvent &&
				this.deviceId == ((FcdTaxiEvent) other).deviceId; // the deviceId is randomly generated 
	}

	@Override
	public int hashCode() {
		return (int)this.deviceId;
	}

}
