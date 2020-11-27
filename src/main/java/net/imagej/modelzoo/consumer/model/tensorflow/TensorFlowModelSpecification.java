package net.imagej.modelzoo.consumer.model.tensorflow;

import io.bioimage.specification.DefaultModelSpecification;

public class TensorFlowModelSpecification extends DefaultModelSpecification {
	@Override
	public String getFramework() {
		return "tensorflow";
	}
}
