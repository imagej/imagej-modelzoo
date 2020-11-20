package net.imagej.modelzoo.consumer.model;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public interface ImageDataReference<T extends RealType<T> & NativeType<T>> {
	RandomAccessibleInterval<T> getData();
	T getDataType();
}
