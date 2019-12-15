package net.imagej.modelzoo.consumer.network.model;

import net.imagej.axis.AxisType;

import java.util.Map;

public interface ModelZooAxis {

	AxisType getType();
	Map<String, Object> getAttributes();

}
