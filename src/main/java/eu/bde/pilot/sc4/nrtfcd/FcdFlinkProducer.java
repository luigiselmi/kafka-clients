package eu.bde.pilot.sc4.nrtfcd;

import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer010;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer09;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.bde.pilot.sc4.nrtfcd.NrtFcdTaxiSource;

/**
 * This class provides the Flink execution plan for the ingestion of the near real-time floating car data
 * from a web service.
 * Flink subtasks list:
 * 
 * 1) source(), read the data from a web service
 * 2) sink(), store in Kafka topics (one for each road segment)
 *  
 * @author Luigi Selmi
 *
 */
public class FcdFlinkProducer {
  
  private static String topic = null;
  private static String sourceUrl = null;
  private static final String KAFKA_BROKER = "localhost:9092";
  public static final String FCD_THESSALONIKI_SCHEMA = "fcd-record-schema.avsc";
  private static final Logger log = LoggerFactory.getLogger(FcdProducer.class);
  private static transient DateTimeFormatter timeFormatter =
      DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");

  public static void main(String[] args) throws Exception {
    
    ParameterTool params = ParameterTool.fromArgs(args);
    String url = params.getRequired("url"); // url of the web service
    String topic = params.getRequired("topic");
    
    final int maxEventDelay = 60;       // events are out of order by max 60 seconds
    final int servingSpeedFactor = 600; // events of 10 minute are served in 1 second
    
    // set up streaming execution environment
    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
    
    // 1) Read the data from the source
    DataStream<FcdTaxiEvent> taxiEventStream = env.addSource(new NrtFcdTaxiSource(url, maxEventDelay, servingSpeedFactor));
    
    // 2) Write the data to a Kafka topic (sink) using the avro binary format
    FlinkKafkaProducer010<FcdTaxiEvent> producer = new FlinkKafkaProducer010<FcdTaxiEvent>(
        KAFKA_BROKER,
        topic,
        new FcdTaxiSchema());
    
    taxiEventStream.addSink(producer);
   
    
    // run the pipeline
    env.execute("Ingestion of Near Real-Time FCD Taxi Data");
    

  }

}
