package net.imagej.modelzoo.consumer.postprocessing;

import net.imagej.modelzoo.consumer.model.DefaultImageDataReference;
import net.imagej.modelzoo.consumer.model.ImageDataReference;
import net.imagej.modelzoo.consumer.model.ModelZooAxis;
import net.imagej.modelzoo.consumer.model.NodeProcessor;
import net.imagej.modelzoo.consumer.model.OutputImageNode;
import net.imagej.modelzoo.specification.TransformationSpecification;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class ResizePostprocessor implements NodeProcessor<TransformationSpecification> {
	private OutputImageNode node;

	public ResizePostprocessor(OutputImageNode node) {
		this.node = node;
	}

	@Override
	public void run() {
		resize(node.getData());
	}

	private <T extends RealType<T> & NativeType<T>> void resize(ImageDataReference<T> dataReference) {
		RandomAccessibleInterval<T> img = dataReference.getData();
		img = toActualSize(img);
		img = Views.dropSingletonDimensions(img);
		node.setData(new DefaultImageDataReference<>(img, dataReference.getDataType()));
	}

	private <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> toActualSize(RandomAccessibleInterval<T> img) {

		if (node.getReference() == null) return img;

		long[] expectedSize = new long[img.numDimensions()];
		int[] mappingIndices = node.getMappingIndices();
		for (int i = 0; i < img.numDimensions(); i++) {
			Long newSize = getExpectedSize(mappingIndices[i]);
			if (newSize == null) expectedSize[i] = -1;
			else expectedSize[i] = newSize;
		}
		for (int i = 0; i < expectedSize.length; i++) {
			img = reduceDimToSize(img, i, expectedSize[i]);
		}
		return img;
	}

	private Long getExpectedSize(int mappingIndex) {
		ModelZooAxis inAxis = node.getReference().getAxes().get(mappingIndex);
		ModelZooAxis outAxis = node.getAxes().get(mappingIndex);
		Long actual = inAxis.getActual();
		Integer offset = outAxis.getOffset();
		Double scale = outAxis.getScale();
		Long newSize = actual != null ? actual : 1;
		if (scale != null) newSize = (long) (newSize * scale);
		if (offset != null) newSize += (int) offset;
		return newSize;
	}


	private <T extends RealType<T>> RandomAccessibleInterval<T> reduceDimToSize(
			final RandomAccessibleInterval<T> im, final int d, final long size) {
		final int n = im.numDimensions();
		final long[] min = new long[n];
		final long[] max = new long[n];
		im.min(min);
		im.max(max);
		max[d] += (size - im.dimension(d));
		return Views.interval(im, new FinalInterval(min, max));
	}
}
