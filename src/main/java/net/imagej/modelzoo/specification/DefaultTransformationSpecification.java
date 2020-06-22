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
import java.util.Map;

public class DefaultTransformationSpecification implements TransformationSpecification {

	private final static String idSpec = "spec";
	private final static String idKwargs = "kwargs";

	private String specification;
	private Map<String, Object> kwargs;

	@Override
	public void setSpec(String specification) {
		this.specification = specification;
	}

	@Override
	public void setKwargs(Map<String, Object> kwargs) {
		this.kwargs = kwargs;
	}

	@Override
	public Map<String, Object> asMap() {
		Map<String, Object> res = new LinkedHashMap<>();
		res.put(idSpec, specification);
		res.put(idKwargs, kwargs);
		return res;
	}

	@Override
	public DefaultTransformationSpecification fromMap(Map<String, Object> data) {
		setSpec((String) data.get(idSpec));
		setKwargs((Map<String, Object>) data.get(idKwargs));
		return this;
	}

	@Override
	public String getSpecification() {
		return specification;
	}

	@Override
	public Map<String, Object> getKwargs() {
		return kwargs;
	}
}
