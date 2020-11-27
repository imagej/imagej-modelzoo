package net.imagej.modelzoo.consumer.model.prediction;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.util.HashMap;
import java.util.Map;

public class ImageOutput<T extends RealType<T> & NativeType<T>> implements PredictionOutput {
	private final String name;
	RandomAccessibleInterval<T> output;
	public ImageOutput(String name, RandomAccessibleInterval<T> output) {
		this.name = name;
		this.output = output;
	}
	public RandomAccessibleInterval<T> getImage() {
		return output;
	}

	@Override
	public Map<String, Object> asMap() {
		HashMap<String, Object> map = new HashMap<>();
		map.put(name, output);
		return map;
	}
}
