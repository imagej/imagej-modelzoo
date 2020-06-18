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
