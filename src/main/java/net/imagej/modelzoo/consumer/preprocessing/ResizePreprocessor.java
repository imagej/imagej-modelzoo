/*-
 * #%L
 * This is the bioimage.io modelzoo library for ImageJ.
 * %%
 * Copyright (C) 2019 - 2020 Center for Systems Biology Dresden
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package net.imagej.modelzoo.consumer.preprocessing;

import net.imagej.axis.AxisType;
import net.imagej.modelzoo.consumer.ModelZooPredictionOptions;
import net.imagej.modelzoo.consumer.model.node.DefaultImageDataReference;
import net.imagej.modelzoo.consumer.model.node.ImageDataReference;
import net.imagej.modelzoo.consumer.model.node.ImageNode;
import net.imagej.modelzoo.consumer.model.node.ModelZooAxis;
import net.imagej.modelzoo.consumer.model.node.processor.NodeProcessor;
import net.imagej.modelzoo.consumer.model.node.processor.NodeProcessorException;
import io.bioimage.specification.TransformationSpecification;
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
	public void run(ModelZooPredictionOptions.Values options) throws NodeProcessorException {
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
