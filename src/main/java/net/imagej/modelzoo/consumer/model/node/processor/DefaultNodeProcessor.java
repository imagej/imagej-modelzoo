package net.imagej.modelzoo.consumer.model.node.processor;

import io.bioimage.specification.TransformationSpecification;

public abstract class DefaultNodeProcessor<T extends TransformationSpecification> implements NodeProcessor<T> {
	private String reference;

	@Override
	public void setReference(String reference) {
		this.reference = reference;
	}

	@Override
	public String getReference() {
		return reference;
	}
}
