/*-
 * #%L
 * This is the bioimage.io modelzoo library for ImageJ.
 * %%
 * Copyright (C) 2019 - 2021 Center for Systems Biology Dresden
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
package net.imagej.modelzoo.consumer.postprocessing;

import net.imagej.modelzoo.consumer.ModelZooPredictionOptions;
import net.imagej.modelzoo.consumer.model.node.DefaultImageDataReference;
import net.imagej.modelzoo.consumer.model.node.ImageDataReference;
import net.imagej.modelzoo.consumer.model.node.ModelZooAxis;
import net.imagej.modelzoo.consumer.model.node.processor.NodeProcessor;
import net.imagej.modelzoo.consumer.model.node.OutputImageNode;
import io.bioimage.specification.TransformationSpecification;
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
	public void run(ModelZooPredictionOptions.Values options) {
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
