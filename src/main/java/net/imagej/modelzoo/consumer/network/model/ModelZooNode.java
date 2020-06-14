package net.imagej.modelzoo.consumer.network.model;

public interface ModelZooNode<T> {
	void setName(String name);

	String getName();

	void setDataType(T type);

	Class getDataType();
}
