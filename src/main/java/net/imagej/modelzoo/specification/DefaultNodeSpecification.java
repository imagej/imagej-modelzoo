package net.imagej.modelzoo.specification;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class DefaultNodeSpecification implements NodeSpecification {

	private final static String idNodeName = "name";
	private final static String idNodeAxes = "axes";
	private final static String idNodeDataType = "data_type";
	private final static String idNodeDataRange = "data_range";
	private final static String idNodeShape = "shape";
	private final static String idNodeHalo = "halo";

	private String name;
	private String axes;
	private String dataType;
	private List<?> dataRange;
	private List<Integer> halo;

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getAxes() {
		return axes;
	}

	@Override
	public String getDataType() {
		return dataType;
	}

	@Override
	public List<?> getDataRange() {
		return dataRange;
	}

	@Override
	public List<Integer> getHalo() {
		return halo;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void setAxes(String axes) {
		this.axes = axes;
	}

	@Override
	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

	@Override
	public void setDataRange(List<?> dataRange) {
		this.dataRange = dataRange;
	}

	@Override
	public void setHalo(List<Integer> halo) {
		this.halo = halo;
	}

	@Override
	public Map<String, Object> asMap() {
		Map<String, Object> res = new LinkedHashMap<>();
		res.put(idNodeName, name);
		if (axes != null) res.put(idNodeAxes, axes);
		if (dataType != null) res.put(idNodeDataType, dataType);
		if (dataRange != null) res.put(idNodeDataRange, dataRange);
		if (halo != null) res.put(idNodeHalo, halo);
		res.put(idNodeShape, getShape());
		return res;
	}

	@Override
	public DefaultNodeSpecification fromMap(Map data) {
		setName((String) data.get(idNodeName));
		setAxes((String) data.get(idNodeAxes));
		setDataType((String) data.get(idNodeDataType));
		setDataRange((List<?>) data.get(idNodeDataRange));
		setShape((Map<String, Object>) data.get(idNodeShape));
		setHalo((List<Integer>) data.get(idNodeHalo));
		return this;
	}

	protected abstract Map<String, Object> getShape();

	protected abstract void setShape(Map<String, Object> data);
}
