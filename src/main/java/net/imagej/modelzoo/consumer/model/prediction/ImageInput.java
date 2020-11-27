package net.imagej.modelzoo.consumer.model.prediction;

import net.imagej.modelzoo.consumer.preprocessing.InputHandler;
import net.imagej.modelzoo.consumer.preprocessing.InputMappingHandler;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class ImageInput<T extends RealType<T> & NativeType<T>> implements PredictionInput {

	private RandomAccessibleInterval<T> image;
	private String axes;
	private String name;

	public ImageInput(String name, RandomAccessibleInterval<T> input, String axes) {
		this.image = input;
		this.axes = axes;
		this.name = name;
	}

	@Override
	public void attachToInputHandler(InputHandler handler) {
		handler.handleImageInput(this);
	}

	public String getName() {
		return name;
	}

	public RandomAccessibleInterval<T> getImage() {
		return image;
	}

	public String getAxes() {
		return axes;
	}
}
