package net.imagej.modelzoo.consumer.model.prediction;

import net.imagej.modelzoo.consumer.preprocessing.InputHandler;

public interface PredictionInput {
	void attachToInputHandler(InputHandler inputHandler);
}
