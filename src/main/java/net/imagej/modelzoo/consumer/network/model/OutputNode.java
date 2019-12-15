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

package net.imagej.modelzoo.consumer.network.model;

import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import java.util.Map;

public class OutputNode extends ModelZooNode {
	private InputNode reference;

	public boolean makeDataFit() {
		RandomAccessibleInterval img = null;
		try {
			img = (RandomAccessibleInterval) getData();
		} catch(ClassCastException e) { return true; }
		img = toActualSize(img);
		img = Views.dropSingletonDimensions(img);
		setData(img);
		return true;
	}

	private RandomAccessibleInterval toActualSize(RandomAccessibleInterval img) {

		if(getReference() == null) return img;

		int[] mappingIndices = getMappingIndices();
		for (int i = 0; i < img.numDimensions(); i++) {
			Map<String, Object> attrs = getReference().getAxis(mappingIndices[i]).getAttributes();
			Object actual = attrs.get("actual");
			if(actual == null) continue;
			img = reduceDimToSize(img, i, (long)actual);
		}
		return img;
	}


	private <T extends RealType<T>> RandomAccessibleInterval<T> reduceDimToSize(
			final RandomAccessibleInterval<T> im, final int d, final long size)
	{
		final int n = im.numDimensions();
		final long[] min = new long[n];
		final long[] max = new long[n];
		im.min(min);
		im.max(max);
		max[d] += (size - im.dimension(d));
		return Views.interval(im, new FinalInterval(min, max));
	}

	public void setReference(InputNode input) {
		this.reference = input;
	}

	public InputNode getReference() {
		return reference;
	}
}
