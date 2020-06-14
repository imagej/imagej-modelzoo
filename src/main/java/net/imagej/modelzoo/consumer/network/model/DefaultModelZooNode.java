package net.imagej.modelzoo.consumer.network.model;

import net.imglib2.img.Img;

public class DefaultModelZooNode<T> implements ModelZooNode<T> {

	private String name;
	private T dataType;

	@Override
	public void setName(final String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setDataType(T type) {
		this.dataType = type;
	}

	@Override
	public Class getDataType() {
		return Img.class;
	}
}
