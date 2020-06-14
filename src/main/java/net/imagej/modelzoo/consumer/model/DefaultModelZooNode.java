package net.imagej.modelzoo.consumer.model;

public class DefaultModelZooNode<T> implements ModelZooNode<T> {

	private String name;

	@Override
	public void setName(final String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

}
