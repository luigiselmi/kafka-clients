package eu.bde.pilot.sc4.nrtfcd;

import static org.junit.Assert.*;

import org.apache.http.HttpHost;
import org.junit.Before;
import org.junit.Test;

public class FcdElasticsearchConsumerTest {

  @Before
  public void setUp() throws Exception {
  }

  @Test
  public void testGetElasticsearchNodes() {
    HttpHost [] esHosts = FcdElasticsearchConsumer.getElasticsearchNodes("es01:9200,es02:9200,es03:9200");
    assertTrue(esHosts.length == 3);
    assertTrue( "es01".equals(esHosts[0].getHostName()) && esHosts[0].getPort() == 9200);
    assertTrue( "es02".equals(esHosts[1].getHostName()) && esHosts[1].getPort() == 9200);
    assertTrue( "es03".equals(esHosts[2].getHostName()) && esHosts[2].getPort() == 9200);
    
    HttpHost [] singleNode = FcdElasticsearchConsumer.getElasticsearchNodes("es01:9200");
    assertTrue(singleNode.length == 1);
    assertTrue( "es01".equals(singleNode[0].getHostName()) && singleNode[0].getPort() == 9200);
  }

}
