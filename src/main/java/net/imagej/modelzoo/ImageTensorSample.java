package net.imagej.modelzoo;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

public class ImageTensorSample<T extends RealType<T>> implements TensorSample {
	private final RandomAccessibleInterval<T> data;
	private final String fileName;

	public ImageTensorSample(RandomAccessibleInterval<T> data, String fileName) {
		this.data = data;
		this.fileName = fileName;
	}

	@Override
	public RandomAccessibleInterval<T> getData() {
		return data;
	}

	@Override
	public String getFileName() {
		return fileName;
	}
}
