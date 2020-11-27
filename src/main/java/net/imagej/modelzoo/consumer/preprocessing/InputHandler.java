package net.imagej.modelzoo.consumer.preprocessing;

import net.imagej.modelzoo.consumer.model.prediction.ImageInput;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public interface InputHandler {
	<T extends RealType<T> & NativeType<T>> void handleImageInput(ImageInput<T> input);
}
