package net.imagej.modelzoo.specification;

import java.util.List;
import java.util.Map;

public interface InputNodeSpecification extends NodeSpecification {

	@Override
	InputNodeSpecification fromMap(Map data);

	void setShapeMin(List<Integer> shapeMin);

	void setShapeStep(List<Integer> shapeStep);

	List<Integer> getShapeMin();

	List<Integer> getShapeStep();
}
