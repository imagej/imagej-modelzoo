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
import java.util.Objects;

public class DefaultCitationSpecification implements CitationSpecification {

	private final static String idCiteText = "text";
	private final static String idCiteDoi = "doi";

	private String citationText;

	private String doiText;

	@Override
	public void setCitationText(String citationText) {
		this.citationText = citationText;
	}

	@Override
	public void setDOIText(String doiText) {
		this.doiText = doiText;
	}

	@Override
	public Map<String, Object> asMap() {
		Map<String, Object> res = new LinkedHashMap<>();
		res.put(idCiteText, citationText);
		res.put(idCiteDoi, doiText);
		return res;
	}

	@Override
	public String getCitationText() {
		return citationText;
	}

	@Override
	public String getDoiText() {
		return doiText;
	}

	@Override
	public DefaultCitationSpecification fromMap(Map data) {
		setCitationText((String) data.get(idCiteText));
		setDOIText((String) data.get(idCiteDoi));
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (DefaultCitationSpecification.class.isAssignableFrom(o.getClass())) {
			DefaultCitationSpecification other = (DefaultCitationSpecification) o;
			return Objects.equals(this.getCitationText(), other.getCitationText())
					&& Objects.equals(this.getDoiText(), other.getDoiText());
		}
		return super.equals(o);
	}
}
