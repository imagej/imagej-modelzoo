package net.imagej.modelzoo.consumer.model;

import net.imagej.modelzoo.specification.TransformationSpecification;
import net.imglib2.RandomAccessibleInterval;

public abstract class DefaultImageNodeProcessor<T extends TransformationSpecification> extends DefaultNodeProcessor<T> implements ImageNodeProcessor<T> {

	private RandomAccessibleInterval input;
	private RandomAccessibleInterval output;
	private Object outputType;
	private InputImageNode<?> imageReference;

	@Override
	public void setInput(RandomAccessibleInterval input) {
		this.input = input;
	}

	@Override
	public RandomAccessibleInterval getOutput() {
		return output;
	}

	public RandomAccessibleInterval getInput() {
		return input;
	}

	public void setOutput(RandomAccessibleInterval output) {
		this.output = output;
	}

	@Override
	public void setOutputType(Object outputType) {
		this.outputType = outputType;
	}

	@Override
	public InputImageNode<?> getImageReference() {
		return imageReference;
	}

	@Override
	public void setImageReference(InputImageNode<?> reference) {
		this.imageReference = reference;
	}

	protected Object getOutputType() {
		if(getReference() != null) return getImageReference().getDataType();
		return outputType;
	}
}
