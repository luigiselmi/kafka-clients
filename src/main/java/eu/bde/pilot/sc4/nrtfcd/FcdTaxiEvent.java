package eu.bde.pilot.sc4.nrtfcd;

import org.joda.time.DateTime;

public class FcdTaxiEvent {
  
  public int deviceId = 1;
  public DateTime timestamp;
  public double lon = 0.0;
  public double lat = 0.0;
  public double altitude = 0.0;
  public double speed = 0;
  public double orientation = 0.0;
  
  public FcdTaxiEvent() {}
  
  public FcdTaxiEvent(int deviceId, 
                  DateTime timestamp, 
                  double lon, 
                  double lat, 
                  double altitude, 
                  double speed, 
                  double orientation) {
    this.deviceId = deviceId;
    this.timestamp = timestamp;
    this.lon = lon;
    this.lat = lat;
    this.altitude = altitude;
    this.speed = speed;
    this.orientation = orientation;
  }
  
  public int getDeviceId() {
    return deviceId;
  }
  public void setDeviceId(int deviceId) {
    this.deviceId = deviceId;
  }
  public DateTime getTimestamp() {
    return timestamp;
  }
  public void setTimestamp(DateTime timestamp) {
    this.timestamp = timestamp;
  }
  public double getLon() {
    return lon;
  }
  public void setLon(double lon) {
    this.lon = lon;
  }
  public double getLat() {
    return lat;
  }
  public void setLat(double lat) {
    this.lat = lat;
  }
  public double getAltitude() {
    return altitude;
  }
  public void setAltitude(double altitude) {
    this.altitude = altitude;
  }
  public double getSpeed() {
    return speed;
  }
  public void setSpeed(double speed) {
    this.speed = speed;
  }
  public double getOrientation() {
    return orientation;
  }
  public void setOrientation(double orientation) {
    this.orientation = orientation;
  }
  
  /**
   * Two events are the indistinguishable if the location (lat, lon) and the timestamp are the same
   */
  @Override
  public boolean equals(Object other) {
    return other instanceof FcdTaxiEvent &&
      this.timestamp.equals( ((FcdTaxiEvent) other).timestamp) &&
      this.lon == ((FcdTaxiEvent) other).lon &&
      this.lat == ((FcdTaxiEvent) other).lat ;
  }

  @Override
  public int hashCode() {
    return this.deviceId;
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(deviceId).append("\t");
    sb.append(timestamp.toString()).append("\t");
    sb.append(lon).append("\t");
    sb.append(lat).append("\t");
    sb.append(altitude).append("\t");
    sb.append(speed).append("\t");
    sb.append(orientation).append("\t");

    return sb.toString();
  }
}
