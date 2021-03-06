package eu.bde.pilot.sc4.nrtfcd;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.http.HttpHost;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Cancellable;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import com.google.common.io.Resources;
import eu.bde.pilot.sc4.nrtfcd.utils.Geohash;

public class FcdElasticsearchConsumer {
  
  private static String topic;
  private static final Logger log = LogManager.getLogger(FcdElasticsearchConsumer.class);
	
	public static void main(String[] args) throws IOException {
	  
	  if (args.length < 2) {
      throw new IllegalArgumentException("A Kafka consumer needs the name of the topic where to read the data.\n");
    }
	  
	  topic = args[1];
	    
    // Set up the consumer
    KafkaConsumer<String, byte []> consumer;
    String elasticsearchHosts = null;
    String elasticsearchIndex = null;
    try (InputStream props = Resources.getResource("elasticsearch-consumer.props").openStream()) {
        Properties properties = new Properties();
        properties.load(props);
        consumer = new KafkaConsumer<>(properties);
        
        elasticsearchHosts = properties.getProperty("elasticsearch.hosts");
        if (elasticsearchHosts == null)
          elasticsearchHosts = "localhost";
        
        elasticsearchIndex = properties.getProperty("elasticsearch.index");
        log.info("Elasticsearch hosts: " + elasticsearchHosts);
    }
    
    consumer.subscribe(Arrays.asList(topic));
    
    // Set up the schema of the messages that will be read from a kafka topic.
    Schema schema;
    try(InputStream schemaIs = Resources.getResource(FcdProducer.FCD_THESSALONIKI_SCHEMA).openStream()){
      Schema.Parser parser = new Schema.Parser();
      schema = parser.parse(schemaIs);
    }
    //Injection<GenericRecord, byte[]> recordInjection = GenericAvroCodecs.toBinary(schema);
    GenericDatumReader<GenericRecord> datumReader = new GenericDatumReader<GenericRecord>(schema);
    
    // Connects to a single node Elasticsearch
    HttpHost [] esHosts = getElasticsearchNodes(elasticsearchHosts);
    RestHighLevelClient elasticsearchClient = new RestHighLevelClient(
        RestClient.builder(esHosts)
    );
    
    try {
      int timeouts = 0;
   
      ActionListener<IndexResponse> listener = new ActionListener<IndexResponse>() {
        @Override
        public void onResponse(IndexResponse indexResponse) {
          log.debug("Response status: " + indexResponse.status().getStatus());
        }
        @Override
        public void onFailure(Exception e) {
           log.warn("Failed to index data." + e.getMessage());      
        }
      };
      
      IndexRequest indexRequest = new IndexRequest(elasticsearchIndex);
      
      while (true) {
        // read records with a short timeout. If we time out, we don't really care.
        ConsumerRecords<String, byte []> records = consumer.poll(FcdProducer.DELAY_BETWEEN_REQUESTS_SEC * 1000);
        int numberOfRecords = records.count();
        if (numberOfRecords == 0) {
            timeouts++;
        } else {
            log.debug("Got %d records after %d timeouts\n", numberOfRecords, timeouts);
            timeouts = 0;
        }

        for (ConsumerRecord<String, byte []> record : records) {
          if(record.topic().equals(topic)) {
            String key = record.key();
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(record.value());
            Decoder decoder = DecoderFactory.get().binaryDecoder(byteArrayInputStream, null);
            GenericRecord recordMessage = datumReader.read(null, decoder);
            String timestamp = recordMessage.get("timestamp").toString();
            int timestamplength = timestamp.length();
            timestamp = timestamp.substring(0, timestamplength - 4);
            double lon = (double) recordMessage.get("lon");
            double lat = (double) recordMessage.get("lat");
            String geohash = Geohash.encodeBase32(lat, lon, 30); // precision 6
            double altitude = (double) recordMessage.get("altitude");
            double speed = (double) recordMessage.get("speed");
            double orientation = (double) recordMessage.get("orientation");
            // write to Elasticsearch
            indexRequest.id(timestamp);
            String json = "{" +
              "\"geohash\":" + "\"" + geohash + "\"," + 
            	"\"timestamp\":" + "\"" + timestamp + "\"," +
            	"\"location\": \"" + lat + ", " + lon + "\"," +
            	"\"speed\":" + "\"" + speed + "\"," +
            	"\"orientation\":" + "\"" + orientation + "\"," +
            	"\"count\":" + "1" +
            	"}";
            indexRequest.source(json, XContentType.JSON);
            Cancellable indexResponse = elasticsearchClient.indexAsync(indexRequest, RequestOptions.DEFAULT, listener);
            log.debug("New record:\n" + json + "\n" + indexResponse + "\n");
          }
          else {
            throw new IllegalStateException("Shouldn't be possible to get message on topic " + record.topic());
          }
        }
        log.info("Indexed " + numberOfRecords + " records in Elasticsearch");
      }
    }
    catch (Throwable throwable) {
      log.error("Error while retrieving data from the Kafka topic.\n" + throwable.getStackTrace().toString());
    }
    finally {
      elasticsearchClient.close();
      consumer.close();  
      log.info("Closed connection to the Kafka topic and to Elasticsearch.");
    }
	}
	// Returns the Elasticsearch nodes names and ports configured
	// in the properties file
	public static HttpHost [] getElasticsearchNodes(String esHosts) {
	  String [] esHostsProperty = esHosts.split(",");
	  int numberOfhosts = esHostsProperty.length;
	  HttpHost [] esHostArray = new HttpHost[numberOfhosts];
	  int hostCounter = 0;
	  for (String hostNamePort: esHostsProperty) {
	    String hostName = hostNamePort.split(":")[0];
	    int port = Integer.parseInt(hostNamePort.split(":")[1]);
	    HttpHost host = new HttpHost(hostName, port, "http");
	    esHostArray[hostCounter] = host;
	    hostCounter++;
	  }
	  return esHostArray;
	}
}
