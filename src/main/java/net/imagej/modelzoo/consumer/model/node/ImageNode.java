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
package net.imagej.modelzoo.consumer.model.node;

import net.imagej.axis.AxisType;
import net.imglib2.EuclideanSpace;
import org.scijava.util.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

public class ImageNode extends DefaultModelZooNode<ImageDataReference<?>> implements EuclideanSpace {

	private final List<ModelZooAxis> axes = new ArrayList<>();
	private List<AxisType> mapping;

	@Override
	public int numDimensions() {
		return axes.size();
	}

	public void clearAxes() {
		axes.clear();
	}

	public void addAxis(ModelZooAxis axis) {
		axes.add(axis);
	}

	public AxisType[] getAxesArray() {
		AxisType[] res = new AxisType[axes.size()];
		for (int i = 0; i < res.length; i++) {
			res[i] = axes.get(i).getType();
		}
		return res;
	}

	public AxisType[] getDataAxesArray() {
		AxisType[] res = new AxisType[getData().getData().numDimensions()];
		for (int i = 0; i < res.length; i++) {
			res[i] = getDataAxis(i).getType();
		}
		return res;
	}

	public void setDataMapping(List<AxisType> axisTypes) {
		this.mapping = axisTypes;
	}

	public List<AxisType> getDataMapping() {
		return mapping;
	}

	public int[] getMappingIndices() {
		int[] res = new int[axes.size()];

		int i = 0;
		// set mapping for axes which exist in the input image
		for (; mapping != null && i < mapping.size(); i++) {
			AxisType axis = mapping.get(i);
			for (int j = 0; j < axes.size(); j++) {
				if (axes.get(j).getType().equals(axis)) {
					res[i] = j;
				}
			}
		}
		// set mapping for axes which will be added to the image to fit the tensor
		for (int j = i; j < res.length; j++) {
			for (int k = 0; k < res.length; k++) {
				if (!ArrayUtils.contains(res, k)) res[j] = k;
			}
		}
		return res;
	}

	public ModelZooAxis getDataAxis(int index) {
		return getAxes().get(getMappingIndices()[index]);
	}

	public List<ModelZooAxis> getAxes() {
		return axes;
	}

	@Override
	public boolean accepts(Object data) {
		return ImageDataReference.class.isAssignableFrom(data.getClass());
	}
}

