package net.imagej.modelzoo.consumer;

import net.imagej.modelzoo.consumer.model.prediction.ImageInput;
import net.imagej.modelzoo.consumer.model.prediction.PredictionOutput;

public interface SingleImagePrediction<O extends PredictionOutput> extends ModelZooPrediction<ImageInput<?>, O> {
}
