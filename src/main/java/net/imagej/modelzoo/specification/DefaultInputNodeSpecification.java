package net.imagej.modelzoo.specification;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultInputNodeSpecification extends DefaultNodeSpecification implements InputNodeSpecification {

	private final static String idNodeShapeMin = "min";
	private final static String idNodeShapeStep = "step";

	private List<Integer> shapeMin;
	private List<Integer> shapeStep;

	@Override
	public Map<String, Object> getShape() {
		Map<String, Object> shape = new LinkedHashMap<>();
		if (shapeMin != null) shape.put(idNodeShapeMin, shapeMin);
		if (shapeStep != null) shape.put(idNodeShapeStep, shapeStep);
		return shape;
	}

	@Override
	protected void setShape(Map<String, Object> data) {
		if (data == null) return;
		setShapeMin((List<Integer>) data.get(idNodeShapeMin));
		setShapeStep((List<Integer>) data.get(idNodeShapeStep));
	}

	@Override
	public DefaultInputNodeSpecification fromMap(Map data) {
		super.fromMap(data);
		return this;
	}

	@Override
	public void setShapeMin(List<Integer> shapeMin) {
		this.shapeMin = shapeMin;
	}

	@Override
	public void setShapeStep(List<Integer> shapeStep) {
		this.shapeStep = shapeStep;
	}

	@Override
	public List<Integer> getShapeMin() {
		return shapeMin;
	}

	@Override
	public List<Integer> getShapeStep() {
		return shapeStep;
	}
}
