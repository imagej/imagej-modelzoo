package net.imagej.modelzoo.consumer.model.tensorflow;

import net.imagej.modelzoo.specification.DefaultWeightsSpecification;

public class TensorFlowSavedModelBundleSpecification extends DefaultWeightsSpecification {
	private String tag = "serve";

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}
}
