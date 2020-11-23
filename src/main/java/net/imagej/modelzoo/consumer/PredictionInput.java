package net.imagej.modelzoo.consumer;

public interface PredictionInput {
	void addToPrediction(ModelZooPrediction<?, ?> prediction);
}
