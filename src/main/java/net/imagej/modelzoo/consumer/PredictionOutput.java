package net.imagej.modelzoo.consumer;

import java.util.Map;

public interface PredictionOutput {
	Map<String, Object> asMap();
}
