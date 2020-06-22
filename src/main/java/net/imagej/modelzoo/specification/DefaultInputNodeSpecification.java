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
package net.imagej.modelzoo.specification;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultInputNodeSpecification extends DefaultNodeSpecification implements InputNodeSpecification {

	private final static String idNodeShapeMin = "min";
	private final static String idNodeShapeStep = "step";

	private List<Integer> shapeMin;
	private List<Integer> shapeStep;

	@Override
	public Map<String, Object> getShape() {
		Map<String, Object> shape = new LinkedHashMap<>();
		if (shapeMin != null) shape.put(idNodeShapeMin, shapeMin);
		if (shapeStep != null) shape.put(idNodeShapeStep, shapeStep);
		return shape;
	}

	@Override
	protected void setShape(Map<String, Object> data) {
		if (data == null) return;
		setShapeMin((List<Integer>) data.get(idNodeShapeMin));
		setShapeStep((List<Integer>) data.get(idNodeShapeStep));
	}

	@Override
	public DefaultInputNodeSpecification fromMap(Map data) {
		super.fromMap(data);
		return this;
	}

	@Override
	public void setShapeMin(List<Integer> shapeMin) {
		this.shapeMin = shapeMin;
	}

	@Override
	public void setShapeStep(List<Integer> shapeStep) {
		this.shapeStep = shapeStep;
	}

	@Override
	public List<Integer> getShapeMin() {
		return shapeMin;
	}

	@Override
	public List<Integer> getShapeStep() {
		return shapeStep;
	}
}
