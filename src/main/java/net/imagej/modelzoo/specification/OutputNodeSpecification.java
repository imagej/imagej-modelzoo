package net.imagej.modelzoo.specification;

import java.util.List;
import java.util.Map;

public interface OutputNodeSpecification extends NodeSpecification {

	@Override
	OutputNodeSpecification fromMap(Map data);

	void setShapeReferenceInput(String referenceInputName);

	void setShapeScale(List<? extends Number> shapeScale);

	void setShapeOffset(List<Integer> shapeOffset);

	String getReferenceInputName();

	List<? extends Number> getShapeScale();

	List<Integer> getShapeOffset();
}
