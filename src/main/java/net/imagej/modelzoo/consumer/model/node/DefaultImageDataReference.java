package net.imagej.modelzoo.consumer.model.node;

import net.imagej.modelzoo.consumer.model.node.ImageDataReference;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class DefaultImageDataReference<T extends RealType<T> & NativeType<T>> implements ImageDataReference<T> {
	private RandomAccessibleInterval<T> data;
	private T type;

	public DefaultImageDataReference(RandomAccessibleInterval<T> data, T type) {
		this.data = data;
		this.type = type;
	}

	@Override
	public RandomAccessibleInterval<T> getData() {
		return data;
	}

	@Override
	public T getDataType() {
		return type;
	}
}
