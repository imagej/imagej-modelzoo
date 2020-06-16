package net.imagej.modelzoo.specification;

import java.util.Map;

public interface CitationSpecification {

	void setCitationText(String citationText);

	void setDOIText(String doiText);

	Map<String, Object> asMap();

	CitationSpecification fromMap(Map data);

	String getCitationText();

	String getDoiText();

}
