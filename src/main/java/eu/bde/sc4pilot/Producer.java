package eu.bde.sc4pilot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import com.google.common.io.Resources;

public class Producer {
	
	public static void main(String[] args) throws IOException {
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
            for (int i = 0; i < 100; i++) {
                String recordSet = getRecordsString();
                if (! lastRecordSet.equals(recordSet) ) {
                  producer.send(new ProducerRecord<String, String>(
                        "taxy-data",
                        Integer.toString(i), recordSet));
                  producer.flush();
                  lastRecordSet = recordSet;
                  System.out.println("\nSent recordset number " + recordSetNumber);
                  recordSetNumber++;
                }
                else {
                  System.out.print("-");
                }
                Thread.sleep(30000);
            }
        } 
        catch (Throwable throwable) {
            System.out.printf("%s", throwable.getStackTrace());
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
        URL certhUrl = new URL("http://feed.opendata.imet.gr:23577/fcd/gps.json");
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
        //System.out.println("data size: " + recordsString.length());
          
    }
		catch (IOException ioe) {
      ioe.printStackTrace();
    }
		finally {
      try {
        certhIs.close();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
	
		return recordsString;
	}

}
