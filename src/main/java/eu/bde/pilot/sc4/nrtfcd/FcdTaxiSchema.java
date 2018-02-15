package eu.bde.pilot.sc4.nrtfcd;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.TypeExtractor;
import org.apache.flink.streaming.util.serialization.DeserializationSchema;
import org.apache.flink.streaming.util.serialization.SerializationSchema;


public class FcdTaxiSchema implements DeserializationSchema<FcdTaxiEvent>, SerializationSchema<FcdTaxiEvent>{

	@Override
	public byte[] serialize(FcdTaxiEvent element) {
		return element.toBinary();
	}

	@Override
	public FcdTaxiEvent deserialize(byte[] message) {
		return FcdTaxiEvent.fromBinary(message);
	}

	@Override
	public boolean isEndOfStream(FcdTaxiEvent nextElement) {
		return false;
	}

	@Override
	public TypeInformation<FcdTaxiEvent> getProducedType() {
		return TypeExtractor.getForClass(FcdTaxiEvent.class);
	}
}
