package net.imagej.modelzoo.consumer.model.prediction;

import java.util.HashMap;
import java.util.Map;

public class DefaultPredictionOutput extends HashMap<String, Object> implements PredictionOutput {
	@Override
	public Map<String, Object> asMap() {
		return this;
	}
}
