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
