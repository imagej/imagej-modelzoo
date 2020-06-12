package net.imagej.modelzoo.specification;

import java.util.LinkedHashMap;
import java.util.Map;

public class TransformationSpecification {

	private final static String idSpec = "spec";
	private final static String idKwargs = "kwargs";

	private String specification;
	private Map<String, Object> kwargs;

	public void setSpec(String specification) {
		this.specification = specification;
	}

	public void setKwargs(Map<String, Object> kwargs) {
		this.kwargs = kwargs;
	}

	public Map<String, Object> asMap() {
		Map<String, Object> res = new LinkedHashMap<>();
		res.put(idSpec, specification);
		res.put(idKwargs, kwargs);
		return res;
	}

	public TransformationSpecification fromMap(Map<String, Object> data) {
		setSpec((String) data.get(idSpec));
		setKwargs((Map<String, Object>) data.get(idKwargs));
		return this;
	}

	public String getSpecification() {
		return specification;
	}

	public Map<String, Object> getKwargs() {
		return kwargs;
	}
}
