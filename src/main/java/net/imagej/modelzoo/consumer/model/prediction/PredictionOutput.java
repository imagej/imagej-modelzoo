package net.imagej.modelzoo.consumer.model.prediction;

import java.util.Map;

public interface PredictionOutput {
	Map<String, Object> asMap();
}
