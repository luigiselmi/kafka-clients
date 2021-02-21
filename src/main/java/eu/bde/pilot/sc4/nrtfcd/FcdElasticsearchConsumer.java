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
import com.google.common.io.Resources;

public class FcdElasticsearchConsumer {
  
  private static String topic;
  //private static final Logger log = LoggerFactory.getLogger(FcdElasticsearchConsumer.class);
	
	public static void main(String[] args) throws IOException {
	  
	  if (args.length < 2) {
      throw new IllegalArgumentException("A Kafka consumer needs the name of the topic where to read the data.\n");
    }
	  
	  topic = args[1];
	    
    // Set up the consumer
    KafkaConsumer<String, byte []> consumer;
    String elasticsearchHostName = null;
    try (InputStream props = Resources.getResource("elasticsearch-consumer.props").openStream()) {
        Properties properties = new Properties();
        properties.load(props);
        consumer = new KafkaConsumer<>(properties);
        elasticsearchHostName = properties.getProperty("elasticsearch.name");
        if (elasticsearchHostName == null)
          elasticsearchHostName = "localhost"; 
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
    
    RestHighLevelClient elasticsearchClient = new RestHighLevelClient(
        RestClient.builder(
                new HttpHost(elasticsearchHostName, 9200, "http"),
                new HttpHost(elasticsearchHostName, 9201, "http")));
    
    try {
   
      int timeouts = 0;
      
      ActionListener<IndexResponse> listener = new ActionListener<IndexResponse>() {
        @Override
        public void onResponse(IndexResponse indexResponse) {
          System.out.println("Response status: " + indexResponse.status().getStatus());
            
        }

        @Override
        public void onFailure(Exception e) {
           e.printStackTrace();      
        }
      };
      
      IndexRequest indexRequest = new IndexRequest("thessaloniki");
      
      while (true) {
        // read records with a short timeout. If we time out, we don't really care.
        ConsumerRecords<String, byte []> records = consumer.poll(1000);
        if (records.count() == 0) {
            timeouts++;
        } else {
            System.out.printf("Got %d records after %d timeouts\n", records.count(), timeouts);
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
            	double altitude = (double) recordMessage.get("altitude");
            	double speed = (double) recordMessage.get("speed");
            	double orientation = (double) recordMessage.get("orientation");
            	// write to Elasticsearch
            	indexRequest.id(timestamp);
            	String json = "{" +
            	    "\"geohash\":" + "\"" + "luigi" + "\"," + 
            	    "\"timestamp\":" + "\"" + timestamp + "\"," +
            	    "\"location\": \"" + lat + ", " + lon + "\"," +
            	    "\"speed\":" + "\"" + speed + "\"," +
            	    "\"orientation\":" + "\"" + orientation + "\"," +
            	    "\"count\":" + "1" +
            	  "}";
              indexRequest.source(json, XContentType.JSON);
            	Cancellable indexResponse = elasticsearchClient.indexAsync(indexRequest, RequestOptions.DEFAULT, listener);
            	//log.info("new record created at" + timestamp);
            }
            else {
                 throw new IllegalStateException("Shouldn't be possible to get message on topic " + record.topic());
            }
        }
      }
    }
    catch (Throwable throwable) {
      //log.error(throwable.getStackTrace().toString());
    }
    finally {
      elasticsearchClient.close();
      consumer.close();  
    }
	}
}
