package net.imagej.modelzoo.consumer.model.prediction;

import net.imagej.modelzoo.consumer.ModelZooPrediction;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class ImageInput<T extends RealType<T> & NativeType<T>> implements PredictionInput {
		RandomAccessibleInterval<T> image;
		String axes;
		String name;
		public ImageInput(String name, RandomAccessibleInterval<T> input, String axes) {
			this.image = input;
			this.axes = axes;
			this.name = name;
		}

	@Override
	public void addToPrediction(ModelZooPrediction<?, ?> prediction) {
		prediction.addImageInput(this);
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
