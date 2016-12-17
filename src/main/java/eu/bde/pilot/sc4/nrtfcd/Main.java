package eu.bde.pilot.sc4.nrtfcd;

import java.io.IOException;

/**
 * Pick whether we want to run as producer or consumer. This lets us
 * have a single executable as a build target.
 */
public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            throw new IllegalArgumentException("Must have either 'producer' or 'consumer' as 1st argument \n"
                + "a Kafka topic as 2nd argument."
                + "A producer needs the source URI as 3rd argument. \n");
        }
        switch (args[0]) {
            case "producer":
                Producer.main(args);
                break;
            case "consumer":
                Consumer.main(args);
                break;
            default:
                throw new IllegalArgumentException("Don't know how to do " + args[0]);
        }
    }
}
