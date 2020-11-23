package net.imagej.modelzoo.consumer.model.prediction;

import net.imagej.modelzoo.consumer.ModelZooPrediction;

public interface PredictionInput {
	void addToPrediction(ModelZooPrediction<?, ?> prediction);
}
