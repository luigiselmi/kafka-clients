package eu.bde.pilot.sc4.nrtfcd;

import java.io.IOException;

/**
 * Pick whether we want to run as producer or consumer. This lets us
 * have a single executable as a build target.
 */
public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            throw new IllegalArgumentException("Must have either 'producer' or 'consumer' as 1st argument \n"
                + "a Kafka topic as 2nd argument.\n");
        }
        switch (args[0]) {
            case "producer":
                FcdProducer.main(args);
                break;
            case "consumer":
                FcdConsumer.main(args);
                break;
            case "consumer-elasticsearch":
              FcdElasticsearchConsumer.main(args);
              break;
            default:
                throw new IllegalArgumentException("The client name " + args[0] + " does not exist.");
        }
    }
}
