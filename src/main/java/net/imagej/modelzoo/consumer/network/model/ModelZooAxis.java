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
import net.imagej.modelzoo.consumer.tiling.Tiling;

public class ModelZooAxis {

	private Integer min;
	private Integer halo;
	private Double scale;
	private Integer offset;
	private Tiling.TilingAction tiling;
	private Long actual;
	private AxisType type;
	private Integer step;

	public Integer getMin() {
		return min;
	}

	public Integer getHalo() {
		return halo;
	}

	public Double getScale() {
		return scale;
	}

	public Integer getOffset() {
		return offset;
	}

	public Tiling.TilingAction getTiling() {
		return tiling;
	}

	public Long getActual() {
		return actual;
	}

	public ModelZooAxis(AxisType axisType) {
		type = axisType;
	}

	public AxisType getType() {
		return type;
	}

	public void setMin(Integer min) {
		this.min = min;
	}

	public void setHalo(Integer halo) {
		this.halo = halo;
	}

	public void setScale(Double scale) {
		this.scale = scale;
	}

	public void setOffset(Integer offset) {
		this.offset = offset;
	}

	public void setTiling(Tiling.TilingAction tiling) {
		this.tiling = tiling;
	}

	public void setActual(Long actual) {
		this.actual = actual;
	}

	public Integer getStep() {
		return step;
	}

	public void setStep(Integer step) {
		this.step = step;
	}
}
