package net.imagej.modelzoo.specification;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class CitationSpecification {

	private final static String idCiteText = "text";
	private final static String idCiteDoi = "doi";

	private String citationText;

	private String doiText;

	public void setCitationText(String citationText) {
		this.citationText = citationText;
	}

	public void setDOIText(String doiText) {
		this.doiText = doiText;
	}

	public Map<String, Object> asMap() {
		Map<String, Object> res = new LinkedHashMap<>();
		res.put(idCiteText, citationText);
		res.put(idCiteDoi, doiText);
		return res;
	}

	public CitationSpecification fromMap(Map data) {
		setCitationText((String) data.get(idCiteText));
		setDOIText((String) data.get(idCiteDoi));
		return this;
	}

	public String getCitationText() {
		return citationText;
	}

	public String getDoiText() {
		return doiText;
	}

	@Override
	public boolean equals(Object o) {
		if (CitationSpecification.class.isAssignableFrom(o.getClass())) {
			CitationSpecification other = (CitationSpecification) o;
			return Objects.equals(this.getCitationText(), other.getCitationText())
					&& Objects.equals(this.getDoiText(), other.getDoiText());
		}
		return super.equals(o);
	}
}
