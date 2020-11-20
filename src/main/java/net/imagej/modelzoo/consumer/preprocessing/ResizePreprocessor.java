package net.imagej.modelzoo.consumer.preprocessing;

import net.imagej.axis.AxisType;
import net.imagej.modelzoo.consumer.model.DefaultImageDataReference;
import net.imagej.modelzoo.consumer.model.ImageDataReference;
import net.imagej.modelzoo.consumer.model.ImageNode;
import net.imagej.modelzoo.consumer.model.ModelZooAxis;
import net.imagej.modelzoo.consumer.model.NodeProcessor;
import net.imagej.modelzoo.consumer.model.NodeProcessorException;
import net.imagej.modelzoo.specification.TransformationSpecification;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import org.scijava.log.LogService;

public class ResizePreprocessor implements NodeProcessor<TransformationSpecification> {
	private ImageNode node;
	private LogService log;

	public ResizePreprocessor(ImageNode node, LogService log) {
		this.node = node;
		this.log = log;
	}

	@Override
	public void run() throws NodeProcessorException {
		resize(node.getData());
	}

	private <T extends RealType<T> & NativeType<T>> void resize(ImageDataReference<T> dataReference) throws NodeProcessorException {
		RandomAccessibleInterval<T> img = dataReference.getData();
		int[] mappingIndices = node.getMappingIndices();

		img = addAxesIfNeeded(img);

		for (int i = 0; i < img.numDimensions(); i++) {
			ModelZooAxis axis = node.getAxes().get(mappingIndices[i]);
			Integer min = axis.getMin();
			if(min == null) min = 0;
			Object step = axis.getStep();
			long size = img.dimension(i);
			long newsize = size;
			if (size < min) {
				newsize = min;
			} else {
				if (step == null) {
					axis.setActual(size);
					continue;
				}
				if ((int) step == 0) {
					if (size != min) {
						throw new NodeProcessorException(getClass().getSimpleName()
								+ ": Input \"" + node.getName() + "\" dimension " + i
								+ " should have size " + min + " but is " + size);
					} else {
						continue;
					}
				} else {
					long rest = (size - min) % (int) step;
					if (rest != 0) {
						newsize = size - rest + (int) step;
					}
				}
			}
			img = expandDimToSize(img, i, newsize);
			axis.setActual(size);
		}
		node.setData(new DefaultImageDataReference<>(img, dataReference.getDataType()));
	}

	private <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> addAxesIfNeeded(RandomAccessibleInterval<T> img) {
		AxisType[] axes = node.getAxesArray();
		while (img.numDimensions() < axes.length) {
			img = Views.addDimension(img, 0, 0);
		}
		return img;
	}

	private <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> expandDimToSize(
			final RandomAccessibleInterval<T> im, final int d, final long size) {
		final int n = im.numDimensions();
		final long[] min = new long[n];
		final long[] max = new long[n];
		im.min(min);
		im.max(max);
		max[d] += (size - im.dimension(d));
		return Views.interval(Views.extendMirrorDouble(im), new FinalInterval(min,
				max));
	}
}
