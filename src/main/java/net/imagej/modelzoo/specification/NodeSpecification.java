package net.imagej.modelzoo.specification;

import java.util.List;
import java.util.Map;

public interface NodeSpecification {

	String getName();

	String getAxes();

	String getDataType();

	List<?> getDataRange();

	List<Integer> getHalo();

	void setName(String name);

	void setAxes(String axes);

	void setDataType(String dataType);

	void setDataRange(List<?> dataRange);

	void setHalo(List<Integer> halo);

	Map<String, Object> asMap();

	NodeSpecification fromMap(Map data);

}
