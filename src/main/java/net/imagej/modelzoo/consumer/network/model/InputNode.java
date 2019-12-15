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

import net.imagej.axis.AxisType;
import net.imagej.modelzoo.consumer.task.Task;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class InputNode extends ModelZooNode {

	//TODO this is ugly
	public boolean makeDataFit() {
		Object data = getData();
		int[] mappingIndices = getMappingIndices();
		try {
			RandomAccessibleInterval img = (RandomAccessibleInterval) data;

			img = addAxesIfNeeded(img);

			for (int i = 0; i < img.numDimensions(); i++) {
				Map<String, Object> attrs = getAxis(mappingIndices[i]).getAttributes();
				int min = (int) attrs.get("min");
				Object step = attrs.get("step");
				long size = img.dimension(i);
				long newsize = size;
				if(size < min) {
					newsize = min;
				} else {
					if(step == null) {
						continue;
					}
					if((int)step == 0) {
						if(size != min) {
							System.out.println("Input " + getName() + " dimension " + i + " should have size " + min + " but is " + size);
							return false;
						} else {
							continue;
						}
					} else {
						long rest = (size - min) % (int)step;
						if(rest != 0) {
							newsize = size - rest + (int)step;
						}
					}
				}
				img = expandDimToSize(img, i, newsize);
				attrs.put("actual", size);

			}
			setData(img);
		} catch(ClassCastException ignored) {}
		return true;
	}

	private RandomAccessibleInterval addAxesIfNeeded(RandomAccessibleInterval img) {
		AxisType[] axes = getAxesArray();
		while(img.numDimensions() < axes.length) {
			img = Views.addDimension(img, 0, 0);
		}
		return img;
	}

	private <T extends RealType<T>> RandomAccessibleInterval<T> expandDimToSize(
			final RandomAccessibleInterval<T> im, final int d, final long size)
	{
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
