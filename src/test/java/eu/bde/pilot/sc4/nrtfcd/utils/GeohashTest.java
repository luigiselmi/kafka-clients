package eu.bde.pilot.sc4.nrtfcd.utils;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class GeohashTest {

  @Before
  public void setUp() throws Exception {
  }

  @Test
  public void testEncodeBase32() {
    double latitude = 40.7049016666667;
    double longitude = 22.93806;
    String geohash = Geohash.encodeBase32(latitude, longitude, 20); // bits = 20 -> precision 4
    assertTrue(geohash.length() == 4);
    geohash = Geohash.encodeBase32(latitude, longitude, 25); // bits = 25 -> precision 5
    assertTrue(geohash.length() == 5);
    geohash = Geohash.encodeBase32(latitude, longitude, 30); // bits = 30 -> precision 6
    assertTrue(geohash.length() == 6);
  }

}
