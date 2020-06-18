package net.imagej.modelzoo.specification;

import java.util.Map;

public interface TransformationSpecification {

	void setSpec(String specification);

	void setKwargs(Map<String, Object> kwargs);

	Map<String, Object> asMap();

	TransformationSpecification fromMap(Map<String, Object> data);

	String getSpecification();

	Map<String, Object> getKwargs();
}
