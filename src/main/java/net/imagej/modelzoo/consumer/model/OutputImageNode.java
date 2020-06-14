/*-
 * #%L
 * ImageJ ModelZoo Consumer
 * %%
 * Copyright (C) 2019 MPI-CBG
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

package net.imagej.modelzoo.consumer.model;

import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class OutputImageNode<T extends RealType<T>, U extends RealType<U>> extends ImageNode<T> {
	private InputImageNode<U> reference;

	public void makeDataFit() {
		RandomAccessibleInterval<T> img = getData();
		img = toActualSize(img);
		img = Views.dropSingletonDimensions(img);
		setData(img);
	}

	private RandomAccessibleInterval<T> toActualSize(RandomAccessibleInterval<T> img) {

		if (getReference() == null) return img;

		long[] expectedSize = new long[img.numDimensions()];
		int[] mappingIndices = getMappingIndices();
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
		ModelZooAxis inAxis = getReference().getAxes().get(mappingIndex);
		ModelZooAxis outAxis = getAxes().get(mappingIndex);
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

	public void setReference(InputImageNode<U> input) {
		this.reference = input;
	}

	public InputImageNode<U> getReference() {
		return reference;
	}
}
