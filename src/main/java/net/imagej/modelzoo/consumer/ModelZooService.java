package net.imagej.modelzoo.consumer;

import net.imagej.ImageJService;
import net.imglib2.RandomAccessibleInterval;

public interface ModelZooService extends ImageJService {
	RandomAccessibleInterval predict(String modelLocation, RandomAccessibleInterval input, String mapping);
}